package com.arrl.radiocraft.client.model;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity;
import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireLook;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The wire's block model. Instead of a multipart blockstate (one condition set per block state, which cost gigabytes
 * once wires had tens of thousands of states), the look is worked out from the neighbours while the chunk is drawn
 * ({@link WireBlock#look}, {@link WireLook#parts}) and the quads are put together from the hand-made part models
 * ({@code <prefix>middle}, {@code <prefix>edge} and so on), each baked once per rotation.
 * <p>
 * Used from a block model JSON as {@code {"loader": "radiocraft:wire", "prefix": "radiocraft:block/wire_"}}. The
 * wire bundle's model is {@code {"loader": "radiocraft:wire", "bundle": true}}: it draws every strand of the bundle
 * with that strand's kind's parts, each moved sideways to its place in the bundle ({@link Strand#offset},
 * {@link WireLook#affine}).
 * <p>
 * A wire hanging from a ceiling ({@link WireBlock#CEILING}) draws the same parts upside down: each part is also baked
 * turned 180 degrees about X, which flips it top to bottom but also front to back, so a part meant for an edge is
 * turned about Y towards the opposite edge first.
 */
public final class WireModel {

	public static final ResourceLocation LOADER_ID = Radiocraft.id("wire");
	private static final ModelProperty<Key> PARTS = new ModelProperty<>();
	private static final ModelProperty<List<StrandDraw>> STRANDS = new ModelProperty<>();

	/**
	 * What a wire block draws: its parts, whether upside down, how far off the middle (a strand of a bundle) and where it
	 * meets the cable in each neighbouring block ({@link WireLook.Faces}).
	 */
	public record Key(List<WireLook.Placed> parts, boolean ceiling, double offset, WireLook.Faces faces) {}

	/** One strand of a bundle: the kind's model, what it draws, and the strand's state (for the quads' tint and shade). */
	private record StrandDraw(Baked model, Key key, BlockState state) {}

	private WireModel() {}

	/** Reads {@code prefix} from the model JSON, or {@code bundle}. */
	public enum Loader implements IGeometryLoader<Geometry> {
		INSTANCE;

		@Override
		public @NotNull Geometry read(JsonObject json, @NotNull JsonDeserializationContext context) {
			if(json.has("bundle") && json.get("bundle").getAsBoolean())
				return new Geometry(null);
			return new Geometry(json.get("prefix").getAsString());
		}
	}

	/** The unbaked model: loads the part models and bakes each one per rotation. A null prefix is the bundle's. */
	public record Geometry(@Nullable String prefix) implements IUnbakedGeometry<Geometry> {

		private ResourceLocation part(WireLook.Part part) {
			return ResourceLocation.parse(prefix + part.suffix);
		}

		@Override
		public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull IGeometryBakingContext context) {
			if(prefix == null)
				return;
			for(WireLook.Part part : WireLook.Part.values())
				modelGetter.apply(part(part)).resolveParents(modelGetter);
		}

		@Override
		public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, @NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
										@NotNull ModelState modelState, @NotNull ItemOverrides overrides) {
			if(prefix == null)
				return new BundleBaked();
			Map<WireLook.Part, Map<Integer, BakedModel>> baked = new EnumMap<>(WireLook.Part.class);
			Map<WireLook.Part, Map<Integer, BakedModel>> flipped = new EnumMap<>(WireLook.Part.class);
			for(WireLook.Part part : WireLook.Part.values()) {
				Map<Integer, BakedModel> byRotation = new HashMap<>();
				Map<Integer, BakedModel> flippedByRotation = new HashMap<>();
				for(int rotation : part.rotates ? new int[] { 0, 90, 180, 270 } : new int[] { 0 }) {
					byRotation.put(rotation, baker.bake(part(part), BlockModelRotation.by(0, rotation), spriteGetter));
					// X 180 also turns north to south, so aim at the opposite edge to land on this one.
					int towardsOpposite = part.rotates ? (rotation + 180) % 360 : 0;
					flippedByRotation.put(rotation, baker.bake(part(part), BlockModelRotation.by(180, towardsOpposite), spriteGetter));
				}
				baked.put(part, byRotation);
				flipped.put(part, flippedByRotation);
			}
			return new Baked(baked, flipped);
		}
	}

	/** The baked wire: picks its parts per block from the model data computed while the chunk is drawn. */
	public static final class Baked implements IDynamicBakedModel {

		private final Map<WireLook.Part, Map<Integer, BakedModel>> parts;
		private final Map<WireLook.Part, Map<Integer, BakedModel>> flipped;
		private final BakedModel middle;
		/** Quads per part list, built once: a world has few distinct wire looks. */
		private final Map<Key, List<BakedQuad>> quadCache = new ConcurrentHashMap<>();

		Baked(Map<WireLook.Part, Map<Integer, BakedModel>> parts, Map<WireLook.Part, Map<Integer, BakedModel>> flipped) {
			this.parts = parts;
			this.flipped = flipped;
			this.middle = parts.get(WireLook.Part.MIDDLE).get(0);
		}

		@Override
		public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
			if(!(state.getBlock() instanceof WireBlock wire))
				return modelData;
			return modelData.derive().with(PARTS, keyFor(level, pos, state, wire, 0.0D)).build();
		}

		/** What a wire of kind {@code wire} in {@code state} at {@code pos} draws, {@code offset} sixteenths off the middle. */
		static Key keyFor(BlockAndTintGetter level, BlockPos pos, BlockState state, WireBlock wire, double offset) {
			List<WireLook.Placed> placed = WireLook.parts(wire.look(level, pos, state), WireBlock.lookConnections(state));
			return new Key(placed, WireBlock.isCeiling(state), offset, wire.faces(level, pos, offset));
		}

		@Override
		public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
												 @NotNull ModelData data, @Nullable RenderType renderType) {
			if(side != null)
				return List.of(); // The part models have no cull faces: everything is in the unculled list.
			Key key = data.get(PARTS);
			if(key == null) { // No level to look at (breaking overlay, a state with no model data): connections only.
				boolean wire = state != null && state.getBlock() instanceof WireBlock;
				key = new Key(wire ? WireLook.parts(WireLook.Look.NONE, WireBlock.lookConnections(state)) : List.of(new WireLook.Placed(WireLook.Part.MIDDLE, null)),
						wire && WireBlock.isCeiling(state), 0.0D, WireLook.Faces.all(0.0D));
			}
			return quadsFor(key, state, rand, renderType);
		}

		/** The quads for {@code key}: the parts, turned to their edges, flipped for a ceiling and moved for a bundle. */
		List<BakedQuad> quadsFor(Key key, @Nullable BlockState state, RandomSource rand, @Nullable RenderType renderType) {
			return quadCache.computeIfAbsent(key, k -> {
				Map<WireLook.Part, Map<Integer, BakedModel>> set = k.ceiling() ? flipped : parts;
				boolean plain = k.offset() == 0 && k.faces().equals(WireLook.Faces.all(0.0D));
				List<BakedQuad> quads = new ArrayList<>();
				for(WireLook.Placed p : k.parts()) {
					BakedModel model = set.get(p.part()).get(p.part().rotates ? WireLook.rotation(p.dir()) : 0);
					if(model == null)
						continue;
					WireLook.Affine affine = plain ? WireLook.Affine.NONE : WireLook.affine(p, k.offset(), k.faces(), k.ceiling());
					for(BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, renderType))
						quads.add(affine.equals(WireLook.Affine.NONE) ? quad : moved(quad, affine));
				}
				return List.copyOf(quads);
			});
		}

		/** {@code quad} with {@code affine} applied to its corners (x and z, y untouched; a slant is slight enough that the faces keep their directions). */
		private static BakedQuad moved(BakedQuad quad, WireLook.Affine affine) {
			int[] vertices = quad.getVertices().clone();
			for(int i = 0; i < 4; i++) {
				int at = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
				float x = Float.intBitsToFloat(vertices[at]), y = Float.intBitsToFloat(vertices[at + 1]), z = Float.intBitsToFloat(vertices[at + 2]);
				vertices[at] = Float.floatToRawIntBits((float)affine.x(x, y, z));
				vertices[at + 2] = Float.floatToRawIntBits((float)affine.z(x, y, z));
			}
			return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
		}

		@Override
		public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
			return middle.getRenderTypes(state, rand, data);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public @NotNull TextureAtlasSprite getParticleIcon() {
			return middle.getParticleIcon();
		}

		@Override
		public @NotNull ItemOverrides getOverrides() {
			return ItemOverrides.EMPTY;
		}
	}

	/**
	 * The wire bundle's model: every strand drawn with its own kind's {@link Baked} (looked up through that kind's block
	 * model), side by side. The strands come from the block entity while the chunk is drawn.
	 */
	public static final class BundleBaked implements IDynamicBakedModel {

		@Nullable
		private static Baked modelOf(BlockState strand) {
			return Minecraft.getInstance().getBlockRenderer().getBlockModel(strand) instanceof Baked baked ? baked : null;
		}

		@Override
		public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
			if(!(level.getBlockEntity(pos) instanceof WireBundleBlockEntity bundle))
				return modelData;
			List<BlockState> strands = bundle.strands();
			double[] offsets = Strand.offsets(strands);
			List<StrandDraw> draws = new ArrayList<>(strands.size());
			for(int i = 0; i < strands.size(); i++) {
				BlockState strand = strands.get(i);
				Baked model = modelOf(strand);
				if(model != null)
					draws.add(new StrandDraw(model, Baked.keyFor(level, pos, strand, (WireBlock)strand.getBlock(), offsets[i]), strand));
			}
			return modelData.derive().with(STRANDS, List.copyOf(draws)).build();
		}

		@Override
		public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
												 @NotNull ModelData data, @Nullable RenderType renderType) {
			List<StrandDraw> draws = data.get(STRANDS);
			if(side != null || draws == null)
				return List.of();
			List<BakedQuad> quads = new ArrayList<>();
			for(StrandDraw draw : draws)
				quads.addAll(draw.model().quadsFor(draw.key(), draw.state(), rand, renderType));
			return quads;
		}

		@Override
		public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
			List<StrandDraw> draws = data.get(STRANDS);
			if(draws == null || draws.isEmpty()) {
				Baked coax = modelOf(RadiocraftBlocks.COAX_WIRE.get().defaultBlockState());
				return coax == null ? ChunkRenderTypeSet.of(RenderType.cutout()) : coax.getRenderTypes(state, rand, ModelData.EMPTY);
			}
			List<ChunkRenderTypeSet> sets = new ArrayList<>(draws.size());
			for(StrandDraw draw : draws)
				sets.add(draw.model().getRenderTypes(draw.state(), rand, ModelData.EMPTY));
			return ChunkRenderTypeSet.union(sets);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public @NotNull TextureAtlasSprite getParticleIcon() {
			Baked coax = modelOf(RadiocraftBlocks.COAX_WIRE.get().defaultBlockState());
			return coax == null ? Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon() : coax.getParticleIcon();
		}

		@Override
		public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
			List<StrandDraw> draws = data.get(STRANDS);
			return draws == null || draws.isEmpty() ? getParticleIcon() : draws.getFirst().model().getParticleIcon();
		}

		@Override
		public @NotNull ItemOverrides getOverrides() {
			return ItemOverrides.EMPTY;
		}
	}
}

package com.arrl.radiocraft.client.model;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireLook;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
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
 * Used from a block model JSON as {@code {"loader": "radiocraft:wire", "prefix": "radiocraft:block/wire_"}}.
 * <p>
 * A wire hanging from a ceiling ({@link WireBlock#CEILING}) draws the same parts upside down: each part is also baked
 * turned 180 degrees about X, which flips it top to bottom but also front to back, so a part meant for an edge is
 * turned about Y towards the opposite edge first.
 */
public final class WireModel {

	public static final ResourceLocation LOADER_ID = Radiocraft.id("wire");
	private static final ModelProperty<Key> PARTS = new ModelProperty<>();

	/** What a wire block draws: its parts, and whether upside down. */
	private record Key(List<WireLook.Placed> parts, boolean ceiling) {}

	private WireModel() {}

	/** Reads {@code prefix} from the model JSON. */
	public enum Loader implements IGeometryLoader<Geometry> {
		INSTANCE;

		@Override
		public @NotNull Geometry read(JsonObject json, @NotNull JsonDeserializationContext context) {
			return new Geometry(json.get("prefix").getAsString());
		}
	}

	/** The unbaked model: loads the part models and bakes each one per rotation. */
	public record Geometry(String prefix) implements IUnbakedGeometry<Geometry> {

		private ResourceLocation part(WireLook.Part part) {
			return ResourceLocation.parse(prefix + part.suffix);
		}

		@Override
		public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull IGeometryBakingContext context) {
			for(WireLook.Part part : WireLook.Part.values())
				modelGetter.apply(part(part)).resolveParents(modelGetter);
		}

		@Override
		public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, @NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
										@NotNull ModelState modelState, @NotNull ItemOverrides overrides) {
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
			List<WireLook.Placed> placed = WireLook.parts(wire.look(level, pos, state), WireBlock.lookConnections(state));
			return modelData.derive().with(PARTS, new Key(placed, WireBlock.isCeiling(state))).build();
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
						wire && WireBlock.isCeiling(state));
			}
			return quadCache.computeIfAbsent(key, k -> {
				Map<WireLook.Part, Map<Integer, BakedModel>> set = k.ceiling() ? flipped : parts;
				List<BakedQuad> quads = new ArrayList<>();
				for(WireLook.Placed p : k.parts()) {
					BakedModel model = set.get(p.part()).get(p.part().rotates ? WireLook.rotation(p.dir()) : 0);
					if(model != null)
						quads.addAll(model.getQuads(state, null, rand, ModelData.EMPTY, renderType));
				}
				return List.copyOf(quads);
			});
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
}

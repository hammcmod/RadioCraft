package com.arrl.radiocraft.client.model;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.TowerBeamBlock;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A tower beam's block model: the core plus an arm toward each neighbouring beam, picked while the chunk is drawn
 * ({@link TowerBeamBlock#arms}: touching beams, and blocks it bears on), from the part models {@code <prefix>core} and {@code <prefix>arm_<name>}
 * that {@code tools/tower_beam_models.py} draws. A beam with no beam beside it is drawn as a straight post.
 * <p>
 * Used from a block model JSON as {@code {"loader": "radiocraft:tower_beam", "prefix": "radiocraft:block/tower_beam_"}}.
 */
public final class TowerBeamModel {

	public static final ResourceLocation LOADER_ID = Radiocraft.id("tower_beam");
	private static final ModelProperty<Integer> MASK = new ModelProperty<>();

	private TowerBeamModel() {}

	public enum Loader implements IGeometryLoader<Geometry> {
		INSTANCE;

		@Override
		public @NotNull Geometry read(JsonObject json, @NotNull JsonDeserializationContext context) {
			return new Geometry(json.get("prefix").getAsString());
		}
	}

	public record Geometry(String prefix) implements IUnbakedGeometry<Geometry> {

		private ResourceLocation core() {
			return ResourceLocation.parse(prefix + "core");
		}

		private ResourceLocation arm(TowerBeamBlock.Offset offset) {
			return ResourceLocation.parse(prefix + "arm_" + offset.name());
		}

		@Override
		public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull IGeometryBakingContext context) {
			modelGetter.apply(core()).resolveParents(modelGetter);
			for(TowerBeamBlock.Offset offset : TowerBeamBlock.OFFSETS)
				modelGetter.apply(arm(offset)).resolveParents(modelGetter);
		}

		@Override
		public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, @NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
										@NotNull ModelState modelState, @NotNull ItemOverrides overrides) {
			BakedModel core = baker.bake(core(), BlockModelRotation.X0_Y0, spriteGetter);
			BakedModel[] arms = new BakedModel[TowerBeamBlock.OFFSETS.size()];
			for(int i = 0; i < arms.length; i++)
				arms[i] = baker.bake(arm(TowerBeamBlock.OFFSETS.get(i)), BlockModelRotation.X0_Y0, spriteGetter);
			return new Baked(core, arms);
		}
	}

	public static final class Baked implements IDynamicBakedModel {

		private final BakedModel core;
		private final BakedModel[] arms;
		/** Quads per neighbour mask: a tower has only a handful of different joints. */
		private final Map<Integer, List<BakedQuad>> quadCache = new ConcurrentHashMap<>();

		Baked(BakedModel core, BakedModel[] arms) {
			this.core = core;
			this.arms = arms;
		}

		@Override
		public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
			return modelData.derive().with(MASK, TowerBeamBlock.arms(level, pos)).build();
		}

		@Override
		public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
												 @NotNull ModelData data, @Nullable RenderType renderType) {
			if(side != null)
				return List.of(); // Everything sits in the unculled list, as the parts overlap at the block edges.
			Integer found = data.get(MASK);
			int mask = found == null || found == 0 ? TowerBeamBlock.POST : found;
			return quadCache.computeIfAbsent(mask, m -> {
				List<BakedQuad> quads = new ArrayList<>();
				addAll(quads, core, state, rand, renderType);
				for(int i = 0; i < arms.length; i++)
					if((m & (1 << i)) != 0 && arms[i] != null)
						addAll(quads, arms[i], state, rand, renderType);
				return List.copyOf(quads);
			});
		}

		private static void addAll(List<BakedQuad> out, BakedModel model, @Nullable BlockState state, RandomSource rand, @Nullable RenderType renderType) {
			out.addAll(model.getQuads(state, null, rand, ModelData.EMPTY, renderType));
			for(Direction direction : Direction.values())
				out.addAll(model.getQuads(state, direction, rand, ModelData.EMPTY, renderType));
		}

		@Override
		public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
			return core.getRenderTypes(state, rand, data);
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
		@Deprecated // BakedModel still requires it; the ModelData version below is the one used.
		public @NotNull TextureAtlasSprite getParticleIcon() {
			return core.getParticleIcon(ModelData.EMPTY);
		}

		@Override
		public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
			return core.getParticleIcon(ModelData.EMPTY);
		}

		@Override
		public @NotNull ItemOverrides getOverrides() {
			return ItemOverrides.EMPTY;
		}
	}
}

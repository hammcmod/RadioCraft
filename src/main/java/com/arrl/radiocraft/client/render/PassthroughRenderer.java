package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.PassthroughBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the block a passthrough replaced, in its place, the way chunk rendering would draw it there: each face lit
 * by the block in front of it, with smooth lighting and ambient occlusion, and faces hidden behind neighbours culled.
 * (An earlier version lit every face with the brightest neighbour, so a wall between a lit and a dark side glowed on
 * the dark side.)
 */
public class PassthroughRenderer implements BlockEntityRenderer<PassthroughBlockEntity> {

	public PassthroughRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(PassthroughBlockEntity be, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
		BlockState mimic = be.getMimic();
		Level level = be.getLevel();
		if(mimic == null || level == null)
			return;
		BlockPos pos = be.getBlockPos();
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		BakedModel model = dispatcher.getBlockModel(mimic);
		RandomSource random = RandomSource.create();
		long seed = mimic.getSeed(pos);
		random.setSeed(seed);
		for(RenderType renderType : model.getRenderTypes(mimic, random, ModelData.EMPTY)) {
			dispatcher.getModelRenderer().tesselateBlock(level, model, mimic, pos, poseStack, buffer.getBuffer(renderType),
					true, random, seed, packedOverlay, ModelData.EMPTY, renderType);
		}
	}
}

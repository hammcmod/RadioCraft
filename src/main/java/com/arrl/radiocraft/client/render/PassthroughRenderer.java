package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.common.blockentities.PassthroughBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the block a passthrough replaced, in its place. The passthrough itself is opaque (so the wall's neighbours
 * cull their faces against it, like any wall block), which also means there is no light inside it; the mimic is lit
 * with the brightest of the six neighbouring blocks instead.
 */
public class PassthroughRenderer implements BlockEntityRenderer<PassthroughBlockEntity> {

	public PassthroughRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(PassthroughBlockEntity be, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
		BlockState mimic = be.getMimic();
		Level level = be.getLevel();
		if(mimic == null || level == null)
			return;
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(mimic, poseStack, buffer, brightestNeighbour(level, be.getBlockPos()), packedOverlay);
	}

	private static int brightestNeighbour(Level level, BlockPos pos) {
		int block = 0, sky = 0;
		for(Direction dir : Direction.values()) {
			int light = LevelRenderer.getLightColor(level, pos.relative(dir));
			block = Math.max(block, LightTexture.block(light));
			sky = Math.max(sky, LightTexture.sky(light));
		}
		return LightTexture.pack(block, sky);
	}
}

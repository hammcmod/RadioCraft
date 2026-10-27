package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TapePlayerBlockEntity;
import com.arrl.radiocraft.common.blocks.TapePlayerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The tape player's two reels, turning smoothly while it plays or records (see
 * {@link TapePlayerBlockEntity#clientTick}). One reel model, centred on the front, drawn at each hub.
 */
public class TapePlayerRenderer implements BlockEntityRenderer<TapePlayerBlockEntity> {

	public static final ModelResourceLocation REEL = ModelResourceLocation.standalone(Radiocraft.id("block/tape_player_reel"));
	/** The hubs on the front, in pixels (tools/station_models.py REELS). */
	private static final double[][] HUBS = { { 4.75D, 9.25D }, { 11.25D, 9.25D } };

	public TapePlayerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(TapePlayerBlockEntity player, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		BakedModel reel = Minecraft.getInstance().getModelManager().getModel(REEL);
		Direction facing = player.getBlockState().getValue(TapePlayerBlock.FACING);
		float angle = player.reelAngle(partialTick);
		poseStack.pushPose();
		// The model faces north; turn it to face the way the block does, about the block's middle.
		poseStack.translate(0.5D, 0.0D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
		poseStack.translate(-0.5D, 0.0D, -0.5D);
		for(int i = 0; i < HUBS.length; i++) {
			double x = HUBS[i][0] / 16.0D, y = HUBS[i][1] / 16.0D;
			poseStack.pushPose();
			poseStack.translate(x, y, 0.0D);
			poseStack.mulPose(Axis.ZP.rotationDegrees(i == 0 ? angle : angle * 0.8F)); // The take-up reel runs a little slower.
			poseStack.translate(-0.5D, -0.5D, 0.0D);
			Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
					null, reel, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.cutoutBlockSheet());
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}

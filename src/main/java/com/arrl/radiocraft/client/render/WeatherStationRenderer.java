package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.WeatherStationBlockEntity;
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
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The weather station's moving parts, turned smoothly every frame: the vane swings round to point into the wind and
 * the anemometer cups spin with its speed (see {@link WeatherStationBlockEntity#clientTick}). The rest of the station is
 * its block model.
 */
public class WeatherStationRenderer implements BlockEntityRenderer<WeatherStationBlockEntity> {

	public static final ModelResourceLocation VANE = ModelResourceLocation.standalone(Radiocraft.id("block/weather_station_vane"));
	public static final ModelResourceLocation CUPS = ModelResourceLocation.standalone(Radiocraft.id("block/weather_station_cups"));

	public WeatherStationRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(WeatherStationBlockEntity station, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		// The vane's arrow points north in its model; turning clockwise by the bearing the wind comes from points it into the wind.
		part(VANE, 13, 8, -station.vaneAngle(partialTick), poseStack, buffers, light);
		part(CUPS, 8, 8, station.cupAngle(partialTick), poseStack, buffers, light);
	}

	/** Draws {@code model} turned {@code degrees} about the vertical line through pixel ({@code x}, {@code z}). */
	private static void part(ModelResourceLocation model, double x, double z, float degrees, PoseStack poseStack, MultiBufferSource buffers, int light) {
		BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
		poseStack.pushPose();
		poseStack.translate(x / 16, 0, z / 16);
		poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
		poseStack.translate(-x / 16, 0, -z / 16);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
				null, baked, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.cutoutBlockSheet());
		poseStack.popPose();
	}
}

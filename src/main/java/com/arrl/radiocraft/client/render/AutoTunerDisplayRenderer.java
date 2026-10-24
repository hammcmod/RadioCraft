package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.RadiocraftClientConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.AutoTunerNetworkObject;
import com.arrl.radiocraft.common.blockentities.AutoTunerBlockEntity;
import com.arrl.radiocraft.common.blocks.antennas.AutoTunerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Writes what the automatic tuner is doing on its model's little display (the {@code DISPLAY} element, see
 * {@link RadioDisplays}): TUNING while it searches, TUNED, or FAIL, in the radio screens' amber. Dark without power or
 * before it has had a radio to tune for. Same distance and client setting as the radio screens.
 */
public class AutoTunerDisplayRenderer implements BlockEntityRenderer<AutoTunerBlockEntity> {

	private static final int COLOUR = 0xFFFFB347;
	private static final int FAIL_COLOUR = 0xFFFF5533;

	private final Font font;

	public AutoTunerDisplayRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.getFont();
	}

	@Override
	public void render(@NotNull AutoTunerBlockEntity tuner, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if(!RadiocraftClientConfig.SHOW_RADIO_SCREENS.get() || !tuner.isShownPowered() || tuner.getLevel() == null)
			return;
		String text = switch(tuner.getShownState()) {
			case TUNING, SEARCHING -> "TUNING";
			case TUNED -> "TUNED";
			case FAILED -> "FAIL";
			case IDLE -> null;
		};
		if(text == null)
			return;
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		if(camera.distanceToSqr(tuner.getBlockPos().getCenter()) > RadioDisplayRenderer.VIEW_DISTANCE * RadioDisplayRenderer.VIEW_DISTANCE)
			return;
		BlockState state = tuner.getBlockState();
		RadioDisplays.Display display = RadioDisplays.get("auto_tuner");
		if(display == null || !state.hasProperty(AutoTunerBlock.FACING))
			return;

		float width = (display.x2() - display.x1()) / 16.0F, height = (display.y2() - display.y1()) / 16.0F;
		float scale = Math.min(width * 0.9F / font.width(text), height * 0.7F / font.lineHeight);
		boolean failed = tuner.getShownState() == AutoTunerNetworkObject.State.FAILED;

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D); // Turn as the blockstate turns the model (it faces north as drawn).
		poseStack.mulPose(Axis.YP.rotationDegrees(-((state.getValue(AutoTunerBlock.FACING).toYRot() + 180.0F) % 360.0F)));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
		// The display's top left as seen from the front: its east edge (the viewer faces south), a hair in front of it.
		poseStack.translate(display.x2() / 16.0D, display.y2() / 16.0D, display.front() / 16.0D - 0.002D);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F)); // Text reads along -x and faces north, out of the display.
		poseStack.translate((width - font.width(text) * scale) / 2.0F, -(height - font.lineHeight * scale) / 2.0F, 0.0F);
		poseStack.scale(scale, -scale, scale);
		font.drawInBatch(text, 0.0F, 0.0F, failed ? FAIL_COLOUR : COLOUR, false, poseStack.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
		poseStack.popPose();
	}

	@Override
	public int getViewDistance() {
		return (int)Math.ceil(RadioDisplayRenderer.VIEW_DISTANCE) + 1;
	}
}

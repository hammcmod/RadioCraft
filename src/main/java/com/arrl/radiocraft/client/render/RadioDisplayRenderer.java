package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.RadiocraftClientConfig;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Draws a radio's readout on its model's screen (the {@code DISPLAY} element, see {@link RadioDisplays}) when a player
 * is close: the frequency, and under it smaller, the mode and an S-meter. Amber and full bright, like a lit LCD; blank
 * while the radio is off. Radios whose model has no display draw nothing. A player can turn it off in their client
 * config ({@link RadiocraftClientConfig#SHOW_RADIO_SCREENS}).
 * <p>
 * The S-meter reads what the receiver hears, signal or noise, whichever is stronger, as a real one does: a quiet band
 * sits low, a station lifts it, and on HF a blackout or an aurora overhead pins it near the top.
 */
public class RadioDisplayRenderer<T extends RadioBlockEntity> implements BlockEntityRenderer<T> {

	/** Furthest a player sees the readout from, in blocks. */
	public static final double VIEW_DISTANCE = 8.0D;
	private static final int COLOUR = 0xFFFFB347;
	private static final int DIM = 0xFF8A5E22;

	private final Font font;

	public RadioDisplayRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.getFont();
	}

	@Override
	public void render(@NotNull T radio, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if(!RadiocraftClientConfig.SHOW_RADIO_SCREENS.get() || !radio.wasPowered() || radio.getLevel() == null)
			return;
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		if(camera.distanceToSqr(radio.getBlockPos().getCenter()) > VIEW_DISTANCE * VIEW_DISTANCE)
			return;
		BlockState state = radio.getBlockState();
		RadioDisplays.Display display = RadioDisplays.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
		if(display == null || !state.hasProperty(RadioBlock.HORIZONTAL_FACING))
			return;

		String frequency = String.format(Locale.ROOT, "%.3f", radio.getFrequency() / 1_000_000.0D);
		String status = mode(radio) + " " + meter(radio);
		float width = (display.x2() - display.x1()) / 16.0F, height = (display.y2() - display.y1()) / 16.0F;
		float big = Math.min(width * 0.9F / font.width(frequency), height * 0.55F / font.lineHeight);
		float small = Math.min(big * 0.6F, width * 0.9F / font.width(status));

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D); // Turn as the blockstate turns the model (it faces north as drawn).
		poseStack.mulPose(Axis.YP.rotationDegrees(-((state.getValue(RadioBlock.HORIZONTAL_FACING).toYRot() + 180.0F) % 360.0F)));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
		if(state.hasProperty(RadioBlock.CEILING) && state.getValue(RadioBlock.CEILING))
			poseStack.translate(0.0D, display.rise() / 16.0D, 0.0D);
		// The screen's top left as seen from the front: its east edge (the viewer faces south), a hair in front of it.
		poseStack.translate(display.x2() / 16.0D, display.y2() / 16.0D, display.front() / 16.0D - 0.002D);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F)); // Text reads along -x and faces north, out of the screen.
		float top = (height - big * font.lineHeight - small * font.lineHeight) / 3.0F;
		draw(poseStack, buffer, frequency, (width - font.width(frequency) * big) / 2.0F, top, big, COLOUR);
		draw(poseStack, buffer, status, (width - font.width(status) * small) / 2.0F, top * 2.0F + big * font.lineHeight, small, DIM);
		poseStack.popPose();
	}

	private void draw(PoseStack poseStack, MultiBufferSource buffer, String text, float x, float y, float scale, int colour) {
		poseStack.pushPose();
		poseStack.translate(x, -y, 0.0F);
		poseStack.scale(scale, -scale, scale);
		font.drawInBatch(text, 0.0F, 0.0F, colour, false, poseStack.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
		poseStack.popPose();
	}

	/** USB or LSB on SSB, CW, or FM (VHF radios and the all-band radio's FM). */
	private static String mode(RadioBlockEntity radio) {
		if(radio instanceof HFRadioAllBandBlockEntity allBand && allBand.getFMEnabled())
			return "FM";
		if(radio.isFM())
			return "FM";
		if(radio instanceof HFRadioBlockEntity hf && hf.getCWEnabled())
			return "CW";
		return radio.isLowerSideband() ? "LSB" : "USB";
	}

	/** S0 to S9, or S9+ at the top: the signal heard or the noise floor, whichever is stronger. */
	private static String meter(RadioBlockEntity radio) {
		double reading = Math.max(radio.getSyncedSignal(), radio.getNoiseFloor());
		if(reading >= 0.95D)
			return "S9+";
		return "S" + Math.min(9, (int)Math.round(reading * 9.0D));
	}

	@Override
	public int getViewDistance() {
		return (int)Math.ceil(VIEW_DISTANCE) + 1;
	}
}

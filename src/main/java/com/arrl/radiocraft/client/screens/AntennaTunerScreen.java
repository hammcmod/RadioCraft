package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity;
import com.arrl.radiocraft.common.menus.AntennaTunerMenu;
import com.arrl.radiocraft.common.network.serverbound.SAntennaTunerDialPacket;
import com.arrl.radiocraft.common.radio.TunerMath;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AntennaTunerScreen extends AbstractContainerScreen<AntennaTunerMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/antenna_tuner.png");

	/** The readout's two lines sit above the panel, over the world, so they are drawn light with a shadow. */
	private static final int ABOVE_Y = -24;
	private static final int READOUT_LINE = 11;
	private static final int ABOVE_COLOR = 0xFFFFFFFF;
	private static final int ABOVE_WARN_COLOR = 0xFFFFB050;
	/** The dials' readings, on the panel. */
	private static final int READOUT_COLOR = 0xFFD8E0D8;

	/** The three dials' left edges, their top and width; each dial's reading is written above it. */
	private static final int[] DIAL_X = {23, 106, 188};
	private static final int DIAL_Y = 76;
	private static final int DIAL_WIDTH = 42;
	private static final int DIAL_LABEL_Y = 66;

	/** The lamp in the middle panel's window, and its sprites: unlit (also tinted for the greens) and red. */
	private static final int LAMP_X = 121, LAMP_Y = 42, LAMP_W = 12, LAMP_H = 19;
	private static final int LAMP_OFF_U = 151, LAMP_RED_U = 167, LAMP_V = 142;

	/** A dial turned here and not yet confirmed by the server, -1 for none: shown instead of the synced reading. */
	private final int[] pending = {-1, -1, -1};

	public AntennaTunerScreen(AntennaTunerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 256;
		this.imageHeight = 128;
	}

	@Override
	protected void init() {
		super.init();
		for(int i = 0; i < 3; i++) {
			int dial = i;
			addRenderableWidget(new Dial(leftPos + DIAL_X[i], topPos + DIAL_Y, DIAL_WIDTH, 45, 48, 137, TEXTURE, 256, 256,
					d -> turn(dial, 1), d -> turn(dial, -1)));
		}
	}

	/** The reading a dial shows: what the player just turned it to, or the server's. */
	private int dial(int index) {
		if(pending[index] >= 0 && pending[index] == menu.getDial(index))
			pending[index] = -1;
		return pending[index] >= 0 ? pending[index] : menu.getDial(index);
	}

	private void turn(int index, int steps) {
		int value = Math.max(0, Math.min(TunerMath.DIAL_MAX, dial(index) + steps));
		pending[index] = value;
		SAntennaTunerDialPacket.send(menu.blockEntity.getBlockPos(), index, value);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);

		int edgeSpacingX = (this.width - this.imageWidth) / 2;
		int edgeSpacingY = (this.height - this.imageHeight) / 2;
		guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
		renderLamp(guiGraphics, edgeSpacingX + LAMP_X, edgeSpacingY + LAMP_Y);
	}

	/**
	 * The lamp that guides the player to a match: red blinking faster as the SWR the radio sees comes down towards 3,
	 * yellow-green below 3 and faster again, solid green at a perfect tune (see {@link TunerMath#lamp}). Dark without
	 * an antenna and a radio to tune for. The greens are the unlit sprite tinted until it has sprites of its own.
	 */
	private void renderLamp(GuiGraphics guiGraphics, int x, int y) {
		int status = menu.getStatus();
		boolean tuning = status == AntennaTunerBlockEntity.STATUS_MATCHED || status == AntennaTunerBlockEntity.STATUS_TUNING
				|| status == AntennaTunerBlockEntity.STATUS_OUT_OF_RANGE;
		double ticks = System.currentTimeMillis() / 50.0D;
		TunerMath.Lamp lamp = tuning ? TunerMath.lamp(menu.getRawSwr(), menu.getTunedSwr(), ticks) : TunerMath.Lamp.OFF;
		switch(lamp) {
			case RED -> guiGraphics.blit(TEXTURE, x, y, LAMP_RED_U, LAMP_V, LAMP_W, LAMP_H, 256, 256);
			case YELLOW_GREEN -> tinted(guiGraphics, x, y, 0.75F, 1.0F, 0.25F);
			case GREEN -> tinted(guiGraphics, x, y, 0.3F, 1.0F, 0.3F);
			case OFF -> guiGraphics.blit(TEXTURE, x, y, LAMP_OFF_U, LAMP_V, LAMP_W, LAMP_H, 256, 256);
		}
	}

	private void tinted(GuiGraphics guiGraphics, int x, int y, float r, float g, float b) {
		guiGraphics.setColor(r, g, b, 1.0F);
		guiGraphics.blit(TEXTURE, x, y, LAMP_OFF_U, LAMP_V, LAMP_W, LAMP_H, 256, 256);
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * The tuner's readout instead of the default title and inventory labels: a line for the state (tuned, keep
	 * turning, out of range, no radio...) and what the antenna presents, centred above the panel where there is room
	 * for it, and each dial's reading above the dial. The lamp, not a number, tells the player how close the dials are.
	 */
	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int status = menu.getStatus();
		Component state = Component.translatable(Radiocraft.translationKey("screen", "antenna_tuner.status." + status));
		boolean warn = status != AntennaTunerBlockEntity.STATUS_MATCHED && status != AntennaTunerBlockEntity.STATUS_TUNING;
		guiGraphics.drawString(font, state, (imageWidth - font.width(state)) / 2, ABOVE_Y, warn ? ABOVE_WARN_COLOR : ABOVE_COLOR, true);

		if(status == AntennaTunerBlockEntity.STATUS_MATCHED || status == AntennaTunerBlockEntity.STATUS_TUNING
				|| status == AntennaTunerBlockEntity.STATUS_OUT_OF_RANGE || status == AntennaTunerBlockEntity.STATUS_DISABLED) {
			Component antenna = Component.translatable(Radiocraft.translationKey("screen", "antenna_tuner.antenna_swr"), String.format(Locale.ROOT, "%.1f", menu.getRawSwr()));
			guiGraphics.drawString(font, antenna, (imageWidth - font.width(antenna)) / 2, ABOVE_Y + READOUT_LINE, ABOVE_COLOR, true);
		}

		for(int i = 0; i < 3; i++) {
			String reading = Integer.toString(dial(i));
			guiGraphics.drawString(font, reading, DIAL_X[i] + (DIAL_WIDTH - font.width(reading)) / 2, DIAL_LABEL_Y, READOUT_COLOR, true);
		}
	}
}

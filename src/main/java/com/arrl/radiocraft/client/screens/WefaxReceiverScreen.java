package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.WefaxReceiverBlockEntity;
import com.arrl.radiocraft.common.menus.WefaxReceiverMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.wefax.WefaxChart;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The WEFAX receiver's screen (art from tools/wefax_gui.py, whose boxes are copied here): the machine, with the sheet
 * being printed rising out of the slot behind the drum as it prints, top first, the chart appearing on it line by
 * line. WX REPT, SOLR WX and AUTO along the base (AUTO stays down while it is on), the paper supply window and the
 * power lamp, lit while a radio is beside it.
 */
public class WefaxReceiverScreen extends StationPanelScreen<WefaxReceiverMenu> {

	private static final ResourceLocation MACHINE = Radiocraft.id("textures/gui/wefax_receiver.png");
	private static final ResourceLocation SPRITES = Radiocraft.id("textures/gui/wefax_receiver_paper.png");
	private static final ResourceLocation CHART = Radiocraft.id("wefax_receiver_chart");
	private static final int SIZE = 256;
	// In wefax_receiver.png, {x, y, width, height}. The screen is the whole texture: the machine is drawn from 84 down,
	// and the paper rises into the clear space above it, which counts when the screen is fitted to the window.
	private static final int[] SLOT = { 60, 140, 136, 7 };
	private static final int[][] BUTTONS = { { 28, 220, 44, 18 }, { 80, 220, 44, 18 }, { 132, 220, 44, 18 } };
	private static final int[] LEVEL = { 196, 216, 20, 28 };
	private static final int[] LAMP = { 226, 222, 8, 8 };
	// In wefax_receiver_paper.png.
	private static final int[] PAPER = { 0, 0, 136, 136 };
	private static final int[][] BUTTONS_DOWN = { { 0, 144, 44, 18 }, { 48, 144, 44, 18 }, { 96, 144, 44, 18 } };
	private static final int[] LAMP_ON = { 144, 144, 8, 8 };
	private static final String[] ACTIONS = { "weather", "solar", "auto" };
	private static final String[] NAMES = { "screen.radiocraft.wefax.weather", "screen.radiocraft.wefax.solar", "screen.radiocraft.wefax.auto" };

	private DynamicTexture chart;
	private byte[] shown = new byte[0];
	private int pressed = -1;
	private long pressedUntil;

	public WefaxReceiverScreen(WefaxReceiverMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = SIZE;
		imageHeight = SIZE;
	}

	/** A box from the machine texture, as {x, y, w, h} on the screen (the same: the screen is the texture). */
	private static int[] screen(int[] r) {
		return r;
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		WefaxReceiverBlockEntity receiver = menu.blockEntity;
		if(receiver.hasReadySheet() && !receiver.isPrinting() && inside(screen(sheetBox()), mouseX, mouseY)) { // The printed sheet: take it.
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 0.6F, 1.1F));
			SStationPacket.send(menu.getBlockPos(), "take");
			return true;
		}
		for(int i = 0; i < BUTTONS.length; i++) {
			if(inside(screen(BUTTONS[i]), mouseX, mouseY)) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 0.9F, 0.4F));
				pressed = i;
				pressedUntil = System.currentTimeMillis() + 180L;
				SStationPacket.send(menu.getBlockPos(), ACTIONS[i]);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		WefaxReceiverBlockEntity receiver = menu.blockEntity;
		renderSheet(graphics, receiver, partialTick); // Behind the machine: it comes up out of the slot.
		graphics.blit(MACHINE, leftPos, topPos, 0, 0, imageWidth, imageHeight, SIZE, SIZE);
		for(int i = 0; i < BUTTONS.length; i++) {
			boolean down = (i == pressed && System.currentTimeMillis() < pressedUntil) || (i == 2 && receiver.isAuto());
			if(down)
				sprite(graphics, BUTTONS_DOWN[i], BUTTONS[i]);
		}
		int paper = receiver.getPaper();
		int level = paper <= 0 ? 0 : 1 + Math.min(3, (paper - 1) * 4 / WefaxReceiverBlockEntity.PAPER_MAX);
		sprite(graphics, new int[] { 24 * level, 168, 20, 28 }, LEVEL);
		if(receiver.clientHasRadio())
			sprite(graphics, LAMP_ON, LAMP);
	}

	private void sprite(GuiGraphics graphics, int[] from, int[] at) {
		graphics.blit(SPRITES, leftPos + at[0], topPos + at[1], from[0], from[1], from[2], from[3], SIZE, SIZE);
	}

	/** Where the whole sheet stands once printed: click it to take it. */
	private int[] sheetBox() {
		return new int[] { SLOT[0], SLOT[1] - PAPER[3], PAPER[2], PAPER[3] };
	}

	/**
	 * The sheet: as much of it as has printed stands up out of the slot, the chart drawn on it down to the print head;
	 * printed, it stays up until it is clicked and taken (maintainer, 2026-10-27).
	 */
	private void renderSheet(GuiGraphics graphics, WefaxReceiverBlockEntity receiver, float partialTick) {
		if(!(receiver.isPrinting() || receiver.hasReadySheet()) || minecraft == null || minecraft.level == null)
			return;
		updateChart(receiver.getPixels());
		float progress = receiver.isPrinting() ? receiver.progress(minecraft.level.getGameTime()) : 1.0F;
		int risen = Math.round(PAPER[3] * progress);
		int slotTop = topPos + SLOT[1];
		int x = leftPos + SLOT[0];
		int y = slotTop - risen;
		graphics.blit(SPRITES, x, y, PAPER[0], PAPER[1], PAPER[2], risen, SIZE, SIZE);
		int margin = (PAPER[2] - WefaxChart.SIZE) / 2;
		int rows = Math.max(0, Math.min(WefaxChart.SIZE, risen - margin));
		if(rows > 0)
			graphics.blit(CHART, x + margin, y + margin, 0, 0, WefaxChart.SIZE, rows, WefaxChart.SIZE, WefaxChart.SIZE);
	}

	/** Puts the sheet's picture into the chart texture when it changes. */
	private void updateChart(byte[] pixels) {
		if(pixels.length != WefaxChart.SIZE * WefaxChart.SIZE || Arrays.equals(pixels, shown))
			return;
		if(chart == null) {
			chart = new DynamicTexture(WefaxChart.SIZE, WefaxChart.SIZE, false);
			Minecraft.getInstance().getTextureManager().register(CHART, chart);
		}
		NativeImage image = chart.getPixels();
		for(int z = 0; z < WefaxChart.SIZE; z++)
			for(int x = 0; x < WefaxChart.SIZE; x++)
				image.setPixelRGBA(x, z, MapColor.getColorFromPackedId(pixels[x + z * WefaxChart.SIZE] & 0xFF));
		chart.upload();
		shown = pixels.clone();
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
		// Everything is on the machine's art.
	}

	@Override
	protected void renderPanelTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		WefaxReceiverBlockEntity receiver = menu.blockEntity;
		for(int i = 0; i < BUTTONS.length; i++)
			if(inside(screen(BUTTONS[i]), mouseX, mouseY)) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(NAMES[i]));
				if(i == 2)
					lines.add(Component.translatable(receiver.isAuto() ? "screen.radiocraft.wefax.auto_on" : "screen.radiocraft.wefax.auto_off"));
				graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
				return;
			}
		if(receiver.hasReadySheet() && !receiver.isPrinting() && inside(screen(sheetBox()), mouseX, mouseY))
			graphics.renderTooltip(font, Component.translatable("screen.radiocraft.wefax.take"), mouseX, mouseY);
		else if(inside(screen(LEVEL), mouseX, mouseY))
			graphics.renderTooltip(font, Component.translatable("screen.radiocraft.wefax.paper", receiver.getPaper()), mouseX, mouseY);
		else if(inside(screen(LAMP), mouseX, mouseY))
			graphics.renderTooltip(font, Component.translatable(receiver.clientHasRadio() ? "screen.radiocraft.wefax.radio" : "screen.radiocraft.wefax.no_radio"), mouseX, mouseY);
	}

	@Override
	public void removed() {
		super.removed();
		if(chart != null) {
			Minecraft.getInstance().getTextureManager().release(CHART);
			chart = null;
		}
	}
}

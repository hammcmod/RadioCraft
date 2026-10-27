package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.network.serverbound.SHandheldDfFrequencyPacket;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * The handheld direction finder's dial: type a frequency in MHz (any amateur band the finder covers) and press
 * ENTER or SET. Drawn as the set's olive face with an amber readout.
 */
public class HandheldDirectionFinderScreen extends Screen {

	private static final int WIDTH = 176, HEIGHT = 92;
	private static final int OLIVE = 0xFF4A5233, OLIVE_DARK = 0xFF2E331F, OLIVE_LIGHT = 0xFF6B7548;
	private static final int AMBER = 0xFFFFB020, PANEL = 0xFF14140E;

	private final InteractionHand hand;
	private final float frequency;
	private EditBox input;
	private String error = "";

	public HandheldDirectionFinderScreen(InteractionHand hand, float frequency) {
		super(Component.translatable("item.radiocraft.handheld_direction_finder"));
		this.hand = hand;
		this.frequency = frequency;
	}

	public static void open(InteractionHand hand, float frequency) {
		Minecraft.getInstance().setScreen(new HandheldDirectionFinderScreen(hand, frequency));
	}

	@Override
	protected void init() {
		int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
		input = new EditBox(font, left + 14, top + 38, 96, 16, Component.literal("MHz"));
		input.setValue(String.format(Locale.ROOT, "%.3f", frequency / 1.0E6D));
		input.setMaxLength(10);
		input.setTextColor(AMBER);
		input.setFilter(text -> text.matches("[0-9]*\\.?[0-9]*"));
		addRenderableWidget(input);
		setInitialFocus(input);
		addRenderableWidget(Button.builder(Component.literal("SET"), button -> apply()).bounds(left + 118, top + 37, 44, 18).build());
	}

	private void apply() {
		try {
			float hertz = (float)(Double.parseDouble(input.getValue()) * 1.0E6D);
			boolean inBand = false;
			for(Band band : DirectionFinderBlockEntity.bands())
				inBand |= hertz >= band.minFrequency() && hertz <= band.maxFrequency();
			if(!inBand) {
				error = "NOT IN AN AMATEUR BAND";
				return;
			}
			SHandheldDfFrequencyPacket.send(hand, hertz);
			onClose();
		}
		catch(NumberFormatException e) {
			error = "TYPE A FREQUENCY IN MHZ";
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			apply();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
		guiGraphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, OLIVE_DARK);
		guiGraphics.fill(left, top, left + WIDTH, top + HEIGHT, OLIVE);
		guiGraphics.fill(left, top, left + WIDTH, top + 1, OLIVE_LIGHT);
		guiGraphics.fill(left + 8, top + 30, left + WIDTH - 8, top + 62, PANEL);
		for(int[] screw : new int[][] { {4, 4}, {WIDTH - 7, 4}, {4, HEIGHT - 7}, {WIDTH - 7, HEIGHT - 7} })
			guiGraphics.fill(left + screw[0], top + screw[1], left + screw[0] + 3, top + screw[1] + 3, OLIVE_DARK);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
		guiGraphics.drawString(font, "LOOP DIRECTION FINDER", left + 10, top + 10, 0xFFE8E0C4, false);
		guiGraphics.drawString(font, "FREQ MHZ", left + 14, top + 58 - 32 + 2, 0xFFB8B090, false);
		guiGraphics.drawString(font, error.isEmpty() ? "ENTER TO SET" : error, left + 14, top + 70, error.isEmpty() ? 0xFFB8B090 : 0xFFFF6040, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

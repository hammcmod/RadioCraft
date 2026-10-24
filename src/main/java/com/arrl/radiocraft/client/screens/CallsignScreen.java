package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.network.clientbound.CCallsignPromptPacket;
import com.arrl.radiocraft.common.network.serverbound.SClaimCallsignPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Asks a player without a callsign to choose one (see {@code CallsignPrompt}): the form, a box, Claim and Later. The
 * server checks the claim and answers with an error to show or a close.
 */
public class CallsignScreen extends Screen {

	private final String digits;
	private EditBox box;
	private Component error = null;

	public CallsignScreen(String digits) {
		super(Component.translatable("screen.radiocraft.callsign.title"));
		this.digits = digits;
	}

	/** Handles the server's prompt packet. */
	public static void handle(int state, String text) {
		Minecraft minecraft = Minecraft.getInstance();
		switch(state) {
			case CCallsignPromptPacket.OPEN -> minecraft.setScreen(new CallsignScreen(text));
			case CCallsignPromptPacket.ERROR -> {
				if(minecraft.screen instanceof CallsignScreen screen)
					screen.error = Component.translatable(text, screen.digits);
			}
			case CCallsignPromptPacket.DONE -> {
				if(minecraft.screen instanceof CallsignScreen)
					minecraft.setScreen(null);
			}
			default -> {}
		}
	}

	@Override
	protected void init() {
		int x = width / 2 - 60, y = height / 2 - 10;
		box = addRenderableWidget(new EditBox(font, x, y, 120, 18, Component.translatable("screen.radiocraft.callsign.box")));
		box.setMaxLength(6);
		box.setFilter(s -> s.matches("[A-Za-z0-9]*"));
		box.setResponder(s -> {
			String upper = s.toUpperCase(Locale.ROOT);
			if(!upper.equals(s))
				box.setValue(upper);
		});
		setInitialFocus(box);
		addRenderableWidget(Button.builder(Component.translatable("screen.radiocraft.callsign.claim"), b -> claim()).bounds(x, y + 26, 58, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.radiocraft.callsign.later"), b -> onClose()).bounds(x + 62, y + 26, 58, 20).build());
	}

	private void claim() {
		if(!box.getValue().isEmpty())
			SClaimCallsignPacket.send(box.getValue());
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == 257 || keyCode == 335) { // Enter.
			claim();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int cx = width / 2, y = height / 2 - 62;
		graphics.drawCenteredString(font, title, cx, y, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.translatable("screen.radiocraft.callsign.form"), cx, y + 16, 0xC0C0C0);
		graphics.drawCenteredString(font, Component.translatable("screen.radiocraft.callsign.digits", digits), cx, y + 28, 0xC0C0C0);
		if(error != null)
			graphics.drawCenteredString(font, error, cx, height / 2 + 42, 0xFF5555);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

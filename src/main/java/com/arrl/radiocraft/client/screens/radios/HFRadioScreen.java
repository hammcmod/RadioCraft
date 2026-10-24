package com.arrl.radiocraft.client.screens.radios;

import java.util.Deque;
import java.util.ArrayDeque;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import com.arrl.radiocraft.common.radio.voice.RepeaterAudio;
import com.arrl.radiocraft.client.RadiocraftKeys;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.menus.RadioMenu;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class HFRadioScreen<T extends RadioMenu<? extends HFRadioBlockEntity>> extends RadioScreen<T> {

	public HFRadioScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, ResourceLocation widgetsTexture) {
		super(menu, inventory, title, texture, widgetsTexture);
	}

	@Override
	public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

		if(menu.blockEntity.getCWEnabled()) {
			if(RadiocraftClientValues.SCREEN_PTT_PRESSED || cwKeyHeld)
				menu.blockEntity.getCWSendBuffer().setShouldAccumulate();
		}
		if(chatBox != null) {
			boolean show = menu.blockEntity.getCWEnabled();
			chatBox.visible = show;
			chatSend.visible = show;
			if(show && !chatQueue.isEmpty())
				pGuiGraphics.drawString(font, Component.translatable("screen.radiocraft.cw.sending"), chatBox.getX(), chatBox.getY() - 10, 0xFFB347);
		}
		RadiocraftClientValues.SCREEN_CW_ENABLED = menu.blockEntity.getCWEnabled();
		RadiocraftClientValues.SCREEN_VOICE_ENABLED = menu.blockEntity.getSSBEnabled();
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	}

	@Override
	protected String modeLabel() {
		if(menu.blockEntity.getCWEnabled())
			return "CW";
		return menu.blockEntity.getSSBEnabled() ? "SSB" : null;
	}

	// -------------------- KEYBOARD CW --------------------

	/** The keyboard straight key ({@link RadiocraftKeys#CW_KEY}) is down. */
	private boolean cwKeyHeld = false;
	/** Chat Morse still to send, one entry per dot length (one tick each, about 24 words a minute). */
	private final Deque<Boolean> chatQueue = new ArrayDeque<>();
	@Nullable
	private EditBox chatBox;
	@Nullable
	private Button chatSend;

	/** False for screens that cannot send (the HF receiver). */
	protected boolean canSendCw() {
		return true;
	}

	@Override
	protected void init() {
		super.init();
		chatBox = null;
		chatSend = null;
		if(!canSendCw() || RadiocraftServerConfig.CW_KEYING.get() != RadiocraftServerConfig.CwKeying.CHAT_MORSE)
			return;
		// Chat Morse: typed text goes out as Morse, in time. Under the radio, shown while it is in CW.
		int w = Math.min(imageWidth, 220);
		int x = leftPos + (imageWidth - w) / 2, y = topPos + imageHeight + 14;
		chatBox = addRenderableWidget(new EditBox(font, x, y, w - 46, 16, Component.translatable("screen.radiocraft.cw.chat")));
		chatBox.setMaxLength(64);
		chatBox.setHint(Component.translatable("screen.radiocraft.cw.chat"));
		chatSend = addRenderableWidget(Button.builder(Component.translatable("screen.radiocraft.cw.send"), b -> sendChat()).bounds(x + w - 42, y, 42, 16).build());
	}

	private void sendChat() {
		if(chatBox == null || chatBox.getValue().isBlank() || !menu.blockEntity.getCWEnabled())
			return;
		chatQueue.addAll(RepeaterAudio.morseUnits(chatBox.getValue()));
		chatBox.setValue("");
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		Boolean unit = chatQueue.poll();
		if(unit != null && unit && menu.blockEntity.getCWEnabled())
			menu.blockEntity.getCWSendBuffer().setShouldAccumulate();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(canSendCw() && menu.blockEntity.getCWEnabled() && RadiocraftKeys.CW_KEY.matches(keyCode, scanCode)) {
			cwKeyHeld = true;
			return true;
		}
		if(chatBox != null && chatBox.isFocused() && chatBox.visible) {
			if(keyCode == 257 || keyCode == 335) { // Enter sends.
				sendChat();
				return true;
			}
			if(keyCode != 256) // Escape still closes; everything else is typing.
				return chatBox.keyPressed(keyCode, scanCode, modifiers) || chatBox.canConsumeInput();
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if(RadiocraftKeys.CW_KEY.matches(keyCode, scanCode)) {
			cwKeyHeld = false;
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	/** CW has no PTT on the server: keying is local, so the readout follows our own key while in CW. */
	@Override
	protected boolean isTransmitting() {
		return super.isTransmitting() || (menu.blockEntity.getCWEnabled() && (RadiocraftClientValues.SCREEN_PTT_PRESSED || cwKeyHeld || !chatQueue.isEmpty()));
	}

	/**
	 * Callback for CW toggle buttons.
	 */
	protected void onPressCW(ValueButton button) {
		if(menu.blockEntity.getCWEnabled())
			return; // Mode buttons select, like a radio's: pressing the active one does nothing.
		SBlockRadioControlPacket.updateServer(menu.blockEntity.getBlockPos(), SBlockRadioControlPacket.Action.SET_CW, true);
		menu.blockEntity.setCWEnabled(true); // Update instantly for GUI (this also releases SSB), server will re-sync this value though.
	}

}

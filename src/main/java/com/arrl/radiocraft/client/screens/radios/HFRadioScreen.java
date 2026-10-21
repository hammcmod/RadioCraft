package com.arrl.radiocraft.client.screens.radios;

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
			if(RadiocraftClientValues.SCREEN_PTT_PRESSED)
				menu.blockEntity.getCWSendBuffer().setShouldAccumulate();
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

	/** CW has no PTT on the server: keying is local, so the readout follows our own key while in CW. */
	@Override
	protected boolean isTransmitting() {
		return super.isTransmitting() || (menu.blockEntity.getCWEnabled() && RadiocraftClientValues.SCREEN_PTT_PRESSED);
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

package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.menus.RadioMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class HFRadioScreen<T extends RadioMenu<? extends HFRadioBlockEntity>> extends RadioScreen<T> {

	public HFRadioScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, ResourceLocation widgetsTexture) {
		super(menu, inventory, title, texture, widgetsTexture);
	}

	@Override
	public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

		if(menu.blockEntity.getCWEnabled()) {
			if(RadiocraftClientValues.SCREEN_PTT_PRESSED)
				menu.blockEntity.getCWSendBuffer().setShouldAccumulate();
		}
		RadiocraftClientValues.SCREEN_CW_ENABLED = menu.blockEntity.getCWEnabled();
		RadiocraftClientValues.SCREEN_VOICE_ENABLED = menu.blockEntity.getSSBEnabled();
	}

	@Override
	public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	}

	/**
	 * Callback for CW toggle buttons.
	 */
	protected void onPressCW(ValueButton button) {
		boolean cwEnabled = menu.blockEntity.getCWEnabled();

		//RadiocraftPackets.sendToServer(new SRadioCWPacket(menu.blockEntity.getBlockPos(), !cwEnabled));
		menu.blockEntity.setCWEnabled(!cwEnabled); // Update instantly for GUI, server will re-sync this value though.
	}

}

package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.HFRadio10mMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadio10mScreen extends HFRadioScreen<HFRadio10mMenu> {

	public HFRadio10mScreen(HFRadio10mMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, guiTexture(menu, "hf_radio_10m", ""), guiTexture(menu, "hf_radio_10m", "_widgets"));

		this.imageWidth = 250;
		this.imageHeight = 147;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 13, topPos + 14, 14, 17, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new ValueButton(leftPos + 197, topPos + 66, 34, 19, 0, 34, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
		addRenderableWidget(new ValueButton(leftPos + 197, topPos + 86, 34, 19, 0, 72, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 128, topPos + 110, 51, 19, 0, 110, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 134, topPos + 37, 42, 45, 102, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown));
		
		addRenderableWidget(new HoldButton(leftPos + 129, topPos + 93, 25, 17, 0, 148, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonUp(null), (btn) -> {}));
		addRenderableWidget(new HoldButton(leftPos + 154, topPos + 93, 25, 17, 0, 182, widgetsTexture, 256, 256, (btn) -> onFrequencyButtonDown(null), (btn) -> {}));
		
		addRenderableWidget(new Dial(leftPos + 209, topPos + 20, 32, 34, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing));
		micGainDial = addRenderableWidget(new Dial(leftPos + 90, topPos + 86, 32, 34, 102, 90, widgetsTexture, 256, 256, this::onMicGainUp, this::onMicGainDown)); // Mic gain dial
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}

	@Override
	protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		renderDisplay(pGuiGraphics, 20, 36, 96, 44, 1.5F, 0xFFFFFF); // Display window on the texture
	}

	@Override
	protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if(menu.isPowered()) {
//			if(menu.blockEntity.isTransmitting())
//				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
//			if(menu.blockEntity.isReceiving())
//				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
		}
	}

}

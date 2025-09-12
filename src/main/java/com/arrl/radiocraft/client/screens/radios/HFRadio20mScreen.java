package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.HFRadio20mMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadio20mScreen extends HFRadioScreen<HFRadio20mMenu> {

	public HFRadio20mScreen(HFRadio20mMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_20m.png"), Radiocraft.id("textures/gui/hf_radio_20m_widgets.png"));

		this.imageWidth = 249;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 7, topPos + 5, 14, 17, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new ValueButton(leftPos + 74, topPos + 74, 30, 21, 0, 34, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
		addRenderableWidget(new ValueButton(leftPos + 74, topPos + 53, 30, 21, 0, 76, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 200, topPos + 11, 34, 21, 0, 118, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 125, topPos + 54, 62, 68, 68, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new ImageButton(leftPos + 103, topPos + 55, 22, 19, 0, 160, widgetsTexture, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
		addRenderableWidget(new ImageButton(leftPos + 103, topPos + 75, 22, 19, 0, 198, widgetsTexture, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
		addRenderableWidget(new Dial(leftPos + 193, topPos + 52, 28, 31, 68, 136, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 193, topPos + 92, 28, 31, 68, 136, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		/*
		if(menu.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(1.5F, 1.5F, 1.5F);
			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 85) / 1.5F, (topPos + 16) / 1.5F, 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}*/
	}

	@Override
	protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
//		if(menu.blockEntity.isPowered()) {
//			if(menu.blockEntity.isTransmitting())
//				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
//			if(menu.blockEntity.isReceiving())
//				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
//		}
	}

}
package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.HFRadio40mMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class HFRadio40mScreen extends HFRadioScreen<HFRadio40mMenu> {

	public HFRadio40mScreen(HFRadio40mMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/hf_radio_40m.png"), Radiocraft.id("textures/gui/hf_radio_40m_widgets.png"));

		this.imageWidth = 250;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 23, topPos + 26, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new ValueButton(leftPos + 161, topPos + 34, 34, 19, 0, 42, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
		addRenderableWidget(new ValueButton(leftPos + 161, topPos + 15, 34, 19, 0, 80, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 172, topPos + 99, 51, 19, 0, 118, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 103, topPos + 63, 42, 45, 102, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 56, topPos + 58, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 56, topPos + 23, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // dial1
		addRenderableWidget(new Dial(leftPos + 56, topPos + 93, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // dial2
		addRenderableWidget(new Dial(leftPos + 200, topPos + 58, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
		addRenderableWidget(new ToggleButton(false, leftPos + 22, topPos + 62, 22, 22, 0, 200, widgetsTexture, 256, 256, (btn) -> {})); // SPK 
		addRenderableWidget(new ToggleButton(false, leftPos + 167, topPos + 62, 22, 22, 0, 156, widgetsTexture, 256, 256, (btn) -> {})); // MIC
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
		/*
		if(menu.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(0.65F, 0.65F, 0.65F);
			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 110) / 0.65F, (topPos + 48) / 0.65F, 0x222222); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

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
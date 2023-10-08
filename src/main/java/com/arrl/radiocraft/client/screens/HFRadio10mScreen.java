package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.HFRadio10mMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio10mScreen extends HFRadioScreen<HFRadio10mMenu> {

	public HFRadio10mScreen(HFRadio10mMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.location("textures/gui/hf_radio_10m.png"), Radiocraft.location("textures/gui/hf_radio_10m_widgets.png"));

		this.imageWidth = 250;
		this.imageHeight = 147;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.blockEntity.isPowered(), leftPos + 13, topPos + 14, 14, 17, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new ValueButton(leftPos + 197, topPos + 66, 34, 19, 0, 34, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
		addRenderableWidget(new ValueButton(leftPos + 197, topPos + 86, 34, 19, 0, 72, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 128, topPos + 110, 51, 19, 0, 110, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 134, topPos + 37, 42, 45, 102, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new ImageButton(leftPos + 129, topPos + 93, 25, 17, 0, 148, widgetsTexture, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
		addRenderableWidget(new ImageButton(leftPos + 154, topPos + 93, 25, 17, 0, 182, widgetsTexture, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
		addRenderableWidget(new Dial(leftPos + 209, topPos + 20, 32, 34, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 90, topPos + 86, 32, 34, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(isHovering(33, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.tx")), mouseX, mouseY);
		if(isHovering(62, 18, 25, 11, mouseX, mouseY))
			renderTooltip(poseStack, Component.translatable(Radiocraft.translationKey("screen", "radio.rx")), mouseX, mouseY);

		if(menu.blockEntity.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(1.5F, 1.5F, 1.5F);
			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 24) / 1.5F, (topPos + 50) / 1.5F, 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}
	}

	@Override
	protected void renderAdditionalBg(PoseStack poseStack, float partialTicks, int x, int y) {
//		if(menu.blockEntity.isPowered()) {
//			if(menu.blockEntity.isTransmitting())
//				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
//			if(menu.blockEntity.isReceiving())
//				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
//		}
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int x, int y) {}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}
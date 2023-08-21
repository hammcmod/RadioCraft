package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.menus.AbstractHFRadioMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio40mScreen extends AbstractHFRadioScreen {

	public HFRadio40mScreen(AbstractHFRadioMenu container, Inventory playerInventory, Component title) {
		super(container, playerInventory, title, Radiocraft.location("textures/gui/hf_radio_40m.png"), Radiocraft.location("textures/gui/hf_radio_40m_widgets.png"));

		this.imageWidth = 250;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(container.isPowered(), leftPos + 23, topPos + 26, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button

		addRenderableWidget(new ToggleButton(container.blockEntity.getCWEnabled(), leftPos + 161, topPos + 34, 34, 19, 0, 42, widgetsTexture, 256, 256, this::onPressCW)); // CW Button
		addRenderableWidget(new ToggleButton(container.blockEntity.getSSBEnabled(), leftPos + 161, topPos + 15, 34, 19, 0, 80, widgetsTexture, 256, 256, this::onPressSSB)); // SSB button
		addRenderableWidget(new HoldButton(leftPos + 172, topPos + 99, 51, 19, 0, 118, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 103, topPos + 63, 42, 45, 102, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 56, topPos + 58, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 200, topPos + 58, 28, 30, 102, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(container.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(0.65F, 0.65F, 0.65F);
			float freqMhz = container.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 110) / 0.65F, (topPos + 48) / 0.65F, 0x222222); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}
	}

	@Override
	protected void renderAdditionalBg(PoseStack poseStack, float partialTicks, int x, int y) {
		if(container.isPowered()) {
			if(container.isTransmitting())
				blit(poseStack, leftPos + 30, topPos + 15, 1, 148, 29, 15);
			if(container.isReceiving())
				blit(poseStack, leftPos + 59, topPos + 15, 30, 148, 29, 15);
		}
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int x, int y) {}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}
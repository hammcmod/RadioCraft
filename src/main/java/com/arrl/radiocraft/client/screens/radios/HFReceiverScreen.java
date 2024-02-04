package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.Dial;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.client.screens.widgets.ValueButton;
import com.arrl.radiocraft.common.menus.HFReceiverMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HFReceiverScreen extends HFRadioScreen<HFReceiverMenu> {

	public HFReceiverScreen(HFReceiverMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.location("textures/gui/hf_receiver.png"), Radiocraft.location("textures/gui/hf_receiver_widgets.png"));

		this.imageWidth = 250;
		this.imageHeight = 130;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.blockEntity.isPowered(), leftPos + 5, topPos + 5, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new ValueButton(leftPos + 10, topPos + 78, 34, 21, 0, 84, widgetsTexture, 256, 256, () -> menu.blockEntity.getCWEnabled(), this::onPressCW)); // CW Button
		addRenderableWidget(new ValueButton(leftPos + 10, topPos + 58, 34, 21, 0, 42, widgetsTexture, 256, 256, () -> menu.blockEntity.getSSBEnabled(), this::onPressSSB)); // SSB button
		addRenderableWidget(new Dial(leftPos + 103, topPos + 63, 42, 45, 68, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 56, topPos + 93, 28, 31, 68, 90, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(menu.blockEntity.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			poseStack.scale(0.55F, 0.55F, 0.55F);
			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 111) / 0.55F, (topPos + 48) / 0.55F, 0x222222); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

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
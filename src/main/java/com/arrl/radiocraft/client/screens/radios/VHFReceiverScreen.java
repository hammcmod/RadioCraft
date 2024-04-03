package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class VHFReceiverScreen extends VHFRadioScreen<VHFReceiverMenu> {

	public VHFReceiverScreen(VHFReceiverMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.location("textures/gui/vhf_receiver.png"), Radiocraft.location("textures/gui/vhf_receiver_widgets.png"));

		this.imageWidth = 251;
		this.imageHeight = 106;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.blockEntity.isPowered(), leftPos + 10, topPos + 11, 14, 19, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new Dial(leftPos + 29, topPos + 11, 28, 31, 28, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 187, topPos + 11, 28, 31, 28, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {

	}

	@Override
	protected void renderAdditionalBg(PoseStack poseStack, float partialTicks, int x, int y) {

	}

	@Override
	protected void renderLabels(PoseStack poseStack, int x, int y) {}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		mouseX -= leftPos;
		mouseY -= topPos;
		return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
	}

}
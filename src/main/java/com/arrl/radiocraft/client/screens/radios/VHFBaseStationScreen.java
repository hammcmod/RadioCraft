package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class VHFBaseStationScreen extends VHFRadioScreen<VHFBaseStationMenu> {

	public VHFBaseStationScreen(VHFBaseStationMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/vhf_base_station.png"), Radiocraft.id("textures/gui/vhf_base_station_widgets.png"));

		this.imageWidth = 249;
		this.imageHeight = 104;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 7, topPos + 6, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new HoldButton(leftPos + 6, topPos + 27, 22, 22, 0, 66, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // PTT button
		addRenderableWidget(new Dial(leftPos + 48, topPos + 59, 42, 42, 44, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new ImageButton(leftPos + 93, topPos + 65, 18, 12, 0, 42, widgetsTexture, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
		addRenderableWidget(new ImageButton(leftPos + 93, topPos + 82, 18, 12, 0, 42, widgetsTexture, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
		addRenderableWidget(new Dial(leftPos + 231, topPos + 5, 15, 17, 44, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 231, topPos + 31, 15, 17, 44, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
	}

	@Override
	protected void renderAdditionalTooltips(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(menu.isPowered()) {
			poseStack.pushPose(); // Push/pop allows you to add a set of transformations to the stack. Pushing starts a new set and popping reverts to the previous set.

			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 46), (topPos + 18), 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}
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
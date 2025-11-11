package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

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
		addRenderableWidget(new HoldButton(leftPos + 6, topPos + 27, 22, 22, 0, 66, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // MIC
		addRenderableWidget(new Dial(leftPos + 48, topPos + 59, 42, 42, 44, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 133, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Dial1
		addRenderableWidget(new Dial(leftPos + 170, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Dial2
		addRenderableWidget(new Dial(leftPos + 231, topPos + 5, 15, 17, 44, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		addRenderableWidget(new Dial(leftPos + 231, topPos + 31, 15, 17, 44, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Mic gain dial
		addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 3, 22, 22, 0, 110, widgetsTexture, 256, 256, (btn) -> {})); // SPK
        addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 29, 22, 22, 0, 66, widgetsTexture, 256, 256, (btn) -> {})); // MIC
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 65, 18, 12, 0, 42, widgetsTexture, 256, 256, (btn) -> {})); // Frequency up button
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 82, 18, 12, 0, 42, widgetsTexture, 256, 256, (btn) -> {})); // Frequency down button
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

			float freqMhz = menu.getFrequency() / 1000.0F; // Frequency is in kHz, divide by 1000 to get MHz
			font.draw(poseStack, String.format("%.3f", freqMhz) + "MHz", (leftPos + 46), (topPos + 18), 0xFFFFFF); // Divide the positions rendered at by 1.5F as the entire pose was scaled by 1.5F.

			poseStack.popPose(); // Reset pose stack. Will cause a memory leak if you push without popping.
		}*/
	}


}
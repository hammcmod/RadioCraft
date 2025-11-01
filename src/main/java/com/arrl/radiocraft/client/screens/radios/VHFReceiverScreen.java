package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class VHFReceiverScreen extends VHFRadioScreen<VHFReceiverMenu> {

	public VHFReceiverScreen(VHFReceiverMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/vhf_receiver.png"), Radiocraft.id("textures/gui/vhf_receiver_widgets.png"));

		this.imageWidth = 251;
		this.imageHeight = 106;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 10, topPos + 11, 14, 19, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new Dial(leftPos + 29, topPos + 11, 28, 31, 28, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 187, topPos + 11, 28, 31, 28, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Gain dial
		
		// Memory buttons (1-6) - positioned over numeric display buttons
		// TODO: Replace with correct button sprites in vhf_receiver_widgets.png
		addRenderableWidget(new ImageButton(leftPos + 40, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(1)));
		addRenderableWidget(new ImageButton(leftPos + 70, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(2)));
		addRenderableWidget(new ImageButton(leftPos + 100, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(3)));
		addRenderableWidget(new ImageButton(leftPos + 130, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(4)));
		addRenderableWidget(new ImageButton(leftPos + 160, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(5)));
		addRenderableWidget(new ImageButton(leftPos + 190, topPos + 75, 22, 10, 56, 0, widgetsTexture, 256, 256, (btn) -> onMemoryButton(6)));
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}

	/**
	 * Callback for memory buttons. No functionality implemented yet.
	 */
	protected void onMemoryButton(int buttonNumber) {
		// TODO: Implement memory save/recall functionality
	}

}
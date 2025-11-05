package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class VHFReceiverScreen extends VHFRadioScreen<VHFReceiverMenu> {

	private StaticToggleButton[] ledButtons = new StaticToggleButton[6];

	public VHFReceiverScreen(VHFReceiverMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/vhf_receiver.png"), Radiocraft.id("textures/gui/vhf_receiver_widgets.png"));

		this.imageWidth = 251;
		this.imageHeight = 106;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 10, topPos + 11, 14, 19, 0, 0, widgetsTexture, 256, 256, this::onPressPower));
		addRenderableWidget(new StaticToggleButton(false, leftPos + 221, topPos + 10, 20, 20, 221, 112, texture, 256, 256, (btn) -> {}));
		
		ledButtons[0] = new StaticToggleButton(false, leftPos + 43, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		ledButtons[1] = new StaticToggleButton(false, leftPos + 73, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		ledButtons[2] = new StaticToggleButton(false, leftPos + 102, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		ledButtons[3] = new StaticToggleButton(false, leftPos + 132, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		ledButtons[4] = new StaticToggleButton(false, leftPos + 161, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		ledButtons[5] = new StaticToggleButton(false, leftPos + 191, topPos + 50, 17, 17, 0, 106, texture, 256, 256, (btn) -> {});
		
		for (StaticToggleButton led : ledButtons) {
			addRenderableWidget(led);
		}
		
		addRenderableWidget(new ImageButton(leftPos + 40, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(1)));
		addRenderableWidget(new ImageButton(leftPos + 70, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(2)));
		addRenderableWidget(new ImageButton(leftPos + 100, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(3)));
		addRenderableWidget(new ImageButton(leftPos + 130, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(4)));
		addRenderableWidget(new ImageButton(leftPos + 160, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(5)));
		addRenderableWidget(new ImageButton(leftPos + 190, topPos + 75, 22, 10, -1, -1, widgetsTexture, 256, 256, (btn) -> onMemoryButton(6)));
		
		addRenderableWidget(new Dial(leftPos + 30, topPos + 12, 28, 31, 28, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown));
		addRenderableWidget(new Dial(leftPos + 188, topPos + 12, 28, 31, 28, 0, widgetsTexture, 256, 256, this::doNothing, this::doNothing));
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}

	/**
	 * Callback for memory buttons - toggles corresponding LED.
	 */
	protected void onMemoryButton(int buttonNumber) {
		if (buttonNumber >= 1 && buttonNumber <= 6) {
			StaticToggleButton led = ledButtons[buttonNumber - 1];
			led.isToggled = !led.isToggled;
		}
	}

}
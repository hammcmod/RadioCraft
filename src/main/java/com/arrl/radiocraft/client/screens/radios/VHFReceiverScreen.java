package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import com.arrl.radiocraft.common.network.serverbound.SReceiverChannelSelectPacket;
import com.arrl.radiocraft.common.network.serverbound.SReceiverChannelSettingsUpdatePacket;
import com.arrl.radiocraft.common.network.serverbound.SRadioPowerUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class VHFReceiverScreen extends VHFRadioScreen<VHFReceiverMenu> {

	private static final int CHANNEL_COUNT = 6;
	private static final ResourceLocation SCREEN_TEXTURE = Radiocraft.id("textures/gui/vhf_base_station.png");
	private static final int SCREEN_U = 37;
	private static final int SCREEN_V = 11;
	private static final int SCREEN_WIDTH = 80;
	private static final int SCREEN_HEIGHT = 20;
	private static final int SCREEN_TEXT_COLOR = 0xF2E9FF;

	private final LedIndicator[] channelLeds = new LedIndicator[CHANNEL_COUNT];
	private final ToggleButton[] channelButtons = new ToggleButton[CHANNEL_COUNT];
	private ToggleButton powerToggle;
	private Dial gainDial;

	public VHFReceiverScreen(VHFReceiverMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/vhf_receiver.png"), Radiocraft.id("textures/gui/vhf_receiver_widgets.png"));

		this.imageWidth = 251;
		this.imageHeight = 106;
	}

	@Override
	protected void init() {
		super.init();
		this.powerToggle = new ToggleButton(menu.isPowered(), leftPos + 10, topPos + 11, 14, 19, 0, 0, widgetsTexture, 256, 256, true, this::onPressPower);
		addRenderableWidget(this.powerToggle);
		addRenderableWidget(new ToggleButton(false, leftPos + 220, topPos + 9, 22, 22, 0, 67, widgetsTexture, 256, 256, (btn) -> {}));
		
		// LEDs - use LedIndicator that only renders when on and is not clickable
		channelLeds[0] = new LedIndicator(Component.literal("Channel LED 1"), leftPos + 43, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		channelLeds[1] = new LedIndicator(Component.literal("Channel LED 2"), leftPos + 73, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		channelLeds[2] = new LedIndicator(Component.literal("Channel LED 3"), leftPos + 102, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		channelLeds[3] = new LedIndicator(Component.literal("Channel LED 4"), leftPos + 132, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		channelLeds[4] = new LedIndicator(Component.literal("Channel LED 5"), leftPos + 161, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		channelLeds[5] = new LedIndicator(Component.literal("Channel LED 6"), leftPos + 191, topPos + 50, 17, 17, 0, 106, texture, 256, 256);
		
		// Initialize all LEDs as off
		for (LedIndicator led : channelLeds) {
			led.setIsOn(false);
			addRenderableWidget(led);
		}
		
		// Channel buttons
		channelButtons[0] = new ToggleButton(false, leftPos + 42, topPos + 73, 19, 19, 93, 0, widgetsTexture, 256, 256, (btn) -> onChannelButton(0));
		channelButtons[1] = new ToggleButton(false, leftPos + 72, topPos + 73, 19, 19, 93, 38, widgetsTexture, 256, 256, (btn) -> onChannelButton(1));
		channelButtons[2] = new ToggleButton(false, leftPos + 101, topPos + 73, 19, 19, 93, 76, widgetsTexture, 256, 256, (btn) -> onChannelButton(2));
		channelButtons[3] = new ToggleButton(false, leftPos + 131, topPos + 73, 19, 19, 93, 114, widgetsTexture, 256, 256, (btn) -> onChannelButton(3));
		channelButtons[4] = new ToggleButton(false, leftPos + 160, topPos + 73, 19, 19, 93, 152, widgetsTexture, 256, 256, (btn) -> onChannelButton(4));
		channelButtons[5] = new ToggleButton(false, leftPos + 190, topPos + 73, 19, 19, 93, 190, widgetsTexture, 256, 256, (btn) -> onChannelButton(5));
		for (ToggleButton button : channelButtons) {
			addRenderableWidget(button);
		}
		
		addRenderableWidget(new Dial(leftPos + 30, topPos + 12, 28, 31, 28, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown));
		this.gainDial = new Dial(leftPos + 188, topPos + 12, 28, 31, 28, 0, widgetsTexture, 256, 256, this::onGainUp, this::onGainDown);
		addRenderableWidget(this.gainDial);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		boolean powered = menu.isPowered();
		if(powerToggle != null)
			powerToggle.isToggled = powered;

		int selectedChannel = powered && menu.blockEntity != null ? menu.blockEntity.getSelectedChannel() : -1;
		for(int i = 0; i < CHANNEL_COUNT; i++) {
			boolean selected = i == selectedChannel;
			channelButtons[i].isToggled = powered && selected;
			channelLeds[i].setIsOn(powered && selected);
		}

		int screenX = leftPos + (imageWidth - SCREEN_WIDTH) / 2;
		int screenY = topPos + 12;
		pGuiGraphics.blit(SCREEN_TEXTURE, screenX, screenY, SCREEN_U, SCREEN_V, SCREEN_WIDTH, SCREEN_HEIGHT, 256, 256);
		if(powered && menu.blockEntity != null) {
			String freqText = String.format("%03.3f MHz", menu.blockEntity.getFrequency() / 1_000_000.0F);
			int textX = screenX + (SCREEN_WIDTH - this.font.width(freqText)) / 2;
			int textY = screenY + (SCREEN_HEIGHT - this.font.lineHeight) / 2;
			pGuiGraphics.drawString(this.font, freqText, textX, textY, SCREEN_TEXT_COLOR);
		}
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}

	@Override
	protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if(gainDial != null && gainDial.isHoveredOrChanging() && menu.blockEntity != null) {
			String strValue = String.format("%d%%", Math.round(menu.blockEntity.getGain() * 100));
			pGuiGraphics.drawString(
					this.font,
					strValue,
					gainDial.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL) - this.font.width(strValue) / 2,
					gainDial.getRectangle().getCenterInAxis(ScreenAxis.VERTICAL) - this.font.lineHeight / 2,
					0xFFFFFF
			);
		}
	}

	@Override
	protected void onPressPower(ToggleButton button) {
		if(menu.blockEntity == null)
			return;
		SRadioPowerUpdatePacket.updateServer(menu.blockEntity.getBlockPos(), button.isToggled);
	}

	protected void onChannelButton(int channel) {
		if(!menu.isPowered() || menu.blockEntity == null)
			return;
		menu.blockEntity.selectChannel(channel);
		SReceiverChannelSelectPacket.updateServer(menu.blockEntity.getBlockPos(), channel);
	}

	@Override
	protected void onFrequencyDialUp(Dial dial) {
		if(!menu.isPowered() || menu.blockEntity == null)
			return;
		menu.blockEntity.adjustChannelFrequency(1);
		sendChannelSettings();
	}

	@Override
	protected void onFrequencyDialDown(Dial dial) {
		if(!menu.isPowered() || menu.blockEntity == null)
			return;
		menu.blockEntity.adjustChannelFrequency(-1);
		sendChannelSettings();
	}

	private void onGainUp(Dial dial) {
		if(!menu.isPowered() || menu.blockEntity == null)
			return;
		menu.blockEntity.applyChannelGain(menu.blockEntity.getGain() + 0.1F);
		sendChannelSettings();
	}

	private void onGainDown(Dial dial) {
		if(!menu.isPowered() || menu.blockEntity == null)
			return;
		menu.blockEntity.applyChannelGain(menu.blockEntity.getGain() - 0.1F);
		sendChannelSettings();
	}

	private void sendChannelSettings() {
		if(menu.blockEntity == null)
			return;
		SReceiverChannelSettingsUpdatePacket.updateServer(
				menu.blockEntity.getBlockPos(),
				menu.blockEntity.getSelectedChannel(),
				menu.blockEntity.getFrequency(),
				menu.blockEntity.getGain()
		);
	}

}

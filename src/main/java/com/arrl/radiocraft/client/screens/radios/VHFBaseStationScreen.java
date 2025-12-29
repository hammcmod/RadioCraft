package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import com.arrl.radiocraft.common.network.serverbound.SRadioPowerUpdatePacket;
import com.arrl.radiocraft.common.network.serverbound.SRadioSettingsUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.navigation.ScreenAxis;
import org.jetbrains.annotations.NotNull;

public class VHFBaseStationScreen extends VHFRadioScreen<VHFBaseStationMenu> {

	private static final int LED_WIDTH = 14;
	private static final int LED_HEIGHT = 6;
	private static final int TX_LED_X = 133;
	private static final int RX_LED_X = 156;
	private static final int DATA_LED_X = 180;
	private static final int LED_Y = 42;
	private static final int LED_TX_COLOR = 0xFFB53030;
	private static final int LED_RX_COLOR = 0xFF30B35A;
	private static final int LED_DATA_COLOR = 0xFF3D73C9;

	private Dial gainDial;
	private Dial micGainDial;
	private ToggleButton powerToggle;

	public VHFBaseStationScreen(VHFBaseStationMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, Radiocraft.id("textures/gui/vhf_base_station.png"), Radiocraft.id("textures/gui/vhf_base_station_widgets.png"));

		this.imageWidth = 249;
		this.imageHeight = 104;
	}

	@Override
	protected void init() {
		super.init();
		this.powerToggle = new ToggleButton(menu.isPowered(), leftPos + 7, topPos + 6, 20, 21, 0, 0, widgetsTexture, 256, 256, true, this::onPressPower);
		addRenderableWidget(this.powerToggle); // Power button
		addRenderableWidget(new HoldButton(leftPos + 6, topPos + 27, 22, 22, 0, 66, widgetsTexture, 256, 256, true, this::onPressPTT, this::onReleasePTT)); // MIC
		addRenderableWidget(new Dial(leftPos + 48, topPos + 59, 42, 42, 44, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 133, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Dial1
		addRenderableWidget(new Dial(leftPos + 170, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, this::doNothing, this::doNothing)); // Dial2
		this.gainDial = new Dial(leftPos + 231, topPos + 5, 15, 17, 44, 84, widgetsTexture, 256, 256, this::onGainUp, this::onGainDown);
		addRenderableWidget(this.gainDial); // Gain dial
		this.micGainDial = new Dial(leftPos + 231, topPos + 31, 15, 17, 44, 84, widgetsTexture, 256, 256, this::onMicGainUp, this::onMicGainDown);
		addRenderableWidget(this.micGainDial); // Mic gain dial
		addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 3, 22, 22, 0, 110, widgetsTexture, 256, 256, (btn) -> {})); // SPK
        addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 29, 22, 22, 0, 66, widgetsTexture, 256, 256, (btn) -> {})); // MIC
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 65, 18, 12, 0, 42, widgetsTexture, 256, 256, this::onFrequencyButtonUp)); // Frequency up button
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 82, 18, 12, 0, 42, widgetsTexture, 256, 256, this::onFrequencyButtonDown)); // Frequency down button
	}

	@Override
	protected void onPressPower(ToggleButton button) {
		if(menu.blockEntity == null)
			return;
		SRadioPowerUpdatePacket.updateServer(menu.blockEntity.getBlockPos(), button.isToggled);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

	}

	@Override
	protected void renderAdditionalBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		boolean powered = menu.isPowered();
		if(powerToggle != null)
			powerToggle.isToggled = powered;

		if(powered) {
			pGuiGraphics.drawString(this.font, String.format("%03.3f MHz", menu.getFrequency() / 1_000_000.0F), leftPos + 60, topPos + 18, 0xF2E9FF);
		}

		boolean txOn = powered && com.arrl.radiocraft.client.RadiocraftClientValues.SCREEN_PTT_PRESSED;
		boolean rxOn = powered && menu.blockEntity != null && menu.blockEntity.isReceivingSignal();
		boolean dataOn = false;

		if(txOn)
			pGuiGraphics.fill(leftPos + TX_LED_X, topPos + LED_Y, leftPos + TX_LED_X + LED_WIDTH, topPos + LED_Y + LED_HEIGHT, LED_TX_COLOR);
		if(rxOn)
			pGuiGraphics.fill(leftPos + RX_LED_X, topPos + LED_Y, leftPos + RX_LED_X + LED_WIDTH, topPos + LED_Y + LED_HEIGHT, LED_RX_COLOR);
		if(dataOn)
			pGuiGraphics.fill(leftPos + DATA_LED_X, topPos + LED_Y, leftPos + DATA_LED_X + LED_WIDTH, topPos + LED_Y + LED_HEIGHT, LED_DATA_COLOR);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Don't render the default title and inventory labels
	}

	@Override
	protected void renderAdditionalTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		if(micGainDial != null && micGainDial.isHoveredOrChanging()) {
			String strValue = String.format("%d%%", Math.round(menu.blockEntity.getMicGain() * 100));
			pGuiGraphics.drawString(
					this.font,
					strValue,
					micGainDial.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL) - this.font.width(strValue) / 2,
					micGainDial.getRectangle().getCenterInAxis(ScreenAxis.VERTICAL) - this.font.lineHeight / 2,
					0xFFFFFF
			);
		}

		if(gainDial != null && gainDial.isHoveredOrChanging()) {
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
	protected void onFrequencyDialUp(Dial dial) {
		if(!menu.isPowered()) return;
		menu.blockEntity.adjustFrequency(1);
		updateServer();
	}

	@Override
	protected void onFrequencyDialDown(Dial dial) {
		if(!menu.isPowered()) return;
		menu.blockEntity.adjustFrequency(-1);
		updateServer();
	}

	protected void onFrequencyButtonUp(ToggleButton button) {
		if(!menu.isPowered()) return;
		menu.blockEntity.adjustFrequency(1);
		updateServer();
	}

	protected void onFrequencyButtonDown(ToggleButton button) {
		if(!menu.isPowered()) return;
		menu.blockEntity.adjustFrequency(-1);
		updateServer();
	}

	protected void onGainUp(Dial dial) {
		menu.blockEntity.setGain(menu.blockEntity.getGain() + 0.1f);
		updateServer();
	}

	protected void onGainDown(Dial dial) {
		menu.blockEntity.setGain(menu.blockEntity.getGain() - 0.1f);
		updateServer();
	}

	protected void onMicGainUp(Dial dial) {
		menu.blockEntity.setMicGain(menu.blockEntity.getMicGain() + 0.1f);
		updateServer();
	}

	protected void onMicGainDown(Dial dial) {
		menu.blockEntity.setMicGain(menu.blockEntity.getMicGain() - 0.1f);
		updateServer();
	}

	private void updateServer() {
		if(menu.blockEntity == null)
			return;
		SRadioSettingsUpdatePacket.updateServer(
				menu.blockEntity.getBlockPos(),
				menu.blockEntity.getFrequency(),
				menu.blockEntity.getGain(),
				menu.blockEntity.getMicGain()
		);
	}


}

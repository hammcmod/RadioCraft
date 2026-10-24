package com.arrl.radiocraft.client.screens.radios;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.*;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.VHFRadioBlockEntity;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class VHFBaseStationScreen extends VHFRadioScreen<VHFBaseStationMenu> {

	public VHFBaseStationScreen(VHFBaseStationMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, guiTexture(menu, "vhf_base_station", ""), guiTexture(menu, "vhf_base_station", "_widgets"));

		this.imageWidth = 249;
		this.imageHeight = 104;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new ToggleButton(menu.isPowered(), leftPos + 7, topPos + 6, 20, 21, 0, 0, widgetsTexture, 256, 256, this::onPressPower)); // Power button
		addRenderableWidget(new HoldButton(leftPos + 6, topPos + 27, 22, 22, 0, 66, widgetsTexture, 256, 256, this::onPressPTT, this::onReleasePTT)); // MIC
		addRenderableWidget(new Dial(leftPos + 48, topPos + 59, 42, 42, 44, 0, widgetsTexture, 256, 256, this::onFrequencyDialUp, this::onFrequencyDialDown)); // Frequency dial
		addRenderableWidget(new Dial(leftPos + 133, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, d -> stepTone(1), d -> stepTone(-1))); // Dial1: CTCSS tone
		addRenderableWidget(new Dial(leftPos + 170, topPos + 69, 28, 31, 74, 84, widgetsTexture, 256, 256, d -> cycleOffset(), d -> cycleOffset())); // Dial2: repeater offset
		// The three soft buttons under the second display: offset, tone down, tone up.
		addRenderableWidget(new PanelButton(leftPos + 133, topPos + 42, 15, 8, this::cycleOffset));
		addRenderableWidget(new PanelButton(leftPos + 155, topPos + 42, 15, 8, () -> stepTone(-1)));
		addRenderableWidget(new PanelButton(leftPos + 179, topPos + 42, 15, 8, () -> stepTone(1)));
		speakerGainDial = addRenderableWidget(new Dial(leftPos + 231, topPos + 5, 15, 17, 44, 84, widgetsTexture, 256, 256, this::onGainUp, this::onGainDown)); // Gain dial
		micGainDial = addRenderableWidget(new Dial(leftPos + 231, topPos + 31, 15, 17, 44, 84, widgetsTexture, 256, 256, this::onMicGainUp, this::onMicGainDown)); // Mic gain dial
		addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 3, 22, 22, 0, 110, widgetsTexture, 256, 256, (btn) -> {})); // SPK
        addRenderableWidget(new ToggleButton(false, leftPos + 210, topPos + 29, 22, 22, 0, 66, widgetsTexture, 256, 256, (btn) -> {})); // MIC
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 65, 18, 12, 0, 42, widgetsTexture, 256, 256, (btn) -> {})); // Frequency up button
		addRenderableWidget(new ToggleButton(false, leftPos + 93, topPos + 82, 18, 12, 0, 42, widgetsTexture, 256, 256, (btn) -> {})); // Frequency down button
	}

	private VHFRadioBlockEntity radio() {
		return (VHFRadioBlockEntity)menu.blockEntity;
	}

	/** Moves the CTCSS tone along the standard list (OFF at the bottom). */
	private void stepTone(int steps) {
		float tone = Ctcss.step(radio().getCtcssTone(), steps);
		SBlockRadioControlPacket.updateServer(menu.blockEntity.getBlockPos(), SBlockRadioControlPacket.Action.SET_TONE, Ctcss.index(tone));
		radio().setCtcssTone(tone); // Optimistic; the server re-syncs.
	}

	/** Simplex, then 600 kHz down, then up, for working through a repeater. */
	private void cycleOffset() {
		int offset = Ctcss.nextOffset(radio().getOffsetHz());
		SBlockRadioControlPacket.updateServer(menu.blockEntity.getBlockPos(), SBlockRadioControlPacket.Action.SET_OFFSET, offset);
		radio().setOffsetHz(offset);
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
		renderDisplay(pGuiGraphics, 43, 10, 66, 23, 0.9F, 0xFFFFFF); // Display window on the texture
		if(menu.getDisplayPhase() == RadioNetworkObject.PHASE_ON) { // Second display: repeater offset and tone.
			drawScaled(pGuiGraphics, "OFS " + Ctcss.formatOffset(radio().getOffsetHz()), leftPos + 162, topPos + 14, 0.7F, 0x202020);
			drawScaled(pGuiGraphics, "TONE " + Ctcss.format(radio().getCtcssTone()), leftPos + 162, topPos + 22, 0.7F, 0x202020);
		}
	}


}

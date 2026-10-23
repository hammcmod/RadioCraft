package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
import com.arrl.radiocraft.common.menus.RadioInterfaceMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * The radio interface's screen: the station callsign (Enter saves it; empty claims your own) and the TX switch. A plain
 * panel until it has art.
 */
public class RadioInterfaceScreen extends AbstractContainerScreen<RadioInterfaceMenu> {

	private static final int PANEL = 0xFF2A2F33;
	private static final int PANEL_EDGE = 0xFF151819;
	private static final int LABEL = 0xFFBFE8C4;
	private static final int DIM = 0xFF7E9A86;

	private EditBox callsignBox;
	private Button transmitButton;

	public RadioInterfaceScreen(RadioInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 82;
	}

	private RadioInterfaceBlockEntity radioInterface() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		callsignBox = new EditBox(font, leftPos + 10, topPos + 30, 90, 16, Component.translatable("screen.radiocraft.radio_interface.callsign"));
		callsignBox.setMaxLength(TncPacket.CALLSIGN_MAX);
		callsignBox.setValue(radioInterface().getCallsign());
		callsignBox.setTooltip(Tooltip.create(Component.translatable("screen.radiocraft.radio_interface.callsign_hint")));
		addRenderableWidget(callsignBox);
		transmitButton = Button.builder(transmitLabel(), btn -> {
					CompoundTag tag = new CompoundTag();
					tag.putBoolean("transmit", !radioInterface().isTransmitEnabled());
					SStationPacket.send(radioInterface().getBlockPos(), "transmit", tag);
				})
				.bounds(leftPos + 10, topPos + 54, 156, 18)
				.tooltip(Tooltip.create(Component.translatable("screen.radiocraft.radio_interface.tip.tx")))
				.build();
		addRenderableWidget(transmitButton);
	}

	private Component transmitLabel() {
		return Component.translatable(radioInterface().isTransmitEnabled() ? "screen.radiocraft.radio_interface.tx_on" : "screen.radiocraft.radio_interface.tx_off");
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		transmitButton.setMessage(transmitLabel()); // Follows the block once the server has switched it.
	}

	private void saveCallsign() {
		CompoundTag tag = new CompoundTag();
		tag.putString("callsign", callsignBox.getValue());
		SStationPacket.send(radioInterface().getBlockPos(), "callsign", tag);
		callsignBox.setFocused(false);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(callsignBox.isFocused()) {
			if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				saveCallsign();
				return true;
			}
			if(keyCode != GLFW.GLFW_KEY_ESCAPE) // Typing 'e' must not close the screen.
				return callsignBox.keyPressed(keyCode, scanCode, modifiers) || callsignBox.canConsumeInput();
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
		guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(font, title, 10, 8, LABEL, false);
		guiGraphics.drawString(font, Component.translatable("screen.radiocraft.radio_interface.callsign"), 10, 20, DIM, false);
		if(!radioInterface().getCallsignSetBy().isEmpty())
			guiGraphics.drawString(font, Component.translatable("screen.radiocraft.radio_interface.set_by", radioInterface().getCallsignSetBy()), 106, 34, DIM, false);
	}
}

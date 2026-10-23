package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.common.blockentities.TransferInterfaceBlockEntity;
import com.arrl.radiocraft.common.menus.TransferInterfaceMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The digital transfer interface's screen (to be redesigned; for now the old digital interface's FILES tab): the file
 * to send and who to, SEND, the progress LEDs, the log on the paper, the copies received and the blank supplies.
 */
public class TransferInterfaceScreen extends StationPanelScreen<TransferInterfaceMenu> {

	private static final ResourceLocation TEXTURE_FILES = Radiocraft.id("textures/gui/digital_interface_files.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/digital_interface_widgets.png");
	/** The player inventory half of the vanilla chest screen, a stand-in until the interface has its own. */
	private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

	private static final int INK = 0xFF2B2118; // On the paper.
	private static final int FIELD_TEXT = 0xFF1E2428; // In the light entry fields.
	private static final int[] FILES_TO = {40, 60, 57, 7};
	private static final int[] FILES_PAPER = {140, 47, 53, 27};
	private static final int[] FILES_LED_X = {26, 41, 56, 71, 86, 101};
	private static final int FILES_LED_Y = 107;
	private static final int PANEL_HEIGHT = 133;

	private EditBox toBox;
	private String draftFileTo = "";

	public TransferInterfaceScreen(TransferInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = PANEL_HEIGHT + 96; // Room for the player's inventory under the panel.
	}

	private TransferInterfaceBlockEntity transfer() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new HoldButton(leftPos + 78, topPos + 40, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, btn -> sendFile(), btn -> {}));
		toBox = new EditBox(font, leftPos + FILES_TO[0] + 1, topPos + FILES_TO[1], FILES_TO[2] - 2, FILES_TO[3] + 1, Component.empty());
		toBox.setBordered(false);
		toBox.setTextColor(FIELD_TEXT);
		toBox.setMaxLength(TncPacket.CALLSIGN_MAX);
		toBox.setValue(draftFileTo);
		toBox.setResponder(value -> draftFileTo = value);
		addRenderableWidget(toBox);
	}

	/** SEND: the outbox goes to the callsign in TO, or to everyone if it is empty. */
	private void sendFile() {
		CompoundTag tag = new CompoundTag();
		tag.putString("to", draftFileTo);
		SStationPacket.send(transfer().getBlockPos(), "file", tag);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode != GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof EditBox box && box.isFocused()) // Typing 'e' must not close the screen.
			return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE_FILES, leftPos, topPos, 0, 0, imageWidth, PANEL_HEIGHT, 217, 133);
		guiGraphics.blit(INVENTORY_TEXTURE, leftPos + TransferInterfaceMenu.INVENTORY_X - 8, topPos + PANEL_HEIGHT, 0, 126, 176, 96);
	}

	/** Progress LEDs (all dark when idle) and, on the paper, the transfer going on and the log of the last ones. */
	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		TransferInterfaceBlockEntity transfer = transfer();
		float progress = transfer.getTransferProgress();
		int lit = progress < 0.0F ? 0 : Math.max(1, (int)Math.ceil(progress * FILES_LED_X.length));
		for(int i = lit; i < FILES_LED_X.length; i++)
			guiGraphics.fill(FILES_LED_X[i], FILES_LED_Y, FILES_LED_X[i] + 8, FILES_LED_Y + 10, 0xC0101010);

		List<String> lines = new ArrayList<>();
		if(progress >= 0.0F)
			lines.add(transfer.getTransferLabel() + " " + (int)(progress * 100) + "%");
		lines.addAll(transfer.getFileLog());
		float scale = 0.4F;
		int rows = (int)(FILES_PAPER[3] / (font.lineHeight * scale));
		for(int i = 0; i < rows && i < lines.size(); i++) {
			String line = font.plainSubstrByWidth(lines.get(i), (int)(FILES_PAPER[2] / scale));
			text(guiGraphics, line, FILES_PAPER[0], FILES_PAPER[1] + i * font.lineHeight * scale, scale, INK, false);
		}
	}
}

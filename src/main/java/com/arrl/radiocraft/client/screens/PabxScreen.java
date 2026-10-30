package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.PabxBlockEntity;
import com.arrl.radiocraft.common.menus.PabxMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * The phone PABX's screen: the telephone's keypad (tools/pabx_gui.py). MODE steps through the master number, the first
 * extension and BEHIND (another PABX); key a number and press SET (or #) to save it, or SET to flip BEHIND. BACK
 * deletes a digit, * clears them. The number keys on the keyboard work too (ENTER sets, BACKSPACE deletes).
 */
public class PabxScreen extends StationPanelScreen<PabxMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/pabx.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/pabx_widgets.png");
	private static final int TEXTURE_WIDTH = 180, TEXTURE_HEIGHT = 240;
	private static final int[] LCD = {16, 50, 120, 30};
	private static final int[] MODE = {142, 54, 26, 22};
	private static final int[] SET = {24, 206, 62, 22};
	private static final int[] BACK = {94, 206, 62, 22};
	private static final String KEY_LABELS = "123456789*0#";
	private static final int LCD_INK = 0xFF1E2814, LABEL_INK = 0xFF2A2418;

	private enum Field { MASTER, FIRST, BEHIND }

	private Field field = Field.MASTER;
	private String number = "";
	private int pressedKey = -1;
	private long pressedUntil = 0L;

	public PabxScreen(PabxMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_WIDTH;
		this.imageHeight = TEXTURE_HEIGHT;
	}

	private PabxBlockEntity pabx() {
		return menu.blockEntity;
	}

	private static int[] key(int index) {
		return new int[] {24 + (index % 3) * 46, 96 + (index / 3) * 27, 36, 22};
	}

	private void send(String action, CompoundTag data) {
		SStationPacket.send(pabx().getBlockPos(), action, data);
	}

	private void tone(float pitch) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, 0.2F));
	}

	private void press(int index) {
		pressedKey = index;
		pressedUntil = System.currentTimeMillis() + 150L;
		char c = KEY_LABELS.charAt(index);
		tone(1.0F + index * 0.05F);
		if(c == '*')
			number = "";
		else if(c == '#')
			set();
		else if(field != Field.BEHIND && number.length() < (field == Field.MASTER ? PabxBlockEntity.MAX_MASTER_DIGITS : 5))
			number += c;
	}

	/** SET: saves the number keyed for this field (an empty master number clears it), or flips BEHIND. */
	private void set() {
		CompoundTag data = new CompoundTag();
		switch(field) {
			case MASTER -> {
				data.putString("number", number);
				send("master", data);
			}
			case FIRST -> {
				if(number.isEmpty())
					return;
				data.putInt("number", Integer.parseInt(number));
				send("first", data);
			}
			case BEHIND -> {
				data.putBoolean("behind", !pabx().isBehind());
				send("behind", data);
			}
		}
		number = "";
	}

	private void backspace() {
		number = number.isEmpty() ? number : number.substring(0, number.length() - 1);
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		for(int i = 0; i < 12; i++)
			if(inside(key(i), mouseX, mouseY)) {
				press(i);
				return true;
			}
		if(inside(SET, mouseX, mouseY)) {
			tone(1.5F);
			set();
			return true;
		}
		if(inside(BACK, mouseX, mouseY)) {
			tone(0.6F);
			backspace();
			return true;
		}
		if(inside(MODE, mouseX, mouseY)) {
			tone(1.2F);
			field = Field.values()[(field.ordinal() + 1) % Field.values().length];
			number = "";
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
			int digit = keyCode - GLFW.GLFW_KEY_0;
			press(digit == 0 ? 10 : digit - 1);
			return true;
		}
		if(keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
			int digit = keyCode - GLFW.GLFW_KEY_KP_0;
			press(digit == 0 ? 10 : digit - 1);
			return true;
		}
		if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			set();
			return true;
		}
		if(keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			backspace();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		long now = System.currentTimeMillis();
		for(int i = 0; i < 12; i++) {
			int[] k = key(i);
			if((i == pressedKey && now < pressedUntil) || inside(k, mouseX, mouseY))
				guiGraphics.blit(WIDGETS, leftPos + k[0], topPos + k[1], k[0], k[1], k[2], k[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}
		for(int[] b : new int[][] { SET, BACK, MODE })
			if(inside(b, mouseX, mouseY))
				guiGraphics.blit(WIDGETS, leftPos + b[0], topPos + b[1], b[0], b[1], b[2], b[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		label(guiGraphics, "MODE", MODE);
		label(guiGraphics, "SET", SET);
		label(guiGraphics, "BACK", BACK);
		text(guiGraphics, "PHONE PABX", leftPos + TEXTURE_WIDTH / 2.0F, topPos + 21, 1.0F, LABEL_INK, true);
		renderLcd(guiGraphics, now);
	}

	private void label(GuiGraphics guiGraphics, String label, int[] r) {
		text(guiGraphics, label, leftPos + r[0] + r[2] / 2.0F, topPos + r[1] + (r[3] - 6) / 2.0F, 0.75F, LABEL_INK, true);
	}

	/** The LCD: its number (as dialed across the microwave link) and power on top, the field being set below. */
	private void renderLcd(GuiGraphics guiGraphics, long now) {
		PabxBlockEntity pabx = pabx();
		int x = leftPos + LCD[0] + 4, y = topPos + LCD[1] + 4;
		String prefix = pabx.getPrefix();
		text(guiGraphics, "PABX " + (prefix.isEmpty() ? "----" : prefix), x, y, 1.0F, LCD_INK, false);
		String power = pabx.isPowered() ? "ON" : "NO POWER";
		text(guiGraphics, power, x + LCD[2] - 8 - font.width(power) * 0.75F, y + 1, 0.75F, LCD_INK, false);
		String typing = number + ((now / 400L) % 2L == 0L ? "_" : "");
		String line = switch(field) {
			case MASTER -> "MASTER NO: " + (number.isEmpty() ? (pabx.getMaster().isEmpty() ? "NONE" : pabx.getMaster()) : typing);
			case FIRST -> "FIRST EXT: " + (number.isEmpty() ? Integer.toString(pabx.getFirstExtension()) : typing);
			case BEHIND -> "BEHIND A PABX: " + (pabx.isBehind() ? "YES" : "NO");
		};
		text(guiGraphics, line, x, y + 13, 0.75F, LCD_INK, false);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// The LCD is drawn in renderBg.
	}
}

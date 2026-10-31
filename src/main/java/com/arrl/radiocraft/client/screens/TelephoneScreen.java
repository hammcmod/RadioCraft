package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.menus.TelephoneMenu;
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
 * The telephone's screen: the desk phone from above. Key in a number and press CALL; CALL also answers a ringing
 * phone, HANG hangs up. EXT, then the new number and CALL, changes this phone's extension. * clears the number, #
 * dials it. The number keys on the keyboard work too (ENTER dials, BACKSPACE deletes).
 */
public class TelephoneScreen extends StationPanelScreen<TelephoneMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/telephone.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/telephone_widgets.png");
	private static final int TEXTURE_WIDTH = 180, TEXTURE_HEIGHT = 240;
	private static final int[] LCD = {16, 50, 120, 30};
	private static final int[] EXT = {142, 54, 26, 22};
	private static final int[] CALL = {24, 206, 62, 22};
	private static final int[] HANG = {94, 206, 62, 22};
	private static final String KEY_LABELS = "123456789*0#";
	private static final int LCD_INK = 0xFF1E2814;

	private String number = "";
	private boolean settingExtension = false;
	private int pressedKey = -1;
	private long pressedUntil = 0L;

	public TelephoneScreen(TelephoneMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_WIDTH;
		this.imageHeight = TEXTURE_HEIGHT;
	}

	private TelephoneBlockEntity phone() {
		return menu.blockEntity;
	}

	private static int[] key(int index) {
		return new int[] {24 + (index % 3) * 46, 96 + (index / 3) * 27, 36, 22};
	}

	private void send(String action, CompoundTag data) {
		SStationPacket.send(phone().getBlockPos(), action, data);
	}

	private void tone(float pitch) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, 0.2F));
	}

	private void press(int index) {
		pressedKey = index;
		pressedUntil = System.currentTimeMillis() + 150L;
		char c = KEY_LABELS.charAt(index);
		tone(1.0F + index * 0.05F); // A touch tone.
		if(c == '*')
			number = "";
		else if(c == '#')
			call();
		else if(number.length() < (settingExtension ? 5 : 12))
			number += c;
	}

	/** CALL: answers a ringing phone, sets the extension in EXT mode, or dials. */
	private void call() {
		TelephoneBlockEntity phone = phone();
		if(phone.getState() == TelephoneBlockEntity.State.RINGING) {
			send("answer", new CompoundTag());
			return;
		}
		if(number.isEmpty() || phone.getState() != TelephoneBlockEntity.State.IDLE)
			return;
		CompoundTag data = new CompoundTag();
		if(settingExtension)
			data.putInt("extension", Integer.parseInt(number));
		else
			data.putString("number", number);
		send(settingExtension ? "extension" : "dial", data);
		settingExtension = false;
		number = "";
	}

	private void hangUp() {
		number = "";
		settingExtension = false;
		send("hangup", new CompoundTag());
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
		if(inside(CALL, mouseX, mouseY)) {
			tone(1.5F);
			call();
			return true;
		}
		if(inside(HANG, mouseX, mouseY)) {
			tone(0.6F);
			hangUp();
			return true;
		}
		if(inside(EXT, mouseX, mouseY)) {
			tone(1.2F);
			settingExtension = !settingExtension;
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
			call();
			return true;
		}
		if(keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			number = number.isEmpty() ? number : number.substring(0, number.length() - 1);
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
		for(int[] b : new int[][] { CALL, HANG })
			if(inside(b, mouseX, mouseY))
				guiGraphics.blit(WIDGETS, leftPos + b[0], topPos + b[1], b[0], b[1], b[2], b[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		if(settingExtension || inside(EXT, mouseX, mouseY))
			guiGraphics.blit(WIDGETS, leftPos + EXT[0], topPos + EXT[1], EXT[0], EXT[1], EXT[2], EXT[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		renderLcd(guiGraphics, now);
	}

	/** The LCD: this phone's extension and state on top, the number being keyed (or the last news) below. */
	private void renderLcd(GuiGraphics guiGraphics, long now) {
		TelephoneBlockEntity phone = phone();
		int x = leftPos + LCD[0] + 4, y = topPos + LCD[1] + 4;
		String state = switch(phone.getState()) {
			case IDLE -> settingExtension ? "NEW EXT" : "READY";
			case CALLING -> "RINGING " + phone.getPartnerNumber();
			case RINGING -> (now / 400L) % 2L == 0L ? "CALL FROM " + phone.getPartnerNumber() : "";
			case IN_CALL -> String.format("TALK %s  %d:%02d", phone.getPartnerNumber(), phone.getStateTicks() / 1200, phone.getStateTicks() / 20 % 60);
		};
		text(guiGraphics, "EXT " + phone.getExtension(), x, y, 1.0F, LCD_INK, false);
		text(guiGraphics, state, x + LCD[2] - 8 - font.width(state) * 0.75F, y + 1, 0.75F, LCD_INK, false);
		String cursor = (now / 400L) % 2L == 0L ? "_" : "";
		// Keying exactly another PABX's master number (and no extension here): which PABX that is.
		String pabx = number.isEmpty() || settingExtension || phone.getState() != TelephoneBlockEntity.State.IDLE ? null : phone.directoryLabel(number);
		if(pabx != null) {
			text(guiGraphics, pabx + cursor, x, y + 13, 0.75F, LCD_INK, false);
			return;
		}
		String line = !number.isEmpty() ? number + cursor
				: phone.getState() == TelephoneBlockEntity.State.RINGING ? "CALL TO ANSWER"
				: phone.getStatus().isEmpty() ? (settingExtension ? "KEY NEW NUMBER" : numberLine(phone)) : phone.getStatus();
		text(guiGraphics, line, x, y + 12, number.isEmpty() ? 0.75F : 1.0F, LCD_INK, false);
	}

	/** This phone's number from another PABX (master number and extension), when that differs from its extension. */
	private static String numberLine(TelephoneBlockEntity phone) {
		String full = phone.getFullNumber();
		return full.isEmpty() || full.equals(Integer.toString(phone.getExtension())) ? "" : "NO. " + full;
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// The LCD is drawn in renderBg.
	}
}

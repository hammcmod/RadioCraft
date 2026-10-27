package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.arrl.radiocraft.common.menus.RadioFaxMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.FaxRates;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The radio fax's screen: the document slot and SEND key on the left, the supplies and the received tray along the
 * bottom, and in the top right corner the machine's little grey LCD, where the callsign to fax to is typed (or picked
 * from the ones used lately), the machine's own callsign is set, and the fax going on shows its packet count and a
 * progress bar. The keyboard belongs to the LCD while the screen is open: ENTER sends, TAB moves between TO and MY.
 */
public class RadioFaxScreen extends StationPanelScreen<RadioFaxMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/radio_fax.png");
	/** The player inventory half of the vanilla chest screen, a stand-in until the fax has its own. */
	private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int PANEL_WIDTH = 217, PANEL_HEIGHT = 110;

	// The LCD: a grid of cells over the face inside the bezel (see tools/radio_fax_models.py).
	private static final int LCD_X = 124, LCD_Y = 12, LCD_W = 84, LCD_H = 49;
	private static final int COLS = 21, ROWS = 8;
	private static final int CELL_W = 4, CELL_H = 6;
	private static final float TEXT_SCALE = 0.6F;
	private static final int INK = 0xFF262A26;
	private static final int FAINT = 0xFF6C726A;
	private static final int FACE = 0xFFB0B8A8;
	private static final int BAR = 0xFF3A403A;
	private static final int[] SEND_KEY = {18, 52, 40, 14};
	private static final int LABEL = 0xFF5A4A36;

	private static final int ROW_MY = 0, ROW_TO = 1, ROW_RECENT = 2, ROW_RECENT_FIRST = 3, ROW_STATUS = 6, ROW_BAR = 7;

	private enum Field { TO, MY }

	private Field field = Field.TO;
	private String draftTo = "";
	private String draftMy = "";
	private boolean myLoaded = false;
	private long lastPacketsDone = -1L;

	public RadioFaxScreen(RadioFaxMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = PANEL_WIDTH;
		this.imageHeight = PANEL_HEIGHT + 96; // Room for the player's inventory under the panel.
	}

	private RadioFaxBlockEntity fax() {
		return menu.blockEntity;
	}

	// -------------------- INPUT --------------------

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			return super.keyPressed(keyCode, scanCode, modifiers);
		switch(keyCode) {
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if(field == Field.TO)
					send();
				else
					setCallsign();
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if(field == Field.TO && !draftTo.isEmpty())
					draftTo = draftTo.substring(0, draftTo.length() - 1);
				else if(field == Field.MY && !draftMy.isEmpty())
					draftMy = draftMy.substring(0, draftMy.length() - 1);
			}
			case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> field = field == Field.TO ? Field.MY : Field.TO;
			default -> {}
		}
		return true; // The keyboard is the LCD's: no closing on 'e', no hotbar keys.
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		String c = TncPacket.sanitizeCallsign(String.valueOf(codePoint));
		if(c.isEmpty())
			return true;
		if(field == Field.TO)
			draftTo = TncPacket.sanitizeCallsign(draftTo + c);
		else
			draftMy = TncPacket.sanitizeCallsign(draftMy + c);
		click(1.4F);
		return true;
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(inside(SEND_KEY, mouseX, mouseY)) {
			click(0.9F);
			send();
			return true;
		}
		int col = (int)Math.floor((mouseX - leftPos - LCD_X) / CELL_W), row = (int)Math.floor((mouseY - topPos - LCD_Y) / CELL_H);
		if(col < 0 || col >= COLS || row < 0 || row >= ROWS)
			return false;
		if(row == ROW_MY)
			field = Field.MY;
		else if(row == ROW_TO)
			field = Field.TO;
		else if(row >= ROW_RECENT_FIRST && row < ROW_STATUS) {
			List<String> recent = fax().getRecent();
			int index = (row - ROW_RECENT_FIRST) * 2 + (col >= COLS / 2 ? 1 : 0);
			if(index < recent.size()) {
				draftTo = recent.get(index);
				field = Field.TO;
				click(1.2F);
			}
		}
		return true;
	}

	private void send() {
		CompoundTag tag = new CompoundTag();
		tag.putString("to", draftTo);
		SStationPacket.send(fax().getBlockPos(), "send", tag);
	}

	private void setCallsign() {
		CompoundTag tag = new CompoundTag();
		tag.putString("callsign", draftMy);
		SStationPacket.send(fax().getBlockPos(), "callsign", tag);
	}

	private void click(float pitch) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, pitch, 0.3F));
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
		guiGraphics.blit(INVENTORY_TEXTURE, leftPos + RadioFaxMenu.INVENTORY_X - 8, topPos + PANEL_HEIGHT - 1, 0, 126, 176, 96);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		RadioFaxBlockEntity fax = fax();
		if(!myLoaded) {
			draftMy = fax.getCallsign();
			myLoaded = true;
		}
		// Labels on the fascia.
		text(guiGraphics, "RADIO FAX", 8, 8, 0.7F, LABEL, false);
		text(guiGraphics, "DOCUMENT", RadioFaxMenu.OUTBOX_AT[0] - 2, RadioFaxMenu.OUTBOX_AT[1] - 8, 0.5F, LABEL, false);
		text(guiGraphics, "SEND", SEND_KEY[0] + SEND_KEY[2] / 2.0F, SEND_KEY[1] + 4, 0.7F, LABEL, true);
		text(guiGraphics, "PAPER", RadioFaxMenu.SUPPLY_X[0] + 8, RadioFaxMenu.SUPPLY_Y - 7, 0.5F, LABEL, true);
		text(guiGraphics, "BOOK", RadioFaxMenu.SUPPLY_X[1] + 8, RadioFaxMenu.SUPPLY_Y - 7, 0.5F, LABEL, true);
		text(guiGraphics, "MAP", RadioFaxMenu.SUPPLY_X[2] + 8, RadioFaxMenu.SUPPLY_Y - 7, 0.5F, LABEL, true);
		text(guiGraphics, "RECEIVED", RadioFaxMenu.TRAY_X[0], RadioFaxMenu.TRAY_Y - 7, 0.5F, LABEL, false);
		renderLcd(guiGraphics, fax);
	}

	private void renderLcd(GuiGraphics guiGraphics, RadioFaxBlockEntity fax) {
		long now = System.currentTimeMillis();
		boolean blink = (now / 500L) % 2L == 0L;
		String my = draftMy.isEmpty() && field != Field.MY ? (fax.myCallsign().isEmpty() ? "----" : fax.myCallsign()) : draftMy;
		put(guiGraphics, ROW_MY, 0, "MY ", FAINT);
		put(guiGraphics, ROW_MY, 3, my + (field == Field.MY && blink ? "_" : ""), INK);
		put(guiGraphics, ROW_TO, 0, "TO ", FAINT);
		put(guiGraphics, ROW_TO, 3, draftTo + (field == Field.TO && blink ? "_" : ""), INK);
		List<String> recent = fax.getRecent();
		put(guiGraphics, ROW_RECENT, 0, recent.isEmpty() ? "NO RECENT CALLS" : "RECENT", FAINT);
		for(int i = 0; i < recent.size() && i < RadioFaxBlockEntity.RECENT_MAX; i++)
			put(guiGraphics, ROW_RECENT_FIRST + i / 2, (i % 2) * (COLS / 2) + 1, recent.get(i), INK);

		String status = fax.getStatus();
		boolean busy = fax.isBusy();
		if(busy && fax.getPacketsDone() != lastPacketsDone) {
			lastPacketsDone = fax.getPacketsDone();
			if(minecraft != null && lastPacketsDone > 0)
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8F, 0.15F)); // The counter ticks over.
		}
		List<String> lines = wrap(status, COLS);
		put(guiGraphics, ROW_STATUS, 0, lines.get(0), INK);
		if(busy) {
			float progress = fax.getPacketsDone() / (float)FaxRates.PACKETS;
			int x = leftPos + LCD_X + 1, y = topPos + LCD_Y + ROW_BAR * CELL_H + 1;
			int w = LCD_W - 2, h = CELL_H - 2;
			guiGraphics.fill(x, y, x + w, y + h, BAR);
			guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
			guiGraphics.fill(x + 1, y + 1, x + 1 + (int)((w - 2) * progress), y + h - 1, BAR);
			if(fax.getRetries() > 0)
				put(guiGraphics, ROW_STATUS, COLS - 3, String.format("R%d", Math.min(99, fax.getRetries())), FAINT);
		}
		else if(lines.size() > 1)
			put(guiGraphics, ROW_BAR, 0, lines.get(1), INK);
	}

	/** {@code text} one character to a cell on the LCD. */
	private void put(GuiGraphics guiGraphics, int row, int col, String text, int colour) {
		Matrix4f pose = new Matrix4f(guiGraphics.pose().last().pose()).translate(leftPos + LCD_X, topPos + LCD_Y + row * CELL_H + 1, 0.0F).scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		for(int i = 0; i < text.length() && col + i < COLS; i++) {
			String c = String.valueOf(text.charAt(i));
			if(c.equals(" "))
				continue;
			float cellWidth = CELL_W / TEXT_SCALE;
			font.drawInBatch(c, (col + i) * cellWidth + (cellWidth - font.width(c)) / 2.0F, 0.0F, colour, false, pose, guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		}
		guiGraphics.flush();
	}

	private static List<String> wrap(String text, int width) {
		List<String> out = new ArrayList<>();
		String rest = text;
		while(rest.length() > width) {
			int cut = rest.lastIndexOf(' ', width);
			if(cut <= 0)
				cut = width;
			out.add(rest.substring(0, cut).strip());
			rest = rest.substring(cut).strip();
		}
		out.add(rest);
		return out;
	}

	@Override
	protected void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> tip = new ArrayList<>();
		int lcdCol = (int)Math.floor((mouseX - leftPos - LCD_X) / CELL_W), lcdRow = (int)Math.floor((mouseY - topPos - LCD_Y) / CELL_H);
		if(inside(SEND_KEY, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.send"));
		else if(lcdCol >= 0 && lcdCol < COLS && lcdRow == ROW_TO)
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.to"));
		else if(lcdCol >= 0 && lcdCol < COLS && lcdRow == ROW_MY)
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.my"));
		else if(inside(new int[] {RadioFaxMenu.OUTBOX_AT[0], RadioFaxMenu.OUTBOX_AT[1], 16, 16}, mouseX, mouseY) && menu.getSlot(0).getItem().isEmpty())
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.document"));
		else if(inside(new int[] {RadioFaxMenu.TRAY_X[0] - 2, RadioFaxMenu.TRAY_Y - 8, RadioFaxMenu.TRAY_X[3] - RadioFaxMenu.TRAY_X[0] + 20, 8}, mouseX, mouseY)) {
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.tray"));
			for(String line : fax().getLog())
				tip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
		}
		else if(inside(new int[] {RadioFaxMenu.SUPPLY_X[0] - 2, RadioFaxMenu.SUPPLY_Y - 8, RadioFaxMenu.SUPPLY_X[2] - RadioFaxMenu.SUPPLY_X[0] + 20, 8}, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.radio_fax.tip.supplies"));
		if(!tip.isEmpty())
			guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
	}
}

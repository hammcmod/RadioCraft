package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity.Heard;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity.Logged;
import com.arrl.radiocraft.common.menus.AprsTerminalMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The APRS terminal's screen (to be redesigned; for now the old digital interface's APRS and MSG tabs). APRS: beacon
 * settings, beacons heard (list on the paper, plotted on the screen around this station, north up), beaconing and the
 * digipeat switch of the radio interface; MSG: messages to and from this station and a box to write one. The lamps top
 * right beacon now (blue), show a radio interface joined (green) and unread messages (red).
 */
public class AprsTerminalScreen extends StationPanelScreen<AprsTerminalMenu> {

	private static final ResourceLocation TEXTURE_APRS = Radiocraft.id("textures/gui/digital_interface_arps.png");
	private static final ResourceLocation TEXTURE_MSG = Radiocraft.id("textures/gui/digital_interface_msg.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/digital_interface_widgets.png");

	private static final int TAB_APRS = 1;
	private static final int TAB_MSG = 2;

	private static final int INK = 0xFF2B2118; // On the paper.
	private static final int FIELD_TEXT = 0xFF1E2428; // In the light entry fields.
	private static final int LCD = 0xFFBFE8C4; // On the grey screens.
	private static final int LCD_DIM = 0xFF7E9A86;
	private static final int CHECK = 0xFF3CC83C;
	private static final int LAMP_RED = 0xC0FF3030;
	private static final int LAMP_GREEN = 0xA040FF40;

	// Regions of the textures, relative to the screen's corner: {x, y, width, height}.
	private static final int[] BLUE_LAMP = {161, 17, 11, 11};
	private static final int[] GREEN_LAMP = {174, 17, 12, 11};
	private static final int[] RED_LAMP = {189, 17, 12, 11};
	private static final int[] APRS_CONTACT_BUZZER = {88, 34, 5, 5};
	private static final int[] APRS_BEACON_ENABLE = {67, 41, 5, 5};
	private static final int[] APRS_CALLSIGN = {50, 47, 29, 6};
	private static final int[] APRS_COMMENT = {34, 54, 57, 7};
	private static final int[] APRS_ICON = {34, 62, 21, 6};
	private static final int[] APRS_PAPER = {21, 83, 52, 27};
	private static final int[] APRS_MAP = {103, 44, 94, 71};
	private static final int[] APRS_RPT_LAMP = {87, 69, 6, 8};
	private static final int[] APRS_DIGIPEAT = {84, 112, 11, 10};
	private static final int[] MSG_BUZZER = {89, 36, 5, 5};
	private static final int[] MSG_BEACON_ENABLE = {78, 45, 5, 5};
	private static final int[] MSG_SEND = {29, 53, 51, 9};
	private static final int[] MSG_COMPOSE = {22, 76, 64, 39};
	private static final int[] MSG_PAPER = {107, 44, 89, 66};
	private static final int PANEL_HEIGHT = 133;
	/** Tops of the bands between the ruled lines of each paper (each rule is the row above a band); a line of text per band. */
	private static final int[] APRS_BANDS = {83, 93, 102};
	private static final int[] MSG_BANDS = {46, 55, 64, 73, 83, 92, 102};
	private static final int BAND_HEIGHT = 8;
	private static final float PAPER_SCALE = 0.75F;
	private static final float APRS_PAPER_SCALE = 0.6F;
	/** Text size on the MSG tab's little command-line screen. */
	private static final float CLI_SCALE = 0.5F;
	private static final int CLI_LINE = 5;

	private static final float SMALL = 0.5F;

	private int currentTab = TAB_APRS;
	private boolean editingComment = false;
	private EditBox fieldBox;
	private enum Compose { NONE, TO, TEXT }
	private Compose composeFocus = Compose.NONE;
	private String draftTo = "";
	private String draftMessage = "";
	private int beaconScroll = 0;
	private int messageScroll = 0;
	private AprsMapTexture mapTexture = null; // Made when the APRS map is first drawn, freed when the screen closes.

	public AprsTerminalScreen(AprsTerminalMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = PANEL_HEIGHT;
		if(menu.blockEntity.hasUnread())
			currentTab = TAB_MSG; // Straight to the waiting message.
	}

	private AprsTerminalBlockEntity terminal() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		fieldBox = null;

		addRenderableWidget(new ToggleButton(currentTab == TAB_APRS, leftPos + 16, topPos + 17, 35, 14, 0, 0, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_APRS)));
		addRenderableWidget(new ToggleButton(currentTab == TAB_MSG, leftPos + 50, topPos + 17, 36, 14, 144, 0, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_MSG)));

		if(currentTab == TAB_APRS && editingComment) {
			fieldBox = new EditBox(font, leftPos + APRS_MAP[0] + 2, topPos + APRS_MAP[1] + 30, APRS_MAP[2] - 4, 12, Component.empty());
			fieldBox.setMaxLength(TncPacket.BEACON_TEXT_MAX);
			fieldBox.setValue(terminal().getBeaconComment());
			addRenderableWidget(fieldBox);
			setInitialFocus(fieldBox);
		}
	}

	private void selectTab(int tab) {
		currentTab = tab;
		editingComment = false;
		rebuildWidgets();
	}

	// -------------------- INPUT --------------------

	private void settings(String key, boolean value) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(key, value);
		SStationPacket.send(terminal().getBlockPos(), "settings", tag);
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		if(inside(BLUE_LAMP, mouseX, mouseY)) {
			SStationPacket.send(terminal().getBlockPos(), "beacon");
			return true;
		}
		if(currentTab == TAB_APRS && !editingComment) {
			if(inside(APRS_CONTACT_BUZZER, mouseX, mouseY)) {
				settings("contactBuzzer", !terminal().isContactBuzzer());
				return true;
			}
			if(inside(APRS_BEACON_ENABLE, mouseX, mouseY)) {
				settings("beaconEnabled", !terminal().isBeaconEnabled());
				return true;
			}
			if(inside(APRS_DIGIPEAT, mouseX, mouseY)) {
				settings("digipeat", !terminal().isDigipeatEnabled());
				return true;
			}
			if(inside(APRS_COMMENT, mouseX, mouseY)) {
				editingComment = true;
				rebuildWidgets();
				return true;
			}
			if(inside(APRS_ICON, mouseX, mouseY)) {
				CompoundTag tag = new CompoundTag();
				tag.putString("icon", terminal().getIcon().next().name());
				SStationPacket.send(terminal().getBlockPos(), "settings", tag);
				return true;
			}
		}
		if(currentTab == TAB_MSG) {
			if(inside(MSG_BUZZER, mouseX, mouseY)) {
				settings("messageBuzzer", !terminal().isMessageBuzzer());
				return true;
			}
			if(inside(MSG_BEACON_ENABLE, mouseX, mouseY)) {
				settings("beaconEnabled", !terminal().isBeaconEnabled());
				return true;
			}
			if(inside(MSG_SEND, mouseX, mouseY)) {
				sendMessage();
				return true;
			}
			if(inside(MSG_COMPOSE, mouseX, mouseY)) { // The top line is TO, the rest the message.
				composeFocus = mouseY - topPos < MSG_COMPOSE[1] + 2 + CLI_LINE ? Compose.TO : Compose.TEXT;
				return true;
			}
			composeFocus = Compose.NONE;
		}
		return false;
	}

	private void sendMessage() {
		String to = TncPacket.sanitizeCallsign(draftTo);
		String text = TncPacket.sanitizeText(draftMessage.replace('\n', ' '), TncPacket.MESSAGE_TEXT_MAX);
		if(to.isEmpty() || text.isEmpty())
			return;
		CompoundTag tag = new CompoundTag();
		tag.putString("to", to);
		tag.putString("text", text);
		SStationPacket.send(terminal().getBlockPos(), "message", tag);
		draftMessage = "";
		composeFocus = Compose.TEXT;
	}

	private void finishEditing(boolean apply) {
		if(apply && fieldBox != null) {
			CompoundTag tag = new CompoundTag();
			tag.putString("comment", fieldBox.getValue());
			SStationPacket.send(terminal().getBlockPos(), "settings", tag);
		}
		editingComment = false;
		rebuildWidgets();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(editingComment) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
				finishEditing(false); // Escape leaves the field, not the screen.
				return true;
			}
			if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				finishEditing(true);
				return true;
			}
		}
		if(keyCode != GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof EditBox box && box.isFocused()) // Typing 'e' must not close the screen.
			return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
		if(currentTab == TAB_MSG && composeFocus != Compose.NONE) {
			switch(keyCode) {
				case GLFW.GLFW_KEY_ESCAPE -> composeFocus = Compose.NONE; // Leaves the prompt; a second Escape closes.
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if(composeFocus == Compose.TO && !draftTo.isEmpty())
						draftTo = draftTo.substring(0, draftTo.length() - 1);
					else if(composeFocus == Compose.TEXT && !draftMessage.isEmpty())
						draftMessage = draftMessage.substring(0, draftMessage.length() - 1);
				}
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					if(composeFocus == Compose.TO)
						composeFocus = Compose.TEXT;
					else
						sendMessage();
				}
				case GLFW.GLFW_KEY_TAB -> composeFocus = composeFocus == Compose.TO ? Compose.TEXT : Compose.TO;
				default -> {} // Typing comes through charTyped; nothing else (no closing the screen on E).
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(currentTab == TAB_MSG && composeFocus == Compose.TO) {
			draftTo = TncPacket.sanitizeCallsign(draftTo + codePoint);
			return true;
		}
		if(currentTab == TAB_MSG && composeFocus == Compose.TEXT) {
			if(codePoint >= 32 && codePoint < 127 && draftMessage.length() < TncPacket.MESSAGE_TEXT_MAX)
				draftMessage += codePoint;
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		int step = scrollY > 0 ? -1 : 1;
		if(currentTab == TAB_APRS && inside(APRS_PAPER, mouseX, mouseY)) {
			beaconScroll = Mth.clamp(beaconScroll + step, 0, Math.max(0, terminal().getBeacons().size() - APRS_BANDS.length));
			return true;
		}
		if(currentTab == TAB_MSG && inside(MSG_PAPER, mouseX, mouseY)) {
			messageScroll = Math.max(0, messageScroll + step); // Clamped when drawn, where the wrapped length is known.
			return true;
		}
		return false;
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(currentTab == TAB_MSG ? TEXTURE_MSG : TEXTURE_APRS, leftPos, topPos, 0, 0, imageWidth, PANEL_HEIGHT, 217, 133);
		if(terminal().bus().hasInterface())
			fillAt(guiGraphics, GREEN_LAMP, leftPos, topPos, LAMP_GREEN);
		if(terminal().hasUnread() && (System.currentTimeMillis() / 500) % 2 == 0)
			fillAt(guiGraphics, RED_LAMP, leftPos, topPos, LAMP_RED);
		if(currentTab == TAB_APRS && editingComment)
			fillAt(guiGraphics, APRS_MAP, leftPos, topPos, 0xFF3A4448); // Here, under the edit box: labels draw over widgets.
		else if(currentTab == TAB_APRS)
			renderTerrain(guiGraphics);
	}

	/** The terrain under the map, at the map's scale and centred on this station. */
	private void renderTerrain(GuiGraphics guiGraphics) {
		if(minecraft == null || minecraft.level == null)
			return;
		if(mapTexture == null)
			mapTexture = new AprsMapTexture(APRS_MAP[2], APRS_MAP[3]);
		float radius = Math.min(APRS_MAP[2], APRS_MAP[3]) / 2.0F - 6.0F;
		double blocksPerPixel = mapRange(terminal().getBeacons()) / (double)radius;
		mapTexture.update(minecraft.level, terminal().getBlockPos(), APRS_MAP[2] / 2.0D, APRS_MAP[3] / 2.0D, blocksPerPixel);
		guiGraphics.blit(AprsMapTexture.LOCATION, leftPos + APRS_MAP[0], topPos + APRS_MAP[1], 0, 0, APRS_MAP[2], APRS_MAP[3], APRS_MAP[2], APRS_MAP[3]);
	}

	@Override
	public void removed() {
		super.removed();
		if(mapTexture != null) {
			mapTexture.close();
			mapTexture = null;
		}
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if(currentTab == TAB_APRS)
			renderAprs(guiGraphics);
		else
			renderMessages(guiGraphics);
	}

	private void renderAprs(GuiGraphics guiGraphics) {
		AprsTerminalBlockEntity terminal = terminal();
		check(guiGraphics, APRS_CONTACT_BUZZER, terminal.isContactBuzzer());
		check(guiGraphics, APRS_BEACON_ENABLE, terminal.isBeaconEnabled());
		if(terminal.isRepeating())
			guiGraphics.fill(APRS_RPT_LAMP[0], APRS_RPT_LAMP[1], APRS_RPT_LAMP[0] + APRS_RPT_LAMP[2], APRS_RPT_LAMP[1] + APRS_RPT_LAMP[3], 0xFFFF3A2E);
		if(terminal.isDigipeatEnabled()) // The blue lamp lit: digipeating on.
			guiGraphics.fill(APRS_DIGIPEAT[0] + 2, APRS_DIGIPEAT[1] + 3, APRS_DIGIPEAT[0] + APRS_DIGIPEAT[2] - 2, APRS_DIGIPEAT[1] + APRS_DIGIPEAT[3] - 2, 0xFF4F7BFF);
		String callsign = terminal.getCallsign();
		fieldText(guiGraphics, APRS_CALLSIGN, callsign.isEmpty() ? "-" : callsign);
		fieldText(guiGraphics, APRS_COMMENT, terminal.getBeaconComment());
		fieldText(guiGraphics, APRS_ICON, terminal.getIcon().name());
		item(guiGraphics, terminal.getIcon(), APRS_ICON[0] + APRS_ICON[2] + 2, APRS_ICON[1] - 1);

		List<Heard> beacons = terminal.getBeacons();
		beaconScroll = Mth.clamp(beaconScroll, 0, Math.max(0, beacons.size() - APRS_BANDS.length));
		int width = (int)(APRS_PAPER[2] / APRS_PAPER_SCALE);
		if(beacons.isEmpty())
			onBand(guiGraphics, Component.translatable("screen.radiocraft.aprs.no_beacons").getVisualOrderText(), APRS_PAPER[0], APRS_BANDS[0], APRS_PAPER_SCALE);
		for(int i = 0; i < APRS_BANDS.length && beaconScroll + i < beacons.size(); i++) {
			TncPacket packet = beacons.get(beaconScroll + i).packet();
			String line = font.plainSubstrByWidth(packet.from() + " " + distanceAndBearing(packet.position()), width);
			onBand(guiGraphics, Component.literal(line).getVisualOrderText(), APRS_PAPER[0], APRS_BANDS[i], APRS_PAPER_SCALE);
		}

		if(editingComment) {
			text(guiGraphics, Component.translatable("screen.radiocraft.aprs.edit_comment").getString(), APRS_MAP[0] + APRS_MAP[2] / 2.0F, APRS_MAP[1] + 20, 0.6F, LCD, true);
			text(guiGraphics, Component.translatable("screen.radiocraft.aprs.edit_keys").getString(), APRS_MAP[0] + APRS_MAP[2] / 2.0F, APRS_MAP[1] + 50, SMALL, LCD_DIM, true);
			return;
		}
		renderMap(guiGraphics, beacons);
	}

	/** Heard stations around this one, north up, scaled so the furthest fits; the range is in the corner. */
	private void renderMap(GuiGraphics guiGraphics, List<Heard> beacons) {
		BlockPos here = terminal().getBlockPos();
		float cx = APRS_MAP[0] + APRS_MAP[2] / 2.0F;
		float cy = APRS_MAP[1] + APRS_MAP[3] / 2.0F;
		float radius = Math.min(APRS_MAP[2], APRS_MAP[3]) / 2.0F - 6.0F;
		int range = mapRange(beacons);
		guiGraphics.fill((int)cx, APRS_MAP[1] + 2, (int)cx + 1, APRS_MAP[1] + APRS_MAP[3] - 2, 0x30FFFFFF);
		guiGraphics.fill(APRS_MAP[0] + 2, (int)cy, APRS_MAP[0] + APRS_MAP[2] - 2, (int)cy + 1, 0x30FFFFFF);
		text(guiGraphics, "N", cx + 3, APRS_MAP[1] + 2, SMALL, LCD_DIM, false);
		text(guiGraphics, rangeLabel(range), APRS_MAP[0] + 2, APRS_MAP[1] + APRS_MAP[3] - 5, SMALL, LCD_DIM, false);
		guiGraphics.fill((int)cx - 1, (int)cy - 1, (int)cx + 2, (int)cy + 2, 0xFFFFE060);
		for(Heard heard : beacons) {
			BlockPos at = heard.packet().position();
			if(at == null)
				continue;
			float x = cx + (at.getX() - here.getX()) / (float)range * radius;
			float y = cy + (at.getZ() - here.getZ()) / (float)range * radius;
			item(guiGraphics, AprsIcon.byName(heard.packet().icon()), x - 4.0F, y - 4.0F);
			text(guiGraphics, heard.packet().from(), x, y + 4.0F, 0.4F, LCD, true);
		}
	}

	private void renderMessages(GuiGraphics guiGraphics) {
		AprsTerminalBlockEntity terminal = terminal();
		check(guiGraphics, MSG_BUZZER, terminal.isMessageBuzzer());
		check(guiGraphics, MSG_BEACON_ENABLE, terminal.isBeaconEnabled());
		renderCompose(guiGraphics);

		List<FormattedCharSequence> lines = new ArrayList<>();
		int wrap = (int)(MSG_PAPER[2] / PAPER_SCALE);
		for(Logged logged : terminal.getMessages()) {
			TncPacket packet = logged.packet();
			String line = logged.outgoing() ? "> " + packet.to() + ": " + packet.text() : packet.from() + ": " + packet.text();
			lines.addAll(font.split(Component.literal(line), wrap));
		}
		messageScroll = Mth.clamp(messageScroll, 0, Math.max(0, lines.size() - MSG_BANDS.length));
		if(lines.isEmpty())
			onBand(guiGraphics, Component.translatable("screen.radiocraft.aprs.no_messages").getVisualOrderText(), MSG_PAPER[0], MSG_BANDS[0], PAPER_SCALE);
		for(int i = 0; i < MSG_BANDS.length && messageScroll + i < lines.size(); i++)
			onBand(guiGraphics, lines.get(messageScroll + i), MSG_PAPER[0], MSG_BANDS[i], PAPER_SCALE);
	}

	/**
	 * The message being written, on the grey screen like a command line: "TO&gt; " and the callsign, then "APRS&gt; "
	 * and the message, wrapped, showing its end if it is longer than the screen. A cursor blinks where typing goes.
	 */
	private void renderCompose(GuiGraphics guiGraphics) {
		boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
		int wrap = (int)((MSG_COMPOSE[2] - 4) / CLI_SCALE);
		List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.literal("TO> ").withColor(LCD_DIM)
				.append(Component.literal(draftTo + (composeFocus == Compose.TO && blink ? "_" : ""))), wrap));
		List<FormattedCharSequence> message = font.split(Component.literal("APRS> ").withColor(LCD_DIM)
				.append(Component.literal(draftMessage + (composeFocus == Compose.TEXT && blink ? "_" : ""))), wrap);
		int room = (MSG_COMPOSE[3] - 4) / CLI_LINE - 1;
		lines.addAll(message.subList(Math.max(0, message.size() - room), message.size()));
		for(int i = 0; i < lines.size(); i++) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(MSG_COMPOSE[0] + 2, MSG_COMPOSE[1] + 2 + i * CLI_LINE, 0.0F);
			guiGraphics.pose().scale(CLI_SCALE, CLI_SCALE, 1.0F);
			guiGraphics.drawString(font, lines.get(i), 0, 0, LCD, false);
			guiGraphics.pose().popPose();
		}
	}

	/** A line of text on a paper band whose top row is {@code top}, centred in the band's height. */
	private void onBand(GuiGraphics guiGraphics, FormattedCharSequence line, float x, int top, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, top + (BAND_HEIGHT - 7.0F * scale) / 2.0F, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(font, line, 0, 0, INK, false);
		guiGraphics.pose().popPose();
	}

	/** Index of the band containing {@code y} (screen-relative); far negative between and outside them. */
	private static int bandAt(int[] bands, int y) {
		for(int i = 0; i < bands.length; i++)
			if(y >= bands[i] && y < bands[i] + BAND_HEIGHT)
				return i;
		return -100_000;
	}

	@Override
	protected void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> tip = new ArrayList<>();
		if(inside(BLUE_LAMP, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.aprs.tip.beacon"));
		else if(inside(GREEN_LAMP, mouseX, mouseY))
			tip.add(Component.translatable(terminal().bus().hasInterface() ? "screen.radiocraft.aprs.tip.callsign" : "screen.radiocraft.aprs.tip.no_interface"));
		else if(inside(RED_LAMP, mouseX, mouseY))
			tip.add(Component.translatable(terminal().hasUnread() ? "screen.radiocraft.aprs.tip.unread" : "screen.radiocraft.aprs.tip.no_unread"));
		else if(currentTab == TAB_APRS && !editingComment) {
			if(inside(APRS_CALLSIGN, mouseX, mouseY))
				tip.add(Component.translatable(terminal().bus().hasInterface() ? "screen.radiocraft.aprs.tip.callsign" : "screen.radiocraft.aprs.tip.no_interface"));
			else if(inside(APRS_COMMENT, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.aprs.tip.comment"));
			else if(inside(APRS_ICON, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.aprs.tip.icon"));
			else if(inside(APRS_BEACON_ENABLE, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.aprs.tip.beacon_enable"));
			else if(inside(APRS_DIGIPEAT, mouseX, mouseY))
				tip.add(Component.translatable(terminal().isDigipeatEnabled() ? "screen.radiocraft.aprs.tip.digipeat_on" : "screen.radiocraft.aprs.tip.digipeat_off"));
			else if(inside(APRS_RPT_LAMP, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.aprs.tip.rpt"));
			else if(inside(APRS_PAPER, mouseX, mouseY)) {
				int row = bandAt(APRS_BANDS, mouseY - topPos) + beaconScroll;
				List<Heard> beacons = terminal().getBeacons();
				if(row >= 0 && row < beacons.size())
					beaconTip(tip, beacons.get(row));
			}
			else if(inside(APRS_MAP, mouseX, mouseY))
				mapTip(tip, mouseX, mouseY);
		}
		else if(currentTab == TAB_MSG && inside(MSG_BEACON_ENABLE, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.aprs.tip.beacon_enable"));
		if(!tip.isEmpty())
			guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
	}

	private void mapTip(List<Component> tip, int mouseX, int mouseY) {
		BlockPos here = terminal().getBlockPos();
		List<Heard> beacons = terminal().getBeacons();
		float cx = leftPos + APRS_MAP[0] + APRS_MAP[2] / 2.0F;
		float cy = topPos + APRS_MAP[1] + APRS_MAP[3] / 2.0F;
		float radius = Math.min(APRS_MAP[2], APRS_MAP[3]) / 2.0F - 6.0F;
		int range = mapRange(beacons);
		for(Heard heard : beacons) {
			BlockPos at = heard.packet().position();
			if(at == null)
				continue;
			float x = cx + (at.getX() - here.getX()) / (float)range * radius;
			float y = cy + (at.getZ() - here.getZ()) / (float)range * radius;
			if(Math.abs(mouseX - x) <= 4.0F && Math.abs(mouseY - y) <= 4.0F) {
				beaconTip(tip, heard);
				return;
			}
		}
	}

	private void beaconTip(List<Component> tip, Heard heard) {
		TncPacket packet = heard.packet();
		tip.add(Component.literal(packet.from()));
		if(packet.position() != null)
			tip.add(Component.literal(packet.position().getX() + ", " + packet.position().getY() + ", " + packet.position().getZ() + "  (" + distanceAndBearing(packet.position()) + ")"));
		if(!packet.text().isEmpty())
			tip.add(Component.literal(packet.text()));
		long ageTicks = minecraft == null || minecraft.level == null ? 0 : Math.max(0, minecraft.level.getGameTime() - heard.gameTime());
		tip.add(Component.translatable(packet.hops() > 0 ? "screen.radiocraft.aprs.tip.heard_via" : "screen.radiocraft.aprs.tip.heard",
				ageTicks / 1200, packet.hops()));
	}

	// -------------------- HELPERS --------------------

	/** Map range in blocks: a power of two from 64 up that fits the furthest station heard. */
	private int mapRange(List<Heard> beacons) {
		BlockPos here = terminal().getBlockPos();
		double furthest = 0.0D;
		for(Heard heard : beacons) {
			BlockPos at = heard.packet().position();
			if(at != null)
				furthest = Math.max(furthest, Math.max(Math.abs(at.getX() - here.getX()), Math.abs(at.getZ() - here.getZ())));
		}
		int range = 64;
		while(range < furthest * 1.1D && range < (1 << 24))
			range *= 2;
		return range;
	}

	private static String rangeLabel(int range) {
		return range >= 1000 ? (range / 1000) + "k" : String.valueOf(range);
	}

	private String distanceAndBearing(BlockPos at) {
		if(at == null)
			return "";
		BlockPos here = terminal().getBlockPos();
		int dx = at.getX() - here.getX();
		int dz = at.getZ() - here.getZ();
		int distance = (int)Math.round(Math.sqrt((double)dx * dx + (double)dz * dz));
		if(distance == 0)
			return "0m";
		String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		double degrees = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0D) % 360.0D;
		return (distance >= 1000 ? String.format("%.1fk", distance / 1000.0D) : distance + "m") + " " + points[(int)Math.round(degrees / 45.0D) % 8];
	}

	private void check(GuiGraphics guiGraphics, int[] r, boolean on) {
		if(on)
			guiGraphics.fill(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1, CHECK);
	}

	/** Text fitted in a light entry field, left aligned and centred vertically, cut short if it would not fit. */
	private void fieldText(GuiGraphics guiGraphics, int[] r, String value) {
		String shown = font.plainSubstrByWidth(value, (int)((r[2] - 2) / SMALL));
		text(guiGraphics, shown, r[0] + 1, r[1] + (r[3] - font.lineHeight * SMALL) / 2.0F + 0.5F, SMALL, FIELD_TEXT, false);
	}

	private void item(GuiGraphics guiGraphics, AprsIcon icon, float x, float y) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		guiGraphics.renderItem(new ItemStack(icon.item()), 0, 0);
		guiGraphics.pose().popPose();
	}
}

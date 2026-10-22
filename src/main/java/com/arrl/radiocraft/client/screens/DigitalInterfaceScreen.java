package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.screens.widgets.HoldButton;
import com.arrl.radiocraft.client.screens.widgets.ToggleButton;
import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity.Heard;
import com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity.Logged;
import com.arrl.radiocraft.common.menus.DigitalInterfaceMenu;
import com.arrl.radiocraft.common.network.serverbound.SDigitalInterfacePacket;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 * The digital interface's screen. APRS: station settings, beacons heard (list on the paper, plotted on the screen
 * around this station, north up) and beaconing; MSG: messages to and from this callsign and a box to write one. The
 * three lamps top right beacon now (blue), claim the callsign (green) and show unread messages (red). RTTY and FILES
 * are still to come.
 */
public class DigitalInterfaceScreen extends AbstractContainerScreen<DigitalInterfaceMenu> {

	private static final ResourceLocation TEXTURE_RTTY = Radiocraft.id("textures/gui/digital_interface_rtty_send.png");
	private static final ResourceLocation TEXTURE_APRS = Radiocraft.id("textures/gui/digital_interface_arps.png");
	private static final ResourceLocation TEXTURE_MSG = Radiocraft.id("textures/gui/digital_interface_msg.png");
	private static final ResourceLocation TEXTURE_FILES = Radiocraft.id("textures/gui/digital_interface_files.png");
	private static final ResourceLocation WIDGETS_TEXTURE = Radiocraft.id("textures/gui/digital_interface_widgets.png");

	private static final int TAB_RTTY = 0;
	private static final int TAB_APRS = 1;
	private static final int TAB_MSG = 2;
	private static final int TAB_FILES = 3;

	private static final int INK = 0xFF2B2118; // On the paper.
	private static final int FIELD_TEXT = 0xFF1E2428; // In the light entry fields.
	private static final int LCD = 0xFFBFE8C4; // On the grey screens.
	private static final int LCD_DIM = 0xFF7E9A86;
	private static final int CHECK = 0xFF3CC83C;
	private static final int LAMP_RED = 0xC0FF3030;

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
	private static final int[] MSG_BUZZER = {89, 36, 5, 5};
	private static final int[] MSG_BEACON_ENABLE = {78, 45, 5, 5};
	private static final int[] MSG_SEND = {29, 53, 51, 9};
	private static final int[] MSG_COMPOSE = {22, 76, 64, 39};
	private static final int[] MSG_PAPER = {107, 44, 89, 66};
	private static final int[] RTTY_PAPER = {25, 45, 171, 55};
	private static final int[] RTTY_INPUT = {17, 114, 154, 11};
	private static final int[] FILES_TO = {40, 60, 57, 7};
	private static final int[] FILES_PAPER = {140, 47, 53, 27};
	private static final int[] FILES_LED_X = {26, 41, 56, 71, 86, 101};
	private static final int FILES_LED_Y = 107;
	private static final int PANEL_HEIGHT = 133;
	/** The player inventory half of the vanilla chest screen, a stand-in until the interface has its own. */
	private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final float RTTY_SCALE = 0.6F;
	private static final int RTTY_LINE = 6;
	private static final int PAPER_OUT = 0xFFB02020;

	private static final float SMALL = 0.5F;
	private static final int SMALL_LINE = 5;

	private enum Editing { NONE, CALLSIGN, COMMENT }

	private int currentTab = TAB_APRS;
	private Editing editing = Editing.NONE;
	private EditBox fieldBox;
	private EditBox toBox;
	private MultiLineEditBox messageBox;
	private String draftTo = "";
	private String draftMessage = "";
	private int beaconScroll = 0;
	private int messageScroll = 0;
	private String draftFileTo = "";
	private String rttyTyped = ""; // This line as typed so far; it is already on the air, so there is no rubbing out.
	private int rttyScroll = 0; // Lines scrolled back from the bottom of the roll.

	public DigitalInterfaceScreen(DigitalInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 217;
		this.imageHeight = PANEL_HEIGHT + 96; // Room for the player's inventory under the panel (FILES tab).
		if(menu.blockEntity.hasUnread())
			currentTab = TAB_MSG; // Straight to the waiting message.
	}

	private DigitalInterfaceBlockEntity tnc() {
		return menu.blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		fieldBox = null;
		toBox = null;
		messageBox = null;

		addRenderableWidget(new ToggleButton(currentTab == TAB_APRS, leftPos + 16, topPos + 17, 35, 14, 0, 0, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_APRS)));
		addRenderableWidget(new ToggleButton(currentTab == TAB_MSG, leftPos + 50, topPos + 17, 36, 14, 144, 0, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_MSG)));
		addRenderableWidget(new ToggleButton(currentTab == TAB_RTTY, leftPos + 85, topPos + 17, 36, 14, 0, 28, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_RTTY)));
		addRenderableWidget(new ToggleButton(currentTab == TAB_FILES, leftPos + 120, topPos + 17, 37, 14, 70, 0, WIDGETS_TEXTURE, 217, 80, btn -> selectTab(TAB_FILES)));

		if(currentTab == TAB_APRS && editing != Editing.NONE) {
			fieldBox = new EditBox(font, leftPos + APRS_MAP[0] + 2, topPos + APRS_MAP[1] + 30, APRS_MAP[2] - 4, 12, Component.empty());
			fieldBox.setMaxLength(editing == Editing.CALLSIGN ? TncPacket.CALLSIGN_MAX : TncPacket.BEACON_TEXT_MAX);
			fieldBox.setValue(editing == Editing.CALLSIGN ? tnc().getCallsign() : tnc().getBeaconComment());
			addRenderableWidget(fieldBox);
			setInitialFocus(fieldBox);
		}
		else if(currentTab == TAB_MSG) {
			toBox = new EditBox(font, leftPos + MSG_COMPOSE[0] + 14, topPos + MSG_COMPOSE[1] + 2, MSG_COMPOSE[2] - 16, 9, Component.empty());
			toBox.setBordered(false);
			toBox.setTextColor(LCD);
			toBox.setMaxLength(TncPacket.CALLSIGN_MAX);
			toBox.setValue(draftTo);
			toBox.setResponder(value -> draftTo = value);
			addRenderableWidget(toBox);
			messageBox = new MultiLineEditBox(font, leftPos + MSG_COMPOSE[0], topPos + MSG_COMPOSE[1] + 12, MSG_COMPOSE[2], MSG_COMPOSE[3] - 12,
					Component.translatable("screen.radiocraft.digital_interface.message_hint"), Component.empty());
			messageBox.setCharacterLimit(TncPacket.MESSAGE_TEXT_MAX);
			messageBox.setValue(draftMessage);
			messageBox.setValueListener(value -> draftMessage = value);
			addRenderableWidget(messageBox);
		}
		else if(currentTab == TAB_RTTY)
			addRenderableWidget(new HoldButton(leftPos + 174, topPos + 113, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, btn -> endRttyLine(), btn -> {}));
		else if(currentTab == TAB_FILES) {
			addRenderableWidget(new HoldButton(leftPos + 78, topPos + 40, 28, 14, 72, 28, WIDGETS_TEXTURE, 217, 80, btn -> sendFile(), btn -> {}));
			toBox = new EditBox(font, leftPos + FILES_TO[0] + 1, topPos + FILES_TO[1], FILES_TO[2] - 2, FILES_TO[3] + 1, Component.empty());
			toBox.setBordered(false);
			toBox.setTextColor(FIELD_TEXT);
			toBox.setMaxLength(TncPacket.CALLSIGN_MAX);
			toBox.setValue(draftFileTo);
			toBox.setResponder(value -> draftFileTo = value);
			addRenderableWidget(toBox);
		}
		menu.setSlotsVisible(currentTab == TAB_FILES);

		if(currentTab == TAB_MSG && tnc().hasUnread())
			SDigitalInterfacePacket.send(tnc().getBlockPos(), "read");
	}

	/** SEND on the FILES tab: the outbox goes to the callsign in TO, or to everyone if it is empty. */
	private void sendFile() {
		CompoundTag tag = new CompoundTag();
		tag.putString("to", draftFileTo);
		SDigitalInterfacePacket.send(tnc().getBlockPos(), "file", tag);
	}

	private void selectTab(int tab) {
		currentTab = tab;
		editing = Editing.NONE;
		rebuildWidgets();
	}

	// -------------------- INPUT --------------------

	private boolean inside(int[] r, double mouseX, double mouseY) {
		double x = mouseX - leftPos;
		double y = mouseY - topPos;
		return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
	}

	private void settings(String key, boolean value) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(key, value);
		SDigitalInterfacePacket.send(tnc().getBlockPos(), "settings", tag);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if(button == 0) {
			if(inside(BLUE_LAMP, mouseX, mouseY)) {
				SDigitalInterfacePacket.send(tnc().getBlockPos(), "beacon");
				return true;
			}
			if(inside(GREEN_LAMP, mouseX, mouseY)) {
				SDigitalInterfacePacket.send(tnc().getBlockPos(), "claim");
				return true;
			}
			if(currentTab == TAB_APRS && editing == Editing.NONE) {
				if(inside(APRS_CONTACT_BUZZER, mouseX, mouseY)) {
					settings("contactBuzzer", !tnc().isContactBuzzer());
					return true;
				}
				if(inside(APRS_BEACON_ENABLE, mouseX, mouseY)) {
					settings("beaconEnabled", !tnc().isBeaconEnabled());
					return true;
				}
				if(inside(APRS_CALLSIGN, mouseX, mouseY)) {
					startEditing(Editing.CALLSIGN);
					return true;
				}
				if(inside(APRS_COMMENT, mouseX, mouseY)) {
					startEditing(Editing.COMMENT);
					return true;
				}
				if(inside(APRS_ICON, mouseX, mouseY)) {
					CompoundTag tag = new CompoundTag();
					tag.putString("icon", tnc().getIcon().next().name());
					SDigitalInterfacePacket.send(tnc().getBlockPos(), "settings", tag);
					return true;
				}
			}
			if(currentTab == TAB_MSG) {
				if(inside(MSG_BUZZER, mouseX, mouseY)) {
					settings("messageBuzzer", !tnc().isMessageBuzzer());
					return true;
				}
				if(inside(MSG_BEACON_ENABLE, mouseX, mouseY)) {
					settings("beaconEnabled", !tnc().isBeaconEnabled());
					return true;
				}
				if(inside(MSG_SEND, mouseX, mouseY)) {
					sendMessage();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void sendMessage() {
		String to = TncPacket.sanitizeCallsign(draftTo);
		String text = TncPacket.sanitizeText(draftMessage.replace('\n', ' '), TncPacket.MESSAGE_TEXT_MAX);
		if(to.isEmpty() || text.isEmpty())
			return;
		CompoundTag tag = new CompoundTag();
		tag.putString("to", to);
		tag.putString("text", text);
		SDigitalInterfacePacket.send(tnc().getBlockPos(), "message", tag);
		draftMessage = "";
		if(messageBox != null)
			messageBox.setValue("");
	}

	private void startEditing(Editing field) {
		editing = field;
		rebuildWidgets();
	}

	private void finishEditing(boolean apply) {
		if(apply && fieldBox != null) {
			String value = fieldBox.getValue();
			if(editing == Editing.CALLSIGN) {
				if(TncPacket.sanitizeCallsign(value).isEmpty())
					SDigitalInterfacePacket.send(tnc().getBlockPos(), "claim"); // Empty means "mine".
				else {
					CompoundTag tag = new CompoundTag();
					tag.putString("callsign", value);
					SDigitalInterfacePacket.send(tnc().getBlockPos(), "callsign", tag);
				}
			}
			else {
				CompoundTag tag = new CompoundTag();
				tag.putString("comment", value);
				SDigitalInterfacePacket.send(tnc().getBlockPos(), "settings", tag);
			}
		}
		editing = Editing.NONE;
		rebuildWidgets();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(editing != Editing.NONE) {
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
		if(keyCode != GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof MultiLineEditBox box && box.isFocused()) {
			box.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		if(currentTab == TAB_RTTY && keyCode != GLFW.GLFW_KEY_ESCAPE) {
			if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
				endRttyLine();
			return true; // The keyboard is the teleprinter's: no closing on 'e', no hotbar keys. Characters come in charTyped.
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** On the RTTY tab every printable key goes straight out on the air, upper case, as typed. */
	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(currentTab == TAB_RTTY) {
			String c = Rtty.sanitize(String.valueOf(codePoint), 1);
			if(!c.isEmpty() && c.charAt(0) != '\n') {
				sendRtty(c);
				rttyTyped = (rttyTyped + c).length() > 400 ? (rttyTyped + c).substring(200) : rttyTyped + c;
			}
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	private void sendRtty(String text) {
		CompoundTag tag = new CompoundTag();
		tag.putString("text", text);
		SDigitalInterfacePacket.send(tnc().getBlockPos(), "rtty", tag);
		rttyScroll = 0; // Back to the bottom of the roll to watch it print.
	}

	/** SEND or Enter: carriage return and line feed, the line is done. */
	private void endRttyLine() {
		if(!rttyTyped.isEmpty())
			sendRtty("\n");
		rttyTyped = "";
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int step = scrollY > 0 ? -1 : 1;
		if(currentTab == TAB_RTTY && inside(RTTY_PAPER, mouseX, mouseY)) {
			rttyScroll = Math.max(0, rttyScroll - step); // Up the roll; clamped when drawn.
			return true;
		}
		if(currentTab == TAB_APRS && inside(APRS_PAPER, mouseX, mouseY)) {
			beaconScroll = Mth.clamp(beaconScroll + step, 0, Math.max(0, tnc().getBeacons().size() - APRS_PAPER[3] / SMALL_LINE));
			return true;
		}
		if(currentTab == TAB_MSG && inside(MSG_PAPER, mouseX, mouseY)) {
			messageScroll = Math.max(0, messageScroll + step); // Clamped when drawn, where the wrapped length is known.
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// -------------------- RENDERING --------------------

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
		renderPanelTooltips(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		ResourceLocation texture = switch(currentTab) {
			case TAB_APRS -> TEXTURE_APRS;
			case TAB_MSG -> TEXTURE_MSG;
			case TAB_FILES -> TEXTURE_FILES;
			default -> TEXTURE_RTTY;
		};
		guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, PANEL_HEIGHT, 217, 133);
		if(currentTab == TAB_FILES)
			guiGraphics.blit(INVENTORY_TEXTURE, leftPos + DigitalInterfaceMenu.INVENTORY_X - 8, topPos + PANEL_HEIGHT, 0, 126, 176, 96);
		if(tnc().hasUnread() && (System.currentTimeMillis() / 500) % 2 == 0)
			fillAt(guiGraphics, RED_LAMP, leftPos, topPos, LAMP_RED);
		if(currentTab == TAB_APRS && editing != Editing.NONE)
			fillAt(guiGraphics, APRS_MAP, leftPos, topPos, 0xFF3A4448); // Here, under the edit box: labels draw over widgets.
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		switch(currentTab) {
			case TAB_APRS -> renderAprs(guiGraphics, mouseX, mouseY);
			case TAB_MSG -> renderMessages(guiGraphics);
			case TAB_RTTY -> renderRtty(guiGraphics);
			default -> renderFiles(guiGraphics);
		}
	}

	/** Progress LEDs (all dark when idle) and, on the paper, the transfer going on and the log of the last ones. */
	private void renderFiles(GuiGraphics guiGraphics) {
		DigitalInterfaceBlockEntity tnc = tnc();
		float progress = tnc.getTransferProgress();
		int lit = progress < 0.0F ? 0 : Math.max(1, (int)Math.ceil(progress * FILES_LED_X.length));
		for(int i = lit; i < FILES_LED_X.length; i++)
			guiGraphics.fill(FILES_LED_X[i], FILES_LED_Y, FILES_LED_X[i] + 8, FILES_LED_Y + 10, 0xC0101010);

		List<String> lines = new ArrayList<>();
		if(progress >= 0.0F)
			lines.add(tnc.getTransferLabel() + " " + (int)(progress * 100) + "%");
		lines.addAll(tnc.getFileLog());
		float scale = 0.4F;
		int rows = (int)(FILES_PAPER[3] / (font.lineHeight * scale));
		for(int i = 0; i < rows && i < lines.size(); i++) {
			String line = font.plainSubstrByWidth(lines.get(i), (int)(FILES_PAPER[2] / scale));
			text(guiGraphics, line, FILES_PAPER[0], FILES_PAPER[1] + i * font.lineHeight * scale, scale, INK, false);
		}
	}

	private void renderAprs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		DigitalInterfaceBlockEntity tnc = tnc();
		check(guiGraphics, APRS_CONTACT_BUZZER, tnc.isContactBuzzer());
		check(guiGraphics, APRS_BEACON_ENABLE, tnc.isBeaconEnabled());
		fieldText(guiGraphics, APRS_CALLSIGN, tnc.getCallsign().isEmpty() ? "-" : tnc.getCallsign());
		fieldText(guiGraphics, APRS_COMMENT, tnc.getBeaconComment());
		fieldText(guiGraphics, APRS_ICON, tnc.getIcon().name());
		item(guiGraphics, tnc.getIcon(), APRS_ICON[0] + APRS_ICON[2] + 2, APRS_ICON[1] - 1);

		List<Heard> beacons = tnc.getBeacons();
		int rows = APRS_PAPER[3] / SMALL_LINE;
		beaconScroll = Mth.clamp(beaconScroll, 0, Math.max(0, beacons.size() - rows));
		if(beacons.isEmpty())
			text(guiGraphics, Component.translatable("screen.radiocraft.digital_interface.no_beacons").getString(), APRS_PAPER[0], APRS_PAPER[1], SMALL, INK, false);
		for(int i = 0; i < rows && beaconScroll + i < beacons.size(); i++) {
			TncPacket packet = beacons.get(beaconScroll + i).packet();
			text(guiGraphics, packet.from() + " " + distanceAndBearing(packet.position()), APRS_PAPER[0], APRS_PAPER[1] + i * SMALL_LINE, SMALL, INK, false);
		}

		if(editing != Editing.NONE) {
			String label = editing == Editing.CALLSIGN ? "screen.radiocraft.digital_interface.edit_callsign" : "screen.radiocraft.digital_interface.edit_comment";
			text(guiGraphics, Component.translatable(label).getString(), APRS_MAP[0] + APRS_MAP[2] / 2.0F, APRS_MAP[1] + 20, 0.6F, LCD, true);
			text(guiGraphics, Component.translatable("screen.radiocraft.digital_interface.edit_keys").getString(), APRS_MAP[0] + APRS_MAP[2] / 2.0F, APRS_MAP[1] + 50, SMALL, LCD_DIM, true);
			return;
		}
		renderMap(guiGraphics, beacons);
	}

	/** Heard stations around this one, north up, scaled so the furthest fits; the range is in the corner. */
	private void renderMap(GuiGraphics guiGraphics, List<Heard> beacons) {
		BlockPos here = tnc().getBlockPos();
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
		DigitalInterfaceBlockEntity tnc = tnc();
		check(guiGraphics, MSG_BUZZER, tnc.isMessageBuzzer());
		check(guiGraphics, MSG_BEACON_ENABLE, tnc.isBeaconEnabled());
		text(guiGraphics, "TO", MSG_COMPOSE[0] + 2, MSG_COMPOSE[1] + 3, 0.6F, LCD_DIM, false);

		List<FormattedCharSequence> lines = new ArrayList<>();
		int wrap = (int)(MSG_PAPER[2] / SMALL);
		for(Logged logged : tnc.getMessages()) {
			TncPacket packet = logged.packet();
			String line = logged.outgoing() ? "> " + packet.to() + ": " + packet.text() : packet.from() + ": " + packet.text();
			lines.addAll(font.split(Component.literal(line), wrap));
		}
		int rows = MSG_PAPER[3] / SMALL_LINE;
		messageScroll = Mth.clamp(messageScroll, 0, Math.max(0, lines.size() - rows));
		if(lines.isEmpty())
			text(guiGraphics, Component.translatable("screen.radiocraft.digital_interface.no_messages").getString(), MSG_PAPER[0], MSG_PAPER[1], SMALL, INK, false);
		for(int i = 0; i < rows && messageScroll + i < lines.size(); i++) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(MSG_PAPER[0], MSG_PAPER[1] + i * SMALL_LINE, 0.0F);
			guiGraphics.pose().scale(SMALL, SMALL, 1.0F);
			guiGraphics.drawString(font, lines.get(messageScroll + i), 0, 0, INK, false);
			guiGraphics.pose().popPose();
		}
	}

	/** The teleprinter roll, newest line at the bottom, and the line being typed under it. */
	private void renderRtty(GuiGraphics guiGraphics) {
		DigitalInterfaceBlockEntity tnc = tnc();
		boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
		List<FormattedCharSequence> lines = new ArrayList<>();
		int wrap = (int)(RTTY_PAPER[2] / RTTY_SCALE);
		for(String line : tnc.getRttyLines())
			lines.addAll(font.split(Component.literal(line), wrap));
		String open = tnc.getRttyOpenLine();
		if(open != null)
			lines.addAll(font.split(Component.literal(open + (blink ? "_" : " ")), wrap));
		int rows = RTTY_PAPER[3] / RTTY_LINE;
		rttyScroll = Mth.clamp(rttyScroll, 0, Math.max(0, lines.size() - rows));
		int end = lines.size() - rttyScroll;
		int start = Math.max(0, end - rows);
		for(int i = start; i < end; i++) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(RTTY_PAPER[0], RTTY_PAPER[1] + RTTY_PAPER[3] - (end - i) * RTTY_LINE, 0.0F);
			guiGraphics.pose().scale(RTTY_SCALE, RTTY_SCALE, 1.0F);
			guiGraphics.drawString(font, lines.get(i), 0, 0, INK, false);
			guiGraphics.pose().popPose();
		}

		int paperLines = tnc.getPaperLines();
		String paper = paperLines > 0 ? Component.translatable("screen.radiocraft.digital_interface.paper", paperLines).getString()
				: Component.translatable("screen.radiocraft.digital_interface.paper_out").getString();
		text(guiGraphics, paper, RTTY_PAPER[0] + RTTY_PAPER[2] - font.width(paper) * SMALL, RTTY_PAPER[1] - 2, SMALL, paperLines > 0 ? INK : PAPER_OUT, false);

		String typed = rttyTyped + (blink ? "_" : "");
		String shown = font.plainSubstrByWidth(new StringBuilder(typed).reverse().toString(), (int)((RTTY_INPUT[2] - 4) / 0.75F));
		text(guiGraphics, new StringBuilder(shown).reverse().toString(), RTTY_INPUT[0] + 2, RTTY_INPUT[1] + 2.5F, 0.75F, LCD, false);
	}

	private void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> tip = new ArrayList<>();
		if(inside(BLUE_LAMP, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.beacon"));
		else if(inside(GREEN_LAMP, mouseX, mouseY)) {
			tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.claim"));
			if(!tnc().getCallsignSetBy().isEmpty())
				tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.set_by", tnc().getCallsign(), tnc().getCallsignSetBy()));
		}
		else if(inside(RED_LAMP, mouseX, mouseY))
			tip.add(Component.translatable(tnc().hasUnread() ? "screen.radiocraft.digital_interface.tip.unread" : "screen.radiocraft.digital_interface.tip.no_unread"));
		else if(currentTab == TAB_APRS && editing == Editing.NONE) {
			if(inside(APRS_CALLSIGN, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.callsign"));
			else if(inside(APRS_COMMENT, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.comment"));
			else if(inside(APRS_ICON, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.icon"));
			else if(inside(APRS_BEACON_ENABLE, mouseX, mouseY))
				tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.beacon_enable"));
			else if(inside(APRS_PAPER, mouseX, mouseY)) {
				int row = (mouseY - topPos - APRS_PAPER[1]) / SMALL_LINE + beaconScroll;
				List<Heard> beacons = tnc().getBeacons();
				if(row >= 0 && row < beacons.size())
					beaconTip(tip, beacons.get(row));
			}
			else if(inside(APRS_MAP, mouseX, mouseY))
				mapTip(tip, mouseX, mouseY);
		}
		else if(currentTab == TAB_MSG && inside(MSG_BEACON_ENABLE, mouseX, mouseY))
			tip.add(Component.translatable("screen.radiocraft.digital_interface.tip.beacon_enable"));
		if(!tip.isEmpty())
			guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
	}

	private void mapTip(List<Component> tip, int mouseX, int mouseY) {
		BlockPos here = tnc().getBlockPos();
		List<Heard> beacons = tnc().getBeacons();
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
		tip.add(Component.translatable(packet.hops() > 0 ? "screen.radiocraft.digital_interface.tip.heard_via" : "screen.radiocraft.digital_interface.tip.heard",
				ageTicks / 1200, packet.hops()));
	}

	// -------------------- HELPERS --------------------

	/** Map range in blocks: a power of two from 64 up that fits the furthest station heard. */
	private int mapRange(List<Heard> beacons) {
		BlockPos here = tnc().getBlockPos();
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
		BlockPos here = tnc().getBlockPos();
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

	private static void fillAt(GuiGraphics guiGraphics, int[] r, int x, int y, int colour) {
		guiGraphics.fill(x + r[0], y + r[1], x + r[0] + r[2], y + r[1] + r[3], colour);
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

	private void text(GuiGraphics guiGraphics, String text, float x, float y, float scale, int colour, boolean centred) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.drawString(font, text, centred ? -font.width(text) / 2 : 0, 0, colour, false);
		guiGraphics.pose().popPose();
	}
}

package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity.Heard;
import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity.Logged;
import com.arrl.radiocraft.common.blockentities.StationBus;
import com.arrl.radiocraft.common.menus.AprsTerminalMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The APRS terminal's screen: an H19 running an APRS program, drawn as text on its green screen, 64 columns by 25
 * rows in a fixed grid, with no buttons or boxes but characters. It boots (with disk and printing noises) the first
 * time it is opened after a radio on its bus comes on. Then the pages, on the top row, by number key or by clicking:
 * <ol>
 *     <li>MAP (the page it opens on), the stations heard drawn round this one (see {@link H19Map}); click one for its
 *     details.</li>
 *     <li>INBOX, messages for this station; one opens to read, reply to, or find the sender on the map.</li>
 *     <li>LIST, the stations heard, with distance, bearing and comment.</li>
 *     <li>MSG, a message to send and those sent, each SENDING, RETRYING n/5, SENT (its ack came back) or FAILED.</li>
 *     <li>SETUP (also the S key), the beacon, callsign, symbol, comment, buzzers, digipeating, and how long stations
 *     stay on the map and list without being heard again.</li>
 * </ol>
 * The side panel has MAP, INBX, LIST and MSG buttons. INBX flashes red for a message that came in since the inbox was
 * last looked at, MAP and LIST for a station heard for the first time since the map or list was.
 */
public class AprsTerminalScreen extends StationPanelScreen<AprsTerminalMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/h19_gui.png");
	private static final int TEXTURE_WIDTH = 300, TEXTURE_HEIGHT = 200;
	/** The glass of the screen in the texture: {x, y, width, height}. */
	private static final int[] CRT = {11, 10, 200, 134};
	private static final ResourceLocation BUTTONS = Radiocraft.id("textures/gui/h19_gui_buttons.png");
	private static final int BUTTONS_WIDTH = 300, BUTTONS_HEIGHT = 200;
	/** The side panel's buttons, top to bottom (MAP, INBX, LIST, MSG): {x, y, width, height}. */
	private static final int[][] SIDE_BUTTONS = {{232, 14, 53, 27}, {232, 42, 53, 27}, {232, 70, 53, 27}, {232, 98, 53, 27}};
	/** Each button's row in h19_gui_buttons.png, in SIDE_BUTTONS order. */
	private static final int[] BUTTON_ROWS = {22, 82, 112, 52};
	/** Columns of h19_gui_buttons.png: the button lit red, as it stands, and hovered or pressed. */
	private static final int BUTTON_LIT = 84, BUTTON_NORMAL = 140, BUTTON_PRESSED = 196;

	// The text grid: each character cell is CELL_W by CELL_H texels, the font drawn at half size.
	private static final int COLS = 64, ROWS = 25, CELL_W = 3, CELL_H = 5;
	private static final float TEXT_SCALE = 0.5F;
	private static final int GRID_X = CRT[0] + (CRT[2] - COLS * CELL_W) / 2, GRID_Y = CRT[1] + (CRT[3] - ROWS * CELL_H) / 2;
	/** Between the top and bottom rows, where the map is drawn. */
	private static final int[] MAP_AREA = {GRID_X, GRID_Y + CELL_H, COLS * CELL_W, (ROWS - 2) * CELL_H};
	private static final int LIST_TOP = 4, LIST_ROWS = 19, STATUS_ROW = 23, FOOTER_ROW = 24;

	private static final int GREEN = 0xFF39FF14; // Neon phosphor.
	private static final int DIM_GREEN = 0xFF1C8C0C;
	private static final int INK = 0xFF021402; // Text on a lit (inverse) cell.
	private static final byte NORMAL = 0, FAINT = 1, INVERSE = 2;

	private enum Page { BOOT, INBOX, READ, MAP, LIST, MSG, SETUP }
	private static final Page[] TABS = {Page.MAP, Page.INBOX, Page.LIST, Page.MSG, Page.SETUP};
	private static final Page[] SIDE_PAGES = {Page.MAP, Page.INBOX, Page.LIST, Page.MSG};

	private static final String[] ART = {
			" ________  ________  ________  ________",
			"|\\   __  \\|\\   __  \\|\\   __  \\|\\   ____\\",
			"\\ \\  \\|\\  \\ \\  \\|\\  \\ \\  \\|\\  \\ \\  \\___|_",
			" \\ \\   __  \\ \\   ____\\ \\   _  _\\ \\_____  \\",
			"  \\ \\  \\ \\  \\ \\  \\___|\\ \\  \\\\  \\\\|____|\\  \\",
			"   \\ \\__\\ \\__\\ \\__\\    \\ \\__\\\\ _\\ ____\\_\\  \\",
			"    \\|__|\\|__|\\|__|     \\|__|\\|__|\\_________\\",
			"       By Bob Bruninga, WB4APR.  \\|_________|"
	};

	private final char[][] chars = new char[ROWS][COLS];
	private final byte[][] attrs = new byte[ROWS][COLS];
	private final boolean[][] opaque = new boolean[ROWS][COLS]; // Cells drawn on black over the map.
	/** A clickable run of cells, rebuilt each frame. */
	private record Spot(int row, int col, int length, Runnable action) {}
	private final List<Spot> spots = new ArrayList<>();
	private final RandomSource random = RandomSource.create();
	/** When the screen had last been opened before now: messages since are new. */
	private final long openedBefore;
	/** Game time the inbox, and the map or list, were last on the screen: what came after makes the buttons flash. */
	private long inboxViewedAt, stationsViewedAt;

	private Page page = Page.MAP;
	private long pageAt = System.currentTimeMillis(); // The page it opens on draws in too.
	private int revealed = 0;
	private String status = "";
	private long statusUntil = 0L;

	private int inboxSel = 0, inboxScroll = 0, readOption = 0;
	private int listSel = 0, listScroll = 0;
	@Nullable
	private String popup = null; // The station whose details are open, over MAP or LIST.
	private int popupOption = 0;

	@Nullable
	private H19Map map = null;
	private boolean mapFitted = false, dragging = false;
	private double mapX, mapZ, mapBpp = 1.0D;
	private String mapSelected = "";

	private enum Field { TO, TEXT, SEND, CLEAR }
	private Field msgFocus = Field.TO;
	private String draftTo = "", draftText = "";

	private static final String[] SETUP_ITEMS = {"BEACON", "SEND BEACON NOW", "CALLSIGN", "SYMBOL", "COMMENT", "CONTACT BUZZER", "MESSAGE BUZZER", "DIGIPEAT",
			"REMOVE STATIONS AFTER", "REBOOT"};
	private static final int SETUP_CALLSIGN = 2, SETUP_SYMBOL = 3, SETUP_COMMENT = 4, SETUP_EXPIRY = 8;
	/** Width of the setup labels with their dots; the values start two after. */
	private static final int SETUP_LABEL = 22, SETUP_TOP = 3;
	private int setupSel = 0;
	private boolean editingComment = false, editingCallsign = false;
	private String commentDraft = "", callsignDraft = "";

	private enum StepKind { PRINT, FAST, TYPE, PROMPT, WAIT, SEEK, BEEP, CALLSIGN, ASK }
	private record Step(StepKind kind, String text, int millis) {}
	private final List<String> console = new ArrayList<>();
	private final List<Step> script = new ArrayList<>();
	private String consoleLine = "";
	private int step = 0, stepChars = 0;
	private long stepAt = 0L, nextNoise = 0L;
	private String bootInput = "";
	private boolean bootAnswered = false;

	/** A sound to play a little later (disk seeks, modem chirps). */
	private record Pending(long at, Runnable play) {}
	private final List<Pending> pending = new ArrayList<>();

	public AprsTerminalScreen(AprsTerminalMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_WIDTH;
		this.imageHeight = TEXTURE_HEIGHT;
		this.openedBefore = menu.blockEntity.getLastOpenedGameTime(); // Not yet updated for this opening.
		this.inboxViewedAt = this.stationsViewedAt = openedBefore;
		if(menu.blockEntity.isBootPending())
			startBoot();
		else
			switchPage(Page.MAP);
	}

	private AprsTerminalBlockEntity terminal() {
		return menu.blockEntity;
	}

	@Override
	public void removed() {
		super.removed();
		if(map != null) {
			map.close();
			map = null;
		}
	}

	// -------------------- DATA --------------------

	private List<Logged> inbox() {
		return terminal().getMessages().stream().filter(logged -> !logged.outgoing()).toList();
	}

	private List<Logged> sent() {
		return terminal().getMessages().stream().filter(Logged::outgoing).toList();
	}

	private boolean isNew(Logged logged) {
		return logged.gameTime() > openedBefore;
	}

	@Nullable
	private Heard stationNamed(String callsign) {
		for(Heard heard : terminal().getBeacons())
			if(heard.packet().from().equals(callsign))
				return heard;
		return null;
	}

	private long gameTime() {
		return minecraft == null || minecraft.level == null ? 0L : minecraft.level.getGameTime();
	}

	private String frequency() {
		return terminal().isRadioOn() ? String.format(Locale.ROOT, "%.3f MHZ", terminal().getRadioFrequency() / 1.0E6F) : "NO RADIO";
	}

	private String callsign() {
		return terminal().getCallsign();
	}

	// -------------------- PAGES --------------------

	private void switchPage(Page to) {
		if(to == page && to != Page.BOOT)
			return;
		page = to;
		pageAt = System.currentTimeMillis();
		revealed = 0;
		popup = null;
		editingComment = editingCallsign = false;
		if(to == Page.MSG)
			msgFocus = draftTo.isEmpty() ? Field.TO : Field.TEXT;
		seekBurst(2);
	}

	private void setStatus(String text) {
		status = text;
		statusUntil = System.currentTimeMillis() + 4000L;
	}

	private void openMessage(int index) {
		inboxSel = index;
		readOption = 0;
		switchPage(Page.READ);
	}

	private void reply(String to) {
		draftTo = TncPacket.sanitizeCallsign(to);
		switchPage(Page.MSG);
		msgFocus = Field.TEXT;
	}

	private void showOnMap(String callsign) {
		Heard heard = stationNamed(callsign);
		if(heard == null || heard.packet().position() == null) {
			setStatus("NO POSITION HEARD FROM " + callsign);
			return;
		}
		if(!mapFitted)
			fitMap();
		mapX = heard.packet().position().getX() + 0.5D;
		mapZ = heard.packet().position().getZ() + 0.5D;
		mapSelected = callsign;
		switchPage(Page.MAP);
	}

	private void openPopup(String callsign) {
		popup = callsign;
		popupOption = 0;
		mapSelected = callsign;
		seekBurst(1);
	}

	private void popupAction(int option) {
		String callsign = popup;
		popup = null;
		if(callsign == null)
			return;
		switch(option) {
			case 0 -> reply(callsign);
			case 1 -> showOnMap(callsign);
			default -> {}
		}
	}

	private void sendMessage() {
		String to = TncPacket.sanitizeCallsign(draftTo);
		String text = TncPacket.sanitizeText(draftText, TncPacket.MESSAGE_TEXT_MAX);
		if(to.isEmpty()) {
			setStatus("WHO TO? TYPE A CALLSIGN");
			msgFocus = Field.TO;
			return;
		}
		if(text.isEmpty()) {
			setStatus("NOTHING TO SEND");
			msgFocus = Field.TEXT;
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.putString("to", to);
		tag.putString("text", text);
		SStationPacket.send(terminal().getBlockPos(), "message", tag);
		chirp();
		setStatus("SENDING TO " + to);
		draftText = "";
		msgFocus = Field.TEXT;
	}

	private void settings(String key, boolean value) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(key, value);
		SStationPacket.send(terminal().getBlockPos(), "settings", tag);
	}

	private void setIcon(AprsIcon icon) {
		CompoundTag tag = new CompoundTag();
		tag.putString("icon", icon.name());
		SStationPacket.send(terminal().getBlockPos(), "settings", tag);
	}

	private static AprsIcon previous(AprsIcon icon) {
		AprsIcon[] all = AprsIcon.values();
		return all[(icon.ordinal() + all.length - 1) % all.length];
	}

	private void setupAction(int item) {
		AprsTerminalBlockEntity terminal = terminal();
		setupSel = item;
		switch(item) {
			case 0 -> settings("beaconEnabled", !terminal.isBeaconEnabled());
			case 1 -> {
				SStationPacket.send(terminal.getBlockPos(), "beacon");
				chirp();
				setStatus("BEACON SENT");
			}
			case SETUP_CALLSIGN -> {
				editingCallsign = true;
				callsignDraft = "";
			}
			case SETUP_SYMBOL -> setIcon(terminal.getIcon().next());
			case SETUP_COMMENT -> {
				editingComment = true;
				commentDraft = terminal.getBeaconComment();
			}
			case 5 -> settings("contactBuzzer", !terminal.isContactBuzzer());
			case 6 -> settings("messageBuzzer", !terminal.isMessageBuzzer());
			case 7 -> settings("digipeat", !terminal.isDigipeatEnabled());
			case SETUP_EXPIRY -> setExpiry(1);
			case 9 -> startBoot();
			default -> {}
		}
	}

	private void saveComment() {
		CompoundTag tag = new CompoundTag();
		tag.putString("comment", TncPacket.sanitizeText(commentDraft, TncPacket.BEACON_TEXT_MAX));
		SStationPacket.send(terminal().getBlockPos(), "settings", tag);
		editingComment = false;
	}

	/** Steps REMOVE STATIONS AFTER through its choices, forwards ({@code 1}) or back. */
	private void setExpiry(int direction) {
		int[] choices = AprsTerminalBlockEntity.STATION_EXPIRY_CHOICES;
		int at = 0;
		for(int i = 0; i < choices.length; i++)
			if(choices[i] == terminal().getStationExpiryMinutes())
				at = i;
		CompoundTag tag = new CompoundTag();
		tag.putInt("stationExpiry", choices[Math.floorMod(at + direction, choices.length)]);
		SStationPacket.send(terminal().getBlockPos(), "settings", tag);
	}

	/** Sets the station's callsign (on its radio interfaces): {@code callsign}, or the player's own if empty. */
	private void sendCallsign(String callsign) {
		CompoundTag tag = new CompoundTag();
		tag.putString("callsign", TncPacket.sanitizeCallsign(callsign));
		SStationPacket.send(terminal().getBlockPos(), "callsign", tag);
		editingCallsign = false;
	}

	// -------------------- BOOT --------------------

	private void startBoot() {
		AprsTerminalBlockEntity terminal = terminal();
		StationBus bus = terminal.bus();
		String callsign = terminal.getCallsign();
		long fresh = inbox().stream().filter(this::isNew).count();
		console.clear();
		script.clear();
		consoleLine = "";
		step = stepChars = 0;
		stepAt = System.currentTimeMillis();
		add(StepKind.WAIT, "", 350);
		add(StepKind.BEEP, "", 0);
		add(StepKind.PRINT, "HEATH H-19 VIDEO TERMINAL    ROM V1.4    (C) 1979", 0);
		add(StepKind.PRINT, "MEMORY TEST .......... 64K OK", 0);
		add(StepKind.PRINT, "SERIAL PORT A ........ 1200 BAUD 8N1", 0);
		add(StepKind.SEEK, "", 700);
		add(StepKind.PRINT, "DRIVE A: ............. READY", 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.PROMPT, "", 500);
		add(StepKind.TYPE, "APRS", 0);
		add(StepKind.SEEK, "", 1000);
		add(StepKind.PRINT, "LOADING APRS.COM", 0);
		add(StepKind.SEEK, "", 600);
		add(StepKind.PRINT, "", 0);
		for(String line : ART)
			add(StepKind.FAST, line, 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.PRINT, "      AUTOMATIC PACKET REPORTING SYSTEM   V2.0", 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.PRINT, "TNC .................. " + (bus.hasInterface() ? "FOUND ON PORT A" : "NOT FOUND"), 0);
		add(StepKind.SEEK, "", 300);
		add(StepKind.PRINT, "RADIO INTERFACES ..... " + bus.interfaces().size(), 0);
		add(StepKind.PRINT, "RADIO ................ " + (terminal.isRadioOn() ? frequency() : "OFF"), 0);
		add(StepKind.CALLSIGN, "", 0);
		add(StepKind.PRINT, "MESSAGES ............. " + inbox().size() + (fresh > 0 ? " (" + fresh + " NEW)" : ""), 0);
		add(StepKind.PRINT, "STATIONS HEARD ....... " + terminal.getBeacons().size(), 0);
		add(StepKind.SEEK, "", 400);
		add(StepKind.PRINT, "", 0);
		add(StepKind.BEEP, "", 0);
		add(StepKind.PRINT, "READY.", 0);
		add(StepKind.PROMPT, "", 900);
		page = Page.BOOT;
		pageAt = stepAt;
	}

	private void add(StepKind kind, String text, int millis) {
		script.add(new Step(kind, text, millis));
	}

	/** Runs the boot script up to {@code now}. */
	private void runBoot(long now) {
		while(step < script.size()) {
			Step current = script.get(step);
			long elapsed = now - stepAt;
			int length = current.text().length();
			switch(current.kind()) {
				case PRINT, FAST -> {
					int perChar = current.kind() == StepKind.FAST ? 2 : 6;
					int done = (int)Math.min(length, elapsed / perChar);
					if(done / 5 > stepChars / 5 || (done > 0 && stepChars == 0))
						printTick();
					stepChars = done;
					consoleLine = current.text().substring(0, done);
					if(done < length)
						return;
					console.add(current.text());
					consoleLine = "";
				}
				case TYPE -> {
					int done = (int)Math.min(length, elapsed / 140L);
					if(done > stepChars)
						keyClick();
					stepChars = done;
					consoleLine = "> " + current.text().substring(0, done);
					if(elapsed < length * 140L + 300L)
						return;
					console.add(consoleLine);
					consoleLine = "";
				}
				case PROMPT -> {
					consoleLine = "> ";
					if(elapsed < current.millis())
						return;
					consoleLine = "";
				}
				case WAIT -> {
					if(elapsed < current.millis())
						return;
				}
				case SEEK -> {
					if(now >= nextNoise) {
						seek();
						if(random.nextInt(3) == 0)
							motor();
						nextNoise = now + 30 + random.nextInt(90);
					}
					if(elapsed < current.millis())
						return;
				}
				case BEEP -> beep();
				case CALLSIGN -> { // Decided when it gets here: the callsign may have arrived since the boot began.
					String have = callsign();
					if(!have.isEmpty())
						script.set(step, new Step(StepKind.PRINT, "CALLSIGN ............. " + have, 0));
					else if(!terminal().bus().hasInterface())
						script.set(step, new Step(StepKind.PRINT, "CALLSIGN ............. NOT SET (NO RADIO INTERFACE)", 0));
					else {
						script.set(step, new Step(StepKind.PRINT, "CALLSIGN ............. NOT SET", 0));
						script.add(step + 1, new Step(StepKind.PRINT, "  TYPE A CALLSIGN, OR PRESS ENTER FOR YOUR OWN", 0));
						script.add(step + 2, new Step(StepKind.ASK, "", 0));
						bootInput = "";
						bootAnswered = false;
						beep();
					}
					continue; // Runs the step it became.
				}
				case ASK -> {
					consoleLine = "CALLSIGN> " + bootInput;
					if(!bootAnswered)
						return;
					console.add(consoleLine);
					consoleLine = "";
					sendCallsign(bootInput);
					script.add(step + 1, new Step(StepKind.PRINT, "CALLSIGN ............. " + (bootInput.isEmpty() ? "YOURS" : bootInput) + " SET", 0));
				}
			}
			step++;
			stepAt = now;
			stepChars = 0;
		}
		finishBoot();
	}

	private void finishBoot() {
		SStationPacket.send(terminal().getBlockPos(), "booted");
		switchPage(Page.MAP);
	}

	// -------------------- INPUT --------------------

	private boolean isTyping() {
		return (page == Page.MSG && (msgFocus == Field.TO || msgFocus == Field.TEXT)) || (page == Page.SETUP && (editingComment || editingCallsign));
	}

	private static boolean isEnter(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(page == Page.BOOT) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE)
				return super.keyPressed(keyCode, scanCode, modifiers);
			if(isAsking()) { // The boot is waiting for a callsign.
				keyClick();
				if(keyCode == GLFW.GLFW_KEY_BACKSPACE)
					bootInput = chop(bootInput);
				else if(isEnter(keyCode))
					bootAnswered = true;
				return true;
			}
			if(isEnter(keyCode) || keyCode == GLFW.GLFW_KEY_SPACE)
				finishBoot(); // Skips the rest.
			return true;
		}
		if(keyCode == GLFW.GLFW_KEY_ESCAPE && page == Page.MAP && popup == null)
			return super.keyPressed(keyCode, scanCode, modifiers);
		keyClick();
		if(isTyping())
			typingKey(keyCode);
		else if(popup != null)
			popupKey(keyCode);
		else if(keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + TABS.length)
			switchPage(TABS[keyCode - GLFW.GLFW_KEY_1]);
		else if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			switchPage(page == Page.READ ? Page.INBOX : Page.MAP);
		else if(keyCode == GLFW.GLFW_KEY_S && page != Page.SETUP)
			switchPage(Page.SETUP);
		else switch(page) {
			case INBOX -> inboxKey(keyCode);
			case READ -> readKey(keyCode);
			case MAP -> mapKey(keyCode);
			case LIST -> listKey(keyCode);
			case MSG -> msgKey(keyCode);
			case SETUP -> setupKey(keyCode);
			default -> {}
		}
		return true; // Nothing else: no closing on the inventory key.
	}

	private void typingKey(int keyCode) {
		if(page == Page.SETUP && editingCallsign) {
			switch(keyCode) {
				case GLFW.GLFW_KEY_ESCAPE -> editingCallsign = false;
				case GLFW.GLFW_KEY_BACKSPACE -> callsignDraft = chop(callsignDraft);
				default -> {
					if(isEnter(keyCode)) {
						sendCallsign(callsignDraft);
						setStatus(callsignDraft.isEmpty() ? "CALLSIGN SET TO YOURS" : "CALLSIGN SET TO " + callsignDraft);
					}
				}
			}
			return;
		}
		if(page == Page.SETUP) {
			switch(keyCode) {
				case GLFW.GLFW_KEY_ESCAPE -> editingComment = false;
				case GLFW.GLFW_KEY_BACKSPACE -> commentDraft = chop(commentDraft);
				default -> {
					if(isEnter(keyCode))
						saveComment();
				}
			}
			return;
		}
		switch(keyCode) {
			case GLFW.GLFW_KEY_ESCAPE -> msgFocus = Field.SEND;
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if(msgFocus == Field.TO)
					draftTo = chop(draftTo);
				else
					draftText = chop(draftText);
			}
			case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_DOWN -> msgFocus = Field.values()[msgFocus.ordinal() + 1];
			case GLFW.GLFW_KEY_UP -> msgFocus = Field.TO;
			default -> {
				if(isEnter(keyCode)) {
					if(msgFocus == Field.TO)
						msgFocus = Field.TEXT;
					else
						sendMessage();
				}
			}
		}
	}

	/** True while the boot waits at the CALLSIGN&gt; prompt. */
	private boolean isAsking() {
		return page == Page.BOOT && step < script.size() && script.get(step).kind() == StepKind.ASK;
	}

	private static String chop(String text) {
		return text.isEmpty() ? text : text.substring(0, text.length() - 1);
	}

	private void popupKey(int keyCode) {
		switch(keyCode) {
			case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_BACKSPACE -> popup = null;
			case GLFW.GLFW_KEY_LEFT -> popupOption = Math.max(0, popupOption - 1);
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_TAB -> popupOption = Math.min(2, popupOption + 1);
			default -> {
				if(isEnter(keyCode))
					popupAction(popupOption);
			}
		}
	}

	private void inboxKey(int keyCode) {
		List<Logged> inbox = inbox();
		switch(keyCode) {
			case GLFW.GLFW_KEY_UP -> inboxSel--;
			case GLFW.GLFW_KEY_DOWN -> inboxSel++;
			case GLFW.GLFW_KEY_PAGE_UP -> inboxSel -= LIST_ROWS;
			case GLFW.GLFW_KEY_PAGE_DOWN -> inboxSel += LIST_ROWS;
			case GLFW.GLFW_KEY_HOME -> inboxSel = 0;
			case GLFW.GLFW_KEY_END -> inboxSel = inbox.size() - 1;
			case GLFW.GLFW_KEY_R -> {
				if(!inbox.isEmpty())
					reply(inbox.get(clampSel(inboxSel, inbox.size())).packet().from());
			}
			case GLFW.GLFW_KEY_M -> {
				if(!inbox.isEmpty())
					showOnMap(inbox.get(clampSel(inboxSel, inbox.size())).packet().from());
			}
			default -> {
				if(isEnter(keyCode) && !inbox.isEmpty())
					openMessage(clampSel(inboxSel, inbox.size()));
			}
		}
		inboxSel = clampSel(inboxSel, inbox.size());
	}

	private static int clampSel(int sel, int size) {
		return Mth.clamp(sel, 0, Math.max(0, size - 1));
	}

	private void readKey(int keyCode) {
		List<Logged> inbox = inbox();
		if(inbox.isEmpty()) {
			switchPage(Page.INBOX);
			return;
		}
		String from = inbox.get(clampSel(inboxSel, inbox.size())).packet().from();
		switch(keyCode) {
			case GLFW.GLFW_KEY_LEFT -> readOption = Math.max(0, readOption - 1);
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_TAB -> readOption = Math.min(2, readOption + 1);
			case GLFW.GLFW_KEY_UP -> inboxSel = clampSel(inboxSel - 1, inbox.size());
			case GLFW.GLFW_KEY_DOWN -> inboxSel = clampSel(inboxSel + 1, inbox.size());
			case GLFW.GLFW_KEY_BACKSPACE -> switchPage(Page.INBOX);
			case GLFW.GLFW_KEY_R -> reply(from);
			case GLFW.GLFW_KEY_M -> showOnMap(from);
			default -> {
				if(isEnter(keyCode))
					readAction(readOption, from);
			}
		}
	}

	private void readAction(int option, String from) {
		switch(option) {
			case 0 -> reply(from);
			case 1 -> showOnMap(from);
			default -> switchPage(Page.INBOX);
		}
	}

	private void listKey(int keyCode) {
		List<Heard> stations = terminal().getBeacons();
		switch(keyCode) {
			case GLFW.GLFW_KEY_UP -> listSel--;
			case GLFW.GLFW_KEY_DOWN -> listSel++;
			case GLFW.GLFW_KEY_PAGE_UP -> listSel -= LIST_ROWS;
			case GLFW.GLFW_KEY_PAGE_DOWN -> listSel += LIST_ROWS;
			case GLFW.GLFW_KEY_HOME -> listSel = 0;
			case GLFW.GLFW_KEY_END -> listSel = stations.size() - 1;
			case GLFW.GLFW_KEY_M -> {
				if(!stations.isEmpty())
					showOnMap(stations.get(clampSel(listSel, stations.size())).packet().from());
			}
			default -> {
				if(isEnter(keyCode) && !stations.isEmpty())
					openPopup(stations.get(clampSel(listSel, stations.size())).packet().from());
			}
		}
		listSel = clampSel(listSel, stations.size());
	}

	private void mapKey(int keyCode) {
		if(map == null)
			return;
		double step = map.width() / 8.0D * mapBpp;
		switch(keyCode) {
			case GLFW.GLFW_KEY_LEFT -> mapX -= step;
			case GLFW.GLFW_KEY_RIGHT -> mapX += step;
			case GLFW.GLFW_KEY_UP -> mapZ -= step;
			case GLFW.GLFW_KEY_DOWN -> mapZ += step;
			case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> zoom(-1);
			case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> zoom(1);
			case GLFW.GLFW_KEY_C -> fitMap();
			case GLFW.GLFW_KEY_TAB -> selectNextOnMap();
			default -> {
				if(isEnter(keyCode) && stationNamed(mapSelected) != null)
					openPopup(mapSelected);
			}
		}
	}

	/** Halves ({@code -1}) or doubles ({@code 1}) the blocks each map pixel covers. */
	private void zoom(int direction) {
		mapBpp = Mth.clamp(direction < 0 ? mapBpp / 2.0D : mapBpp * 2.0D, 0.25D, 65536.0D);
	}

	private void selectNextOnMap() {
		List<String> calls = terminal().getBeacons().stream().filter(heard -> heard.packet().position() != null).map(heard -> heard.packet().from()).toList();
		if(calls.isEmpty())
			return;
		mapSelected = calls.get((calls.indexOf(mapSelected) + 1) % calls.size());
		BlockPos at = stationNamed(mapSelected) == null ? null : stationNamed(mapSelected).packet().position();
		if(map != null && at != null) { // Brought into view if it is off the map.
			int px = map.pixelX(at.getX() + 0.5D), pz = map.pixelZ(at.getZ() + 0.5D);
			if(px < 8 || pz < 8 || px >= map.width() - 8 || pz >= map.height() - 8) {
				mapX = at.getX() + 0.5D;
				mapZ = at.getZ() + 0.5D;
			}
		}
	}

	/** Fits home and every station heard on the map, at a power-of-two scale. */
	private void fitMap() {
		BlockPos home = terminal().getBlockPos();
		int minX = home.getX(), maxX = minX, minZ = home.getZ(), maxZ = minZ;
		for(Heard heard : terminal().getBeacons()) {
			BlockPos at = heard.packet().position();
			if(at == null)
				continue;
			minX = Math.min(minX, at.getX());
			maxX = Math.max(maxX, at.getX());
			minZ = Math.min(minZ, at.getZ());
			maxZ = Math.max(maxZ, at.getZ());
		}
		double spanX = Math.max(96.0D, (maxX - minX) * 1.3D + 16.0D), spanZ = Math.max(96.0D, (maxZ - minZ) * 1.3D + 16.0D);
		int width = MAP_AREA[2] * 2, height = MAP_AREA[3] * 2;
		double bpp = 0.25D;
		while((bpp * width < spanX || bpp * height < spanZ) && bpp < 65536.0D)
			bpp *= 2.0D;
		mapBpp = bpp;
		mapX = (minX + maxX) / 2.0D + 0.5D;
		mapZ = (minZ + maxZ) / 2.0D + 0.5D;
		mapFitted = true;
	}

	private void msgKey(int keyCode) {
		switch(keyCode) {
			case GLFW.GLFW_KEY_UP -> msgFocus = msgFocus == Field.CLEAR ? Field.SEND : msgFocus == Field.SEND ? Field.TEXT : Field.TO;
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> msgFocus = msgFocus == Field.SEND ? Field.CLEAR : msgFocus == Field.CLEAR ? Field.TO : Field.SEND;
			case GLFW.GLFW_KEY_LEFT -> msgFocus = msgFocus == Field.CLEAR ? Field.SEND : msgFocus;
			case GLFW.GLFW_KEY_RIGHT -> msgFocus = msgFocus == Field.SEND ? Field.CLEAR : msgFocus;
			default -> {
				if(isEnter(keyCode))
					msgButton(msgFocus);
			}
		}
	}

	private void msgButton(Field field) {
		msgFocus = field;
		if(field == Field.SEND)
			sendMessage();
		else if(field == Field.CLEAR) {
			draftTo = "";
			draftText = "";
			msgFocus = Field.TO;
		}
	}

	private void setupKey(int keyCode) {
		switch(keyCode) {
			case GLFW.GLFW_KEY_UP -> setupSel = Math.max(0, setupSel - 1);
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> setupSel = Math.min(SETUP_ITEMS.length - 1, setupSel + 1);
			case GLFW.GLFW_KEY_LEFT -> {
				if(setupSel == SETUP_SYMBOL)
					setIcon(previous(terminal().getIcon()));
				else if(setupSel == SETUP_EXPIRY)
					setExpiry(-1);
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				if(setupSel == SETUP_SYMBOL)
					setIcon(terminal().getIcon().next());
				else if(setupSel == SETUP_EXPIRY)
					setExpiry(1);
			}
			default -> {
				if(isEnter(keyCode) || keyCode == GLFW.GLFW_KEY_SPACE)
					setupAction(setupSel);
			}
		}
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(isAsking()) {
			bootInput = TncPacket.sanitizeCallsign(bootInput + codePoint);
			return true;
		}
		if(!isTyping() || codePoint < 32 || codePoint >= 127)
			return true;
		if(page == Page.SETUP && editingCallsign)
			callsignDraft = TncPacket.sanitizeCallsign(callsignDraft + codePoint);
		else if(page == Page.SETUP) {
			if(commentDraft.length() < TncPacket.BEACON_TEXT_MAX)
				commentDraft += codePoint;
		}
		else if(msgFocus == Field.TO)
			draftTo = TncPacket.sanitizeCallsign(draftTo + codePoint);
		else if(draftText.length() < TncPacket.MESSAGE_TEXT_MAX)
			draftText += codePoint;
		return true;
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		for(int i = 0; i < SIDE_BUTTONS.length; i++) {
			if(inside(SIDE_BUTTONS[i], mouseX, mouseY)) {
				play(SoundEvents.STONE_BUTTON_CLICK_ON, 1.0F, 0.4F);
				if(page != Page.BOOT)
					switchPage(SIDE_PAGES[i]);
				return true;
			}
		}
		if(page == Page.BOOT)
			return inside(CRT, mouseX, mouseY);
		Spot spot = spotAt(mouseX, mouseY);
		if(spot != null) {
			keyClick();
			spot.action().run();
			return true;
		}
		if(page == Page.MAP && popup == null && map != null && inside(MAP_AREA, mouseX, mouseY)) {
			String clicked = stationAt((mouseX - leftPos - MAP_AREA[0]) * 2.0D, (mouseY - topPos - MAP_AREA[1]) * 2.0D);
			if(clicked != null)
				openPopup(clicked);
			else
				dragging = true;
			return true;
		}
		if(popup != null && inside(CRT, mouseX, mouseY)) {
			popup = null; // A click off the details closes them.
			return true;
		}
		return false;
	}

	/** The station heard nearest map pixel {@code px}, {@code pz}, within a few pixels; null if none. */
	@Nullable
	private String stationAt(double px, double pz) {
		if(map == null)
			return null;
		String best = null;
		double bestDistance = 7.0D;
		for(Heard heard : terminal().getBeacons()) {
			BlockPos at = heard.packet().position();
			if(at == null)
				continue;
			double distance = Math.max(Math.abs(map.pixelX(at.getX() + 0.5D) + 0.5D - px), Math.abs(map.pixelZ(at.getZ() + 0.5D) + 0.5D - pz));
			if(distance < bestDistance) {
				bestDistance = distance;
				best = heard.packet().from();
			}
		}
		return best;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if(dragging && page == Page.MAP) {
			mapX -= dragX / getScale() * 2.0D * mapBpp;
			mapZ -= dragY / getScale() * 2.0D * mapBpp;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		if(!inside(CRT, mouseX, mouseY) || popup != null)
			return false;
		int step = scrollY > 0 ? -1 : 1;
		switch(page) {
			case INBOX -> inboxSel = clampSel(inboxSel + step, inbox().size());
			case READ -> inboxSel = clampSel(inboxSel + step, inbox().size());
			case LIST -> listSel = clampSel(listSel + step, terminal().getBeacons().size());
			case MAP -> zoom(step);
			default -> {
				return false;
			}
		}
		printTick();
		return true;
	}

	@Nullable
	private Spot spotAt(double mouseX, double mouseY) {
		int col = (int)Math.floor((mouseX - leftPos - GRID_X) / CELL_W), row = (int)Math.floor((mouseY - topPos - GRID_Y) / CELL_H);
		for(Spot spot : spots)
			if(spot.row() == row && col >= spot.col() && col < spot.col() + spot.length())
				return spot;
		return null;
	}

	// -------------------- SOUND --------------------

	private void play(SoundEvent sound, float pitch, float volume) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	/** Printing: a soft tick. */
	private void printTick() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.7F + random.nextFloat() * 0.3F, 0.05F);
	}

	private void keyClick() {
		play(SoundEvents.STONE_BUTTON_CLICK_ON, 1.8F + random.nextFloat() * 0.2F, 0.12F);
	}

	/** The drive head stepping. */
	private void seek() {
		play(SoundEvents.COMPARATOR_CLICK, 0.6F + random.nextFloat() * 0.8F, 0.18F);
	}

	/** The drive motor's grind under the seeks. */
	private void motor() {
		play(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.5F, 0.05F);
	}

	/** The terminal's bell. */
	private void beep() {
		play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.6F, 0.25F);
	}

	/** A few seeks, as when a page loads. */
	private void seekBurst(int clicks) {
		long at = System.currentTimeMillis();
		for(int i = 0; i < clicks; i++) {
			at += 30 + random.nextInt(60);
			pending.add(new Pending(at, this::seek));
		}
	}

	/** A packet going out: the modem's tones. */
	private void chirp() {
		long at = System.currentTimeMillis();
		for(int i = 0; i < 10; i++) {
			float pitch = random.nextBoolean() ? 1.2F : 1.9F;
			pending.add(new Pending(at + i * 35L, () -> play(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, 0.12F)));
		}
	}

	private void runPending(long now) {
		List<Pending> due = pending.stream().filter(sound -> sound.at() <= now).toList();
		pending.removeAll(due);
		due.forEach(sound -> sound.play().run());
	}

	// -------------------- COMPOSING THE GRID --------------------

	private void clear() {
		for(int row = 0; row < ROWS; row++) {
			java.util.Arrays.fill(chars[row], ' ');
			java.util.Arrays.fill(attrs[row], NORMAL);
			java.util.Arrays.fill(opaque[row], false);
		}
		spots.clear();
	}

	private void put(int row, int col, String text, byte attr) {
		if(row < 0 || row >= ROWS)
			return;
		for(int i = 0; i < text.length(); i++) {
			int c = col + i;
			if(c < 0 || c >= COLS)
				continue;
			char ch = text.charAt(i);
			chars[row][c] = ch < 32 || ch > 126 ? '?' : ch;
			attrs[row][c] = attr;
		}
	}

	private void spot(int row, int col, String text, byte attr, Runnable action) {
		put(row, col, text, attr);
		spots.add(new Spot(row, col, text.length(), action));
	}

	private void fillRow(int row, byte attr) {
		put(row, 0, " ".repeat(COLS), attr);
	}

	private void cursor(int row, int col, long now) {
		if((now / 400L) % 2L == 0L && row >= 0 && row < ROWS && col >= 0 && col < COLS)
			attrs[row][col] = INVERSE;
	}

	private static String clip(String text, int length) {
		return text.length() <= length ? text : text.substring(0, length);
	}

	private static String pad(String text, int length) {
		return text.length() >= length ? clip(text, length) : text + " ".repeat(length - text.length());
	}

	private static String dots(String label, int length) {
		return label.length() >= length ? label : label + ".".repeat(length - label.length());
	}

	/** Word-wrapped to {@code width} columns; a word longer than that is cut. */
	private static List<String> wrap(String text, int width) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for(String word : text.split(" ")) {
			while(word.length() > width) {
				if(!line.isEmpty()) {
					lines.add(line.toString());
					line.setLength(0);
				}
				lines.add(word.substring(0, width));
				word = word.substring(width);
			}
			if(line.length() + word.length() + (line.isEmpty() ? 0 : 1) > width) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if(!line.isEmpty())
				line.append(' ');
			line.append(word);
		}
		if(!line.isEmpty())
			lines.add(line.toString());
		return lines;
	}

	private String age(long when) {
		long seconds = Math.max(0L, gameTime() - when) / 20L;
		if(seconds < 60L)
			return seconds + "S";
		if(seconds < 3600L)
			return seconds / 60L + "M";
		if(seconds < 86400L)
			return seconds / 3600L + "H";
		return seconds / 86400L + "D";
	}

	private String distance(@Nullable BlockPos at) {
		if(at == null)
			return "-";
		BlockPos here = terminal().getBlockPos();
		double dx = at.getX() - here.getX(), dz = at.getZ() - here.getZ();
		int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
		return distance >= 1000 ? String.format(Locale.ROOT, "%.1fK", distance / 1000.0D) : distance + "M";
	}

	private String bearing(@Nullable BlockPos at) {
		if(at == null)
			return "-";
		BlockPos here = terminal().getBlockPos();
		int dx = at.getX() - here.getX(), dz = at.getZ() - here.getZ();
		if(dx == 0 && dz == 0)
			return "-";
		String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		double degrees = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0D) % 360.0D;
		return points[(int)Math.round(degrees / 45.0D) % 8];
	}

	private static String via(int hops) {
		return hops == 0 ? "DIRECT" : hops == 1 ? "1 DIGI" : hops + " DIGIS";
	}

	/** Fills the grid for this frame, and the clickable spots. */
	private void compose(long now, int mouseX, int mouseY) {
		clear();
		switch(page) {
			case BOOT -> composeBoot(now);
			case INBOX -> composeInbox(now);
			case READ -> composeRead();
			case MAP -> composeMap();
			case LIST -> composeList();
			case MSG -> composeMsg(now);
			case SETUP -> composeSetup(now);
		}
		if(page == Page.BOOT)
			return;
		if(popup != null)
			composePopup();
		if(now < statusUntil) {
			fillRow(STATUS_ROW, NORMAL);
			put(STATUS_ROW, 1, status, INVERSE);
			java.util.Arrays.fill(opaque[STATUS_ROW], true);
		}
		composeHeader();

		// The page draws in from the top, a row at a time.
		int reveal = (int)Math.min(ROWS + 1L, (now - pageAt) / 12L); // Clamped: a long time is all of it, not an overflow.
		if(reveal / 4 > revealed / 4 && revealed < ROWS)
			printTick();
		revealed = Math.max(revealed, reveal);
		for(int row = Math.max(1, reveal); row < ROWS; row++) {
			java.util.Arrays.fill(chars[row], ' ');
			java.util.Arrays.fill(attrs[row], NORMAL);
			java.util.Arrays.fill(opaque[row], false);
		}
		spots.removeIf(spot -> spot.row() > 0 && spot.row() >= reveal);

		Spot hover = spotAt(mouseX, mouseY);
		if(hover != null)
			for(int col = hover.col(); col < hover.col() + hover.length() && col < COLS; col++)
				attrs[hover.row()][col] = attrs[hover.row()][col] == INVERSE ? FAINT : INVERSE;
	}

	private void composeHeader() {
		fillRow(0, INVERSE);
		String callsign = callsign();
		put(0, 1, (callsign.isEmpty() ? "NOCALL" : callsign) + "  " + frequency(), INVERSE);
		int col = 33;
		for(Page tab : TABS) {
			String name = tab.name();
			boolean current = page == tab || (page == Page.READ && tab == Page.INBOX);
			spot(0, col, name, current ? NORMAL : INVERSE, () -> switchPage(tab));
			col += name.length() + 1;
		}
		if(minecraft != null && minecraft.level != null) {
			long day = minecraft.level.getDayTime() % 24000L;
			put(0, 58, String.format(Locale.ROOT, "%02d:%02d", (day / 1000L + 6L) % 24L, day % 1000L * 60L / 1000L), INVERSE);
		}
	}

	private void footer(String help) {
		put(FOOTER_ROW, 0, help, FAINT);
	}

	private void composeBoot(long now) {
		List<String> lines = new ArrayList<>(console);
		lines.add(consoleLine);
		int first = Math.max(0, lines.size() - ROWS);
		for(int i = first; i < lines.size(); i++)
			put(i - first, 0, lines.get(i), NORMAL);
		cursor(lines.size() - 1 - first, consoleLine.length(), now);
	}

	private void composeInbox(long now) {
		List<Logged> inbox = inbox();
		int count = inbox.size();
		long fresh = inbox.stream().filter(this::isNew).count();
		put(1, 1, "INBOX - " + count + (count == 1 ? " MESSAGE" : " MESSAGES") + (fresh > 0 ? ", " + fresh + " NEW" : ""), NORMAL);
		put(2, 1, " #    FROM          AGE  MESSAGE", FAINT);
		put(3, 0, "-".repeat(COLS), FAINT);
		footer(" ^V MOVE  ENTER READ  R REPLY  M MAP  S SETUP  ESC BACK");
		if(count == 0) {
			put(5, 2, "NO MESSAGES. LISTENING", NORMAL);
			cursor(5, 24, now);
			return;
		}
		inboxSel = clampSel(inboxSel, count);
		inboxScroll = Mth.clamp(inboxScroll, Math.max(0, inboxSel - LIST_ROWS + 1), inboxSel);
		for(int i = 0; i < LIST_ROWS && inboxScroll + i < count; i++) {
			int index = inboxScroll + i;
			Logged logged = inbox.get(index);
			String line = String.format(Locale.ROOT, " %02d %s %-12s %5s  %s", index + 1, isNew(logged) ? "*" : " ", logged.packet().from(), age(logged.gameTime()), logged.packet().messageBody());
			spot(LIST_TOP + i, 0, pad(line, COLS - 1), index == inboxSel ? INVERSE : NORMAL, () -> openMessage(index));
		}
		scrollMarks(inboxScroll, count);
	}

	private void scrollMarks(int scroll, int count) {
		if(scroll > 0)
			put(LIST_TOP, COLS - 1, "^", NORMAL);
		if(scroll + LIST_ROWS < count)
			put(LIST_TOP + LIST_ROWS - 1, COLS - 1, "v", NORMAL);
	}

	private void composeRead() {
		List<Logged> inbox = inbox();
		footer(" <> CHOOSE  ENTER OK  ^V PREV/NEXT  R REPLY  M MAP  ESC BACK");
		if(inbox.isEmpty()) {
			put(1, 1, "NO MESSAGE", NORMAL);
			return;
		}
		inboxSel = clampSel(inboxSel, inbox.size());
		Logged logged = inbox.get(inboxSel);
		TncPacket packet = logged.packet();
		put(1, 1, "MESSAGE " + (inboxSel + 1) + " OF " + inbox.size() + (isNew(logged) ? "   [NEW]" : ""), NORMAL);
		put(3, 1, "FROM...: " + packet.from(), NORMAL);
		put(3, 34, "HEARD..: " + age(logged.gameTime()) + " AGO", NORMAL);
		put(4, 1, "TO.....: " + packet.to(), NORMAL);
		put(4, 34, "VIA....: " + via(packet.hops()), NORMAL);
		put(5, 0, "-".repeat(COLS), FAINT);
		List<String> lines = wrap(packet.messageBody(), 60);
		for(int i = 0; i < lines.size() && i < 12; i++)
			put(7 + i, 2, lines.get(i), NORMAL);
		put(20, 0, "-".repeat(COLS), FAINT);
		String[] options = {"[ REPLY ]", "[ SHOW ON MAP ]", "[ BACK ]"};
		int col = 2;
		for(int i = 0; i < options.length; i++) {
			int option = i;
			spot(21, col, options[i], i == readOption ? INVERSE : NORMAL, () -> readAction(option, packet.from()));
			col += options[i].length() + 3;
		}
	}

	private void composeMap() {
		footer(" ARROWS PAN  +/- ZOOM  TAB NEXT  ENTER INFO  C FIT  ESC QUIT");
	}

	private void composeList() {
		List<Heard> stations = terminal().getBeacons();
		int count = stations.size();
		put(1, 1, "STATIONS HEARD - " + count, NORMAL);
		put(2, 0, String.format(Locale.ROOT, " %-11s %-8s %6s %-3s %4s  %-6s %s", "CALL", "SYMBOL", "DIST", "BRG", "AGE", "VIA", "COMMENT"), FAINT);
		put(3, 0, "-".repeat(COLS), FAINT);
		footer(" ^V MOVE  ENTER INFO  M MAP  S SETUP  1-5 PAGE  ESC BACK");
		if(count == 0) {
			put(5, 2, "NO STATIONS HEARD YET", NORMAL);
			return;
		}
		listSel = clampSel(listSel, count);
		listScroll = Mth.clamp(listScroll, Math.max(0, listSel - LIST_ROWS + 1), listSel);
		for(int i = 0; i < LIST_ROWS && listScroll + i < count; i++) {
			int index = listScroll + i;
			Heard heard = stations.get(index);
			TncPacket packet = heard.packet();
			String line = String.format(Locale.ROOT, " %-11s %-8s %6s %-3s %4s  %-6s %s", clip(packet.from(), 11), AprsIcon.byName(packet.icon()).name(),
					distance(packet.position()), bearing(packet.position()), age(heard.gameTime()), packet.hops() == 0 ? "DIRECT" : packet.hops() + " DIGI", packet.text());
			spot(LIST_TOP + i, 0, pad(line, COLS - 1), index == listSel ? INVERSE : NORMAL, () -> {
				listSel = index;
				openPopup(packet.from());
			});
		}
		scrollMarks(listScroll, count);
	}

	/** A station's details in a box over the page. */
	private void composePopup() {
		Heard heard = popup == null ? null : stationNamed(popup);
		if(heard == null) {
			popup = null;
			return;
		}
		spots.clear(); // Only the box answers clicks.
		TncPacket packet = heard.packet();
		int left = 10, right = 53, top = 6, bottom = 16;
		for(int row = top; row <= bottom; row++) {
			put(row, left, "|" + " ".repeat(right - left - 1) + "|", NORMAL);
			for(int col = left; col <= right; col++)
				opaque[row][col] = true;
		}
		String rule = "+" + "-".repeat(right - left - 1) + "+";
		put(top, left, rule, NORMAL);
		put(top, left + (right - left) / 2 - 5, " STATION ", INVERSE);
		put(bottom, left, rule, NORMAL);
		BlockPos at = packet.position();
		put(top + 1, left + 2, "CALL....: " + packet.from(), NORMAL);
		put(top + 2, left + 2, "SYMBOL..: " + AprsIcon.byName(packet.icon()).name(), NORMAL);
		put(top + 3, left + 2, "POSITION: " + (at == null ? "UNKNOWN" : at.getX() + " " + at.getY() + " " + at.getZ()), NORMAL);
		put(top + 4, left + 2, "RANGE...: " + (at == null ? "-" : distance(at) + " " + bearing(at)), NORMAL);
		put(top + 5, left + 2, "HEARD...: " + age(heard.gameTime()) + " AGO, " + via(packet.hops()), NORMAL);
		put(top + 6, left + 2, "COMMENT.: " + clip(packet.text(), right - left - 14), NORMAL);
		String[] options = {"[ MESSAGE ]", "[ MAP ]", "[ CLOSE ]"};
		int col = left + 2;
		for(int i = 0; i < options.length; i++) {
			int option = i;
			spot(top + 8, col, options[i], i == popupOption ? INVERSE : NORMAL, () -> popupAction(option));
			col += options[i].length() + 2;
		}
		footer(pad(" <> CHOOSE  ENTER OK  ESC CLOSE", COLS));
	}

	private void composeMsg(long now) {
		String callsign = callsign();
		put(1, 1, "SEND MESSAGE" + (callsign.isEmpty() ? "   (NO CALLSIGN: SET ONE ON THE RADIO INTERFACE)" : "   FROM " + callsign), NORMAL);
		put(3, 1, "TO.....: ", NORMAL);
		put(3, 10, ".".repeat(TncPacket.CALLSIGN_MAX), FAINT);
		put(3, 10, draftTo, NORMAL);
		spots.add(new Spot(3, 10, TncPacket.CALLSIGN_MAX, () -> msgFocus = Field.TO));
		if(msgFocus == Field.TO)
			cursor(3, 10 + draftTo.length(), now);

		int width = 50, lines = 4;
		put(5, 1, "TEXT...: ", NORMAL);
		for(int i = 0; i < lines; i++) {
			put(5 + i, 10, ".".repeat(width), FAINT);
			spots.add(new Spot(5 + i, 10, width, () -> msgFocus = Field.TEXT));
			int from = i * width;
			if(from < draftText.length())
				put(5 + i, 10, draftText.substring(from, Math.min(draftText.length(), from + width)), NORMAL);
		}
		if(msgFocus == Field.TEXT)
			cursor(5 + Math.min(lines - 1, draftText.length() / width), 10 + (draftText.length() >= lines * width ? width - 1 : draftText.length() % width), now);
		put(9, 10 + width - 7, String.format(Locale.ROOT, "%3d/%d", draftText.length(), TncPacket.MESSAGE_TEXT_MAX), FAINT);

		spot(11, 10, "[ SEND ]", msgFocus == Field.SEND ? INVERSE : NORMAL, () -> msgButton(Field.SEND));
		spot(11, 21, "[ CLEAR ]", msgFocus == Field.CLEAR ? INVERSE : NORMAL, () -> msgButton(Field.CLEAR));

		put(13, 0, "-- SENT " + "-".repeat(COLS - 8), FAINT);
		List<Logged> sent = sent();
		if(sent.isEmpty())
			put(14, 2, "NOTHING SENT YET", FAINT);
		for(int i = 0; i < sent.size() && i < 9; i++) {
			Logged logged = sent.get(i);
			String delivery = switch(logged.delivery()) {
				case SENDING -> "SENDING";
				case RETRYING -> "RETRYING " + logged.retries() + "/" + RadiocraftServerConfig.APRS_MESSAGE_RETRIES.get();
				case SENT -> "SENT";
				case FAILED -> "FAILED";
			};
			put(14 + i, 0, clip(String.format(Locale.ROOT, " %5s  > %-11s %-13s %s", age(logged.gameTime()), logged.packet().to(), delivery, logged.packet().messageBody()), COLS),
					logged.delivery() == AprsTerminalBlockEntity.Delivery.FAILED ? FAINT : NORMAL);
		}
		footer(isTyping() ? " TAB NEXT FIELD  ENTER NEXT/SEND  ESC LEAVE FIELD" : " ^V FIELD  ENTER SELECT  1-5 PAGE  ESC BACK");
	}

	private void composeSetup(long now) {
		AprsTerminalBlockEntity terminal = terminal();
		String callsign = callsign();
		put(1, 1, "SETUP", NORMAL);
		for(int i = 0; i < SETUP_ITEMS.length; i++) {
			int item = i;
			String value = switch(i) {
				case 0 -> onOff(terminal.isBeaconEnabled());
				case SETUP_CALLSIGN -> editingCallsign ? "[" + callsignDraft + "]" : callsign.isEmpty() ? "NOT SET" : callsign;
				case SETUP_SYMBOL -> "< " + terminal.getIcon().name() + " >";
				case SETUP_COMMENT -> editingComment ? "[" + commentDraft + "]" : "\"" + terminal.getBeaconComment() + "\"";
				case 5 -> onOff(terminal.isContactBuzzer());
				case 6 -> onOff(terminal.isMessageBuzzer());
				case 7 -> onOff(terminal.isDigipeatEnabled());
				case SETUP_EXPIRY -> "< " + (terminal.getStationExpiryMinutes() == 0 ? "NEVER" : terminal.getStationExpiryMinutes() + " MIN") + " >";
				default -> "";
			};
			String line = value.isEmpty() ? SETUP_ITEMS[i] : dots(SETUP_ITEMS[i], SETUP_LABEL) + ": " + value;
			spot(SETUP_TOP + i * 2, 1, line, i == setupSel && !isTyping() ? INVERSE : NORMAL, () -> setupAction(item));
		}
		if(editingComment)
			cursor(SETUP_TOP + SETUP_COMMENT * 2, 1 + SETUP_LABEL + 3 + commentDraft.length(), now);
		if(editingCallsign)
			cursor(SETUP_TOP + SETUP_CALLSIGN * 2, 1 + SETUP_LABEL + 3 + callsignDraft.length(), now);
		footer(editingComment ? " TYPE THE COMMENT  ENTER SAVE  ESC CANCEL"
				: editingCallsign ? " TYPE A CALLSIGN (BLANK FOR YOUR OWN)  ENTER SET  ESC CANCEL"
				: " ^V MOVE  ENTER CHANGE  <> CHOOSE  1-5 PAGE  ESC BACK");
	}

	private static String onOff(boolean on) {
		return on ? "ON" : "OFF";
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		runPending(now);
		if(page == Page.BOOT)
			runBoot(now);
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		compose(now, mouseX, mouseY);
		if(page == Page.MAP)
			renderMap(guiGraphics);
		renderGrid(guiGraphics);
		renderGlass(guiGraphics);
		renderSideButtons(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// All drawn on the screen in renderBg.
	}

	private void renderMap(GuiGraphics guiGraphics) {
		if(minecraft == null || minecraft.level == null)
			return;
		if(map == null)
			map = new H19Map(MAP_AREA[2] * 2, MAP_AREA[3] * 2);
		if(!mapFitted)
			fitMap();
		map.view(mapX, mapZ, mapBpp);
		List<H19Map.Mark> marks = new ArrayList<>();
		AprsTerminalBlockEntity terminal = terminal();
		marks.add(new H19Map.Mark(callsign().isEmpty() ? "HOME" : callsign(), terminal.getBlockPos(), terminal.getIcon(), true, false));
		for(Heard heard : terminal.getBeacons())
			if(heard.packet().position() != null)
				marks.add(new H19Map.Mark(heard.packet().from(), heard.packet().position(), AprsIcon.byName(heard.packet().icon()), false, heard.packet().from().equals(mapSelected)));
		if(revealed < ROWS)
			return;
		if(map.update(minecraft.level, marks) && random.nextInt(4) == 0)
			seek();
		guiGraphics.blit(H19Map.LOCATION, leftPos + MAP_AREA[0], topPos + MAP_AREA[1], MAP_AREA[2], MAP_AREA[3], 0.0F, 0.0F, map.width(), map.height(), map.width(), map.height());
	}

	/** Lit cells and the black under text over the map, then the characters, each centred in its cell. */
	private void renderGrid(GuiGraphics guiGraphics) {
		for(int row = 0; row < ROWS; row++) {
			int y = topPos + GRID_Y + row * CELL_H;
			int col = 0;
			while(col < COLS) {
				int colour = attrs[row][col] == INVERSE ? GREEN : opaque[row][col] ? H19Map.BLACK : 0;
				int end = col + 1;
				while(end < COLS && (attrs[row][end] == INVERSE ? GREEN : opaque[row][end] ? H19Map.BLACK : 0) == colour)
					end++;
				if(colour != 0)
					guiGraphics.fill(leftPos + GRID_X + col * CELL_W, y, leftPos + GRID_X + end * CELL_W, y + CELL_H, colour);
				col = end;
			}
		}
		guiGraphics.pose().pushPose(); // All the characters in one batch, flushed once.
		guiGraphics.pose().translate(leftPos + GRID_X, topPos + GRID_Y, 0.0F);
		guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		Matrix4f pose = guiGraphics.pose().last().pose();
		int cellW = (int)(CELL_W / TEXT_SCALE), cellH = (int)(CELL_H / TEXT_SCALE);
		for(int row = 0; row < ROWS; row++) {
			for(int col = 0; col < COLS; col++) {
				char c = chars[row][col];
				if(c == ' ')
					continue;
				String text = String.valueOf(c);
				int colour = attrs[row][col] == INVERSE ? INK : attrs[row][col] == FAINT ? DIM_GREEN : GREEN;
				font.drawInBatch(text, col * cellW + (cellW - font.width(text) + 1) / 2.0F, row * cellH + 1, colour, false, pose,
						guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			}
		}
		guiGraphics.flush();
		guiGraphics.pose().popPose();
	}

	/** The glass: a green cast, scanlines, and now and then a flicker. */
	private void renderGlass(GuiGraphics guiGraphics) {
		int x0 = leftPos + CRT[0], y0 = topPos + CRT[1];
		guiGraphics.fill(x0 + 1, y0 + 1, x0 + CRT[2] - 1, y0 + CRT[3] - 1, 0x0A39FF14);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x0, y0, 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		for(int y = 2; y < CRT[3] * 2 - 2; y += 2)
			guiGraphics.fill(2, y, CRT[2] * 2 - 2, y + 1, 0x28000000);
		guiGraphics.pose().popPose();
		if(random.nextInt(90) == 0)
			guiGraphics.fill(x0 + 1, y0 + 1, x0 + CRT[2] - 1, y0 + CRT[3] - 1, 0x14000000);
	}

	/** The side panel's buttons: pressed while hovered or showing their page, flashing red for something new. */
	private void renderSideButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		long now = gameTime();
		if(page == Page.INBOX || page == Page.READ)
			inboxViewedAt = now;
		if(page == Page.MAP || page == Page.LIST)
			stationsViewedAt = now;
		boolean newMessage = inbox().stream().anyMatch(logged -> logged.gameTime() > inboxViewedAt);
		boolean newStation = terminal().getBeacons().stream().anyMatch(heard -> heard.firstHeard() > stationsViewedAt);
		boolean flash = (System.currentTimeMillis() / 400L) % 2L == 0L;
		for(int i = 0; i < SIDE_BUTTONS.length; i++) {
			int[] r = SIDE_BUTTONS[i];
			Page target = SIDE_PAGES[i];
			boolean showing = page == target || (target == Page.INBOX && page == Page.READ);
			boolean news = target == Page.INBOX ? newMessage : (target == Page.MAP || target == Page.LIST) && newStation;
			int u = inside(r, mouseX, mouseY) || showing ? BUTTON_PRESSED : news && flash ? BUTTON_LIT : BUTTON_NORMAL;
			guiGraphics.blit(BUTTONS, leftPos + r[0], topPos + r[1], u, BUTTON_ROWS[i], r[2], r[3], BUTTONS_WIDTH, BUTTONS_HEIGHT);
		}
	}
}

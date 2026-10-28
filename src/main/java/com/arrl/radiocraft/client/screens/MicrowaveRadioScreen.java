package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.DishInfo;
import com.arrl.radiocraft.common.blockentities.MicrowaveRadioBlockEntity.Phase;
import com.arrl.radiocraft.common.menus.MicrowaveRadioMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveMath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The microwave radio's screen: an industrial terminal, green on black with a larger font than the H19's, behind the
 * radio's beige rack front. POWER, STATUS and SETUP are the cream buttons; the FREQ knob tunes (mouse wheel).
 * <ul>
 *     <li>STATUS: this station, the link (with a spinner and a progress bar while searching), the far end or the
 *     subscribers, interference, and the actions: BROADCAST MASTER LINK, SEARCH FOR MASTER LINK, DISCONNECT. A master
 *     found is shown with both callsigns for confirmation.</li>
 *     <li>SETUP: callsign, 4 digit hex ID, frequency (5925-7125 MHz in 10 MHz steps), and every dish on the coax, turned
 *     and tilted a degree at a time (arrows; SHIFT for 10).</li>
 * </ul>
 */
public class MicrowaveRadioScreen extends StationPanelScreen<MicrowaveRadioMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/microwave_radio.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/microwave_radio_widgets.png");
	private static final int TEXTURE_WIDTH = 300, TEXTURE_HEIGHT = 200;
	private static final int[] CRT = {26, 14, 200, 144};
	private static final int[] LAMP = {248, 18, 12, 12};
	private static final int[][] BUTTONS = {{234, 44, 40, 16}, {234, 64, 40, 16}, {234, 84, 40, 16}}; // POWER, STATUS, SETUP
	private static final int[] KNOB = {240, 110, 28, 28};

	private static final int COLS = 39, ROWS = 17, CELL_W = 5, CELL_H = 8;
	private static final float TEXT_SCALE = 0.75F;
	private static final int GRID_X = CRT[0] + (CRT[2] - COLS * CELL_W) / 2, GRID_Y = CRT[1] + (CRT[3] - ROWS * CELL_H) / 2;

	private static final int GREEN = 0xFF40FF70;
	private static final int DIM = 0xFF1C8A3C;
	private static final int INK = 0xFF021A08;
	private static final byte NORMAL = 0, LOW = 1, INVERSE = 2;
	private static final String SPINNER = "|/-\\";
	/** The maintainer's banner, shown as the radio boots. */
	private static final String[] ART = {
			"  ________  _   ,",
			"    /  /  )' \\ /",
			" --/  /  /    X",
			"(_/  /__/_   / \\_"
	};

	private enum Page { OFF, BOOT, STATUS, SETUP, ALIGN }
	private enum Field { NONE, CALLSIGN, ID, FREQUENCY }

	private final char[][] chars = new char[ROWS][COLS];
	private final byte[][] attrs = new byte[ROWS][COLS];
	private record Spot(int row, int col, int length, Runnable action) {}
	private final List<Spot> spots = new ArrayList<>();
	private final RandomSource random = RandomSource.create();

	private Page page;
	private long pageAt = System.currentTimeMillis();
	private long bootAt = 0L;
	private Field field = Field.NONE;
	private String typed = "";
	private int dishSel = 0;
	private boolean wasOn;
	private Phase lastPhase = null;

	public MicrowaveRadioScreen(MicrowaveRadioMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_WIDTH;
		this.imageHeight = TEXTURE_HEIGHT;
		wasOn = radio().isPowered();
		page = wasOn ? Page.STATUS : Page.OFF;
	}

	private MicrowaveRadioBlockEntity radio() {
		return menu.blockEntity;
	}

	private void send(String action) {
		send(action, new CompoundTag());
	}

	private void send(String action, CompoundTag data) {
		SStationPacket.send(radio().getBlockPos(), action, data);
	}

	private void switchPage(Page to) {
		if(page == to || page == Page.OFF || page == Page.BOOT)
			return;
		page = to;
		pageAt = System.currentTimeMillis();
		field = Field.NONE;
	}

	// -------------------- INPUT --------------------

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_ESCAPE && field == Field.NONE)
			return super.keyPressed(keyCode, scanCode, modifiers);
		if(page == Page.OFF || page == Page.BOOT)
			return super.keyPressed(keyCode, scanCode, modifiers);
		click();
		if(field != Field.NONE) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE)
				field = Field.NONE;
			else if(keyCode == GLFW.GLFW_KEY_BACKSPACE)
				typed = typed.isEmpty() ? typed : typed.substring(0, typed.length() - 1);
			else if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
				enterField();
			return true;
		}
		boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
		int step = shift ? 10 : 1;
		if(keyCode == GLFW.GLFW_KEY_TAB)
			switchPage(page == Page.STATUS ? Page.SETUP : page == Page.SETUP ? Page.ALIGN : Page.STATUS);
		else if(keyCode == GLFW.GLFW_KEY_A && page != Page.ALIGN)
			switchPage(Page.ALIGN);
		else if(radio().getPhase() == Phase.FOUND && keyCode == GLFW.GLFW_KEY_Y)
			send("confirm");
		else if(radio().getPhase() == Phase.FOUND && keyCode == GLFW.GLFW_KEY_N)
			send("reject");
		else if(page == Page.STATUS) {
			switch(keyCode) {
				case GLFW.GLFW_KEY_B -> send("broadcast");
				case GLFW.GLFW_KEY_S -> send("search");
				case GLFW.GLFW_KEY_D -> send("disconnect");
				default -> {}
			}
		}
		else if(page == Page.ALIGN) {
			int count = radio().getDishInfo().size();
			double fine = shift ? ALIGN_COARSE : ALIGN_FINE;
			switch(keyCode) {
				case GLFW.GLFW_KEY_RIGHT -> dish(fine, 0);
				case GLFW.GLFW_KEY_LEFT -> dish(-fine, 0);
				case GLFW.GLFW_KEY_UP -> dish(0, fine);
				case GLFW.GLFW_KEY_DOWN -> dish(0, -fine);
				case GLFW.GLFW_KEY_N -> dishSel = count == 0 ? 0 : (dishSel + 1) % count;
				default -> {}
			}
		}
		else if(page == Page.SETUP) {
			int count = radio().getDishInfo().size();
			switch(keyCode) {
				case GLFW.GLFW_KEY_C -> startField(Field.CALLSIGN);
				case GLFW.GLFW_KEY_I -> startField(Field.ID);
				case GLFW.GLFW_KEY_F -> startField(Field.FREQUENCY);
				case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_EQUAL -> tune(step);
				case GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_MINUS -> tune(-step);
				case GLFW.GLFW_KEY_RIGHT -> dish(step, 0);
				case GLFW.GLFW_KEY_LEFT -> dish(-step, 0);
				case GLFW.GLFW_KEY_UP -> dish(0, step);
				case GLFW.GLFW_KEY_DOWN -> dish(0, -step);
				case GLFW.GLFW_KEY_N -> dishSel = count == 0 ? 0 : (dishSel + 1) % count;
				default -> {}
			}
		}
		return true;
	}

	private void tune(int steps) {
		CompoundTag data = new CompoundTag();
		data.putInt("steps", steps);
		send("tune", data);
	}

	private void dish(double azimuth, double elevation) {
		if(radio().getDishInfo().isEmpty())
			return;
		CompoundTag data = new CompoundTag();
		data.putInt("index", Math.min(dishSel, radio().getDishInfo().size() - 1));
		data.putDouble("azimuth", azimuth);
		data.putDouble("elevation", elevation);
		send("dish", data);
	}

	private void startField(Field to) {
		field = to;
		typed = "";
	}

	private void enterField() {
		MicrowaveRadioBlockEntity radio = radio();
		try {
			switch(field) {
				case CALLSIGN -> {
					if(typed.isEmpty())
						return;
					CompoundTag data = new CompoundTag();
					data.putString("callsign", typed);
					data.putInt("id", radio.getHexId());
					send("identity", data);
				}
				case ID -> {
					if(radio.getCallsign().isEmpty()) {
						field = Field.CALLSIGN; // A callsign first.
						typed = "";
						return;
					}
					CompoundTag data = new CompoundTag();
					data.putString("callsign", radio.getCallsign());
					data.putInt("id", Integer.parseInt(typed, 16));
					send("identity", data);
				}
				case FREQUENCY -> {
					CompoundTag data = new CompoundTag();
					data.putInt("mhz", Integer.parseInt(typed));
					send("frequency", data);
				}
				default -> {}
			}
		}
		catch(NumberFormatException e) {
			beep();
		}
		field = Field.NONE;
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		if(field == Field.NONE)
			return true;
		char upper = Character.toUpperCase(c);
		boolean ok = switch(field) {
			case CALLSIGN -> typed.length() < 10 && (Character.isLetterOrDigit(upper) || upper == '/') && upper < 128;
			case ID -> typed.length() < 4 && "0123456789ABCDEF".indexOf(upper) >= 0;
			case FREQUENCY -> typed.length() < 4 && Character.isDigit(upper);
			default -> false;
		};
		if(ok)
			typed += upper;
		return true;
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		for(int i = 0; i < BUTTONS.length; i++) {
			if(inside(BUTTONS[i], mouseX, mouseY)) {
				if(i == 0) {
					tones(0.12F, 0.9F, 1.35F); // Power: a low two-tone.
					send("power");
				}
				else {
					tones(0.1F, i == 1 ? 1.5F : 1.2F, i == 1 ? 1.2F : 1.5F); // STATUS falls, SETUP rises.
					switchPage(i == 1 ? Page.STATUS : Page.SETUP);
				}
				return true;
			}
		}
		if(page == Page.OFF || page == Page.BOOT)
			return inside(CRT, mouseX, mouseY);
		Spot spot = spotAt(mouseX, mouseY);
		if(spot != null) {
			click();
			spot.action().run();
			return true;
		}
		return false;
	}

	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		if(!inside(KNOB, mouseX, mouseY) || page == Page.OFF)
			return false;
		tune(scrollY > 0 ? 1 : -1);
		click();
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

	private void play(SoundEvent sound, float pitch, float volume) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	/** Boot lines printed, and how far the page drawing in has ticked, for the typing sounds. */
	private int bootShown = 0, revealTicked = 0;
	private long revealPageAt = -1L;

	/** The screen printing: the H19's soft tick. */
	private void typeTick() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.7F + random.nextFloat() * 0.3F, 0.05F);
	}

	/** A key or the knob: a soft tick. */
	private void click() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.75F + random.nextFloat() * 0.2F, 0.07F);
	}

	/** Quick beeps at these pitches, one after another. */
	private void tones(float volume, float... pitches) {
		long at = System.currentTimeMillis();
		for(int i = 0; i < pitches.length; i++) {
			float pitch = pitches[i];
			later(at + i * 80L, () -> play(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, volume));
		}
	}

	private record Later(long at, Runnable sound) {}

	private final java.util.List<Later> later = new java.util.ArrayList<>();

	private void later(long at, Runnable sound) {
		later.add(new Later(at, sound));
	}

	private void playLater(long now) {
		java.util.List<Later> due = later.stream().filter(sound -> sound.at() <= now).toList();
		later.removeAll(due);
		due.forEach(sound -> sound.sound().run());
	}

	/** When the last status blip sounded, and where in its rising sweep the search is. */
	private long lastBlip;
	private int sweepStep;

	/** Status blips: a sweep rising in pitch while searching, a slow low pulse while broadcasting. */
	private void blips(long now, Phase phase) {
		if(phase == Phase.SEARCHING && now - lastBlip > 260L) {
			lastBlip = now;
			play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.8F + (sweepStep++ % 8) * 0.12F, 0.05F);
		}
		else if(phase == Phase.BROADCASTING && now - lastBlip > 2000L) {
			lastBlip = now;
			play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.7F, 0.06F);
		}
		else if(phase != Phase.SEARCHING)
			sweepStep = 0;
	}

	/** The terminal's fan and transformer. */
	@Nullable
	private com.arrl.radiocraft.client.screens.TerminalWhirr whirr;

	@Override
	protected void init() {
		super.init();
		if(whirr == null)
			whirr = TerminalWhirr.start(0.15F);
	}

	@Override
	public void removed() {
		super.removed();
		if(whirr != null) {
			whirr.finish();
			whirr = null;
		}
	}

	private void beep() {
		play(SoundEvents.NOTE_BLOCK_BIT.value(), 0.8F, 0.25F);
	}

	// -------------------- GRID --------------------

	private void clear() {
		for(int row = 0; row < ROWS; row++) {
			Arrays.fill(chars[row], ' ');
			Arrays.fill(attrs[row], NORMAL);
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

	private static String dots(String label, int length) {
		return label.length() >= length ? label : label + ".".repeat(length - label.length());
	}

	private void compose(long now, int mouseX, int mouseY) {
		clear();
		MicrowaveRadioBlockEntity radio = radio();
		if(page == Page.BOOT) {
			composeBoot(now);
			return;
		}
		put(0, 0, " ".repeat(COLS), INVERSE);
		put(0, 1, "MW-6 " + radio.identity(), INVERSE);
		put(0, COLS - 10, String.format(Locale.ROOT, "%4d MHZ", radio.getFrequency()), INVERSE);
		if(page == Page.STATUS)
			composeStatus(now);
		else if(page == Page.ALIGN)
			composeAlign();
		else
			composeSetup(now);
		if(radio.getPhase() == Phase.FOUND)
			composeFound();
		if(field != Field.NONE)
			composeField(now);
		int reveal = (int)Math.min(ROWS + 1L, (now - pageAt) / 15L); // Draws in from the top.
		if(pageAt != revealPageAt) {
			revealPageAt = pageAt;
			revealTicked = 0;
		}
		if(reveal <= ROWS && reveal >= revealTicked + 3) { // A tick every few rows as the page draws in.
			revealTicked = reveal;
			typeTick();
		}
		for(int row = Math.max(1, reveal); row < ROWS; row++) {
			Arrays.fill(chars[row], ' ');
			Arrays.fill(attrs[row], NORMAL);
		}
		spots.removeIf(spot -> spot.row() > 0 && spot.row() >= reveal);
		Spot hover = spotAt(mouseX, mouseY);
		if(hover != null)
			for(int col = hover.col(); col < hover.col() + hover.length() && col < COLS; col++)
				attrs[hover.row()][col] = attrs[hover.row()][col] == INVERSE ? LOW : INVERSE;
	}

	private void composeBoot(long now) {
		long t = now - bootAt;
		List<String> lines = new ArrayList<>();
		for(String line : ART)
			lines.add("  " + line);
		lines.addAll(List.of("", "MW-6 DIGITAL MICROWAVE RADIO  FW 3.1", "SYNTHESIZER ...... LOCKED", "MODEM ............ OK",
				"DISHES ON COAX ... " + radio().getDishInfo().size(), "IDENTITY ......... " + radio().identity(), "", "READY"));
		int shown = (int)Math.min(lines.size(), t / 220L);
		if(shown > bootShown) { // Each line as it prints, and a beep for READY.
			bootShown = shown;
			if(shown == lines.size())
				beep();
			else
				typeTick();
		}
		for(int i = 0; i < shown; i++)
			put(1 + i, 1, lines.get(i), NORMAL);
		if(t > lines.size() * 220L + 500L) {
			page = Page.STATUS;
			pageAt = now;
		}
	}

	private void composeStatus(long now) {
		MicrowaveRadioBlockEntity radio = radio();
		int label = 14;
		put(2, 1, dots("STATION ", label) + " " + radio.identity(), NORMAL);
		String role = switch(radio.getPhase()) {
			case BROADCASTING -> "MASTER";
			case SEARCHING, FOUND, LINKED, LOST, NOT_FOUND -> "SUBSCRIBER";
			default -> "NONE";
		};
		put(3, 1, dots("ROLE ", label) + " " + role, NORMAL);
		String link = switch(radio.getPhase()) {
			case IDLE -> "IDLE";
			case BROADCASTING -> radio.getSubscribers().isEmpty() ? "BROADCASTING" : "UP, " + radio.getSubscribers().size() + " SUBSCRIBER" + (radio.getSubscribers().size() == 1 ? "" : "S");
			case SEARCHING -> "SEARCHING " + SPINNER.charAt((int)((now / 150L) % 4L));
			case FOUND -> "MASTER FOUND";
			case LINKED -> "UP";
			case NOT_FOUND -> "NO MASTER FOUND";
			case LOST -> "LOST, RETRYING";
		};
		put(4, 1, dots("LINK ", label) + " " + link, radio.isLinked() ? INVERSE : NORMAL);
		if(radio.getPhase() == Phase.SEARCHING) {
			int width = 24, done = Math.round(radio.getSearchProgress() * width);
			put(5, 3, "[" + "#".repeat(done) + ".".repeat(width - done) + "] " + Math.round(radio.getSearchProgress() * 100) + "%", NORMAL);
			put(6, 3, rssiLine(radio), LOW);
		}
		else if(radio.getPhase() == Phase.BROADCASTING && radio.getSubscribers().isEmpty())
			put(5, 3, "WAITING FOR SUBSCRIBERS " + ".".repeat((int)((now / 400L) % 4L)), LOW);
		int row = 7;
		if(radio.getPhase() == Phase.LINKED || radio.getPhase() == Phase.LOST)
			put(row++, 1, dots("MASTER ", label) + " " + radio.getMasterName(), NORMAL);
		List<String> subs = radio.getSubscribers();
		for(int i = 0; i < subs.size() && i < 3; i++)
			put(row++, 1, dots(i == 0 ? "SUBSCRIBERS " : "", label) + " " + subs.get(i), NORMAL);
		if(radio.isLinked())
			put(row++, 1, dots("QUALITY ", label) + " " + (radio.hasInterference() ? "INTERFERENCE" : "CLEAN"), radio.hasInterference() ? INVERSE : NORMAL);
		if(!radio.getProblem().isEmpty())
			put(row++, 1, dots("FAULT ", label) + " " + radio.getProblem(), INVERSE);
		if(radio.getCallsign().isEmpty())
			put(row, 1, "SET A CALLSIGN IN SETUP FIRST", INVERSE);
		spot(13, 1, "[B] BROADCAST MASTER LINK", NORMAL, () -> send("broadcast"));
		spot(14, 1, "[S] SEARCH FOR MASTER LINK", NORMAL, () -> send("search"));
		spot(15, 1, "[D] DISCONNECT", NORMAL, () -> send("disconnect"));
		spot(15, 20, "[A] ALIGN DISHES", NORMAL, () -> switchPage(Page.ALIGN));
		put(16, 0, " TAB SETUP  ESC QUIT", LOW);
	}

	/** The signal the search hears, as the meter reads it: dBm, a short bar, and whether it decodes. */
	private static String rssiLine(MicrowaveRadioBlockEntity radio) {
		double rssi = radio.getRssi();
		if(rssi <= MicrowaveMath.NO_SIGNAL_DBM + 0.5D)
			return "RSSI ----.- DBM  NO SIGNAL";
		int bars = (int)Math.round(Math.max(0.0D, Math.min(1.0D, (rssi - GRAPH_LOW) / (GRAPH_HIGH - GRAPH_LOW))) * 8);
		return String.format(Locale.ROOT, "RSSI %6.1f DBM %s %s", rssi, "|".repeat(bars) + " ".repeat(8 - bars), MicrowaveMath.grade(rssi).label);
	}

	private void composeFound() {
		MicrowaveRadioBlockEntity radio = radio();
		int top = 5;
		for(int row = top; row < top + 7; row++)
			put(row, 3, " ".repeat(COLS - 6), INVERSE);
		put(top + 1, 5, "MASTER LINK FOUND", INVERSE);
		put(top + 2, 5, "FAR END ... " + radio.getCandidateName(), INVERSE);
		put(top + 3, 5, "THIS END .. " + radio.identity(), INVERSE);
		put(top + 4, 5, "CONFIRM LINK?", INVERSE);
		spot(top + 5, 5, "[Y] YES", NORMAL, () -> send("confirm"));
		spot(top + 5, 15, "[N] NO, KEEP LOOKING", NORMAL, () -> send("reject"));
	}

	private void composeSetup(long now) {
		MicrowaveRadioBlockEntity radio = radio();
		int label = 14;
		spot(2, 1, dots("CALLSIGN ", label) + " " + (radio.getCallsign().isEmpty() ? "(NONE)" : radio.getCallsign()) + "  [C]", NORMAL, () -> startField(Field.CALLSIGN));
		spot(3, 1, dots("ID (HEX) ", label) + " " + MicrowaveMath.hex(radio.getHexId()) + "  [I]", NORMAL, () -> startField(Field.ID));
		put(4, 1, dots("FREQUENCY ", label) + " ", NORMAL);
		spot(4, label + 2, "<", NORMAL, () -> tune(-1));
		spot(4, label + 4, radio.getFrequency() + " MHZ", NORMAL, () -> startField(Field.FREQUENCY));
		spot(4, label + 13, ">", NORMAL, () -> tune(1));
		put(5, 3, MicrowaveMath.MIN_MHZ + "-" + MicrowaveMath.MAX_MHZ + " MHZ, " + MicrowaveMath.STEP_MHZ + " MHZ STEPS  [F]", LOW);
		put(7, 1, "DISHES ON COAX", NORMAL);
		List<DishInfo> dishes = radio.getDishInfo();
		if(dishes.isEmpty())
			put(8, 3, "NONE: RUN COAX TO A DISH OR HORN", LOW);
		for(int i = 0; i < dishes.size() && i < 4; i++) {
			DishInfo dish = dishes.get(i);
			int index = i;
			String text = String.format(Locale.ROOT, "%d %s AZ %05.1f EL %+05.1f", i + 1, dish.horn() ? "HORN" : "DISH", dish.azimuth(), dish.elevation());
			spot(8 + i, 2, text, i == dishSel ? INVERSE : NORMAL, () -> dishSel = index);
		}
		if(!dishes.isEmpty()) {
			put(13, 1, "TURN:", LOW);
			spot(13, 7, "<", NORMAL, () -> dish(-1, 0));
			spot(13, 9, ">", NORMAL, () -> dish(1, 0));
			put(13, 12, "TILT:", LOW);
			spot(13, 18, "v", NORMAL, () -> dish(0, -1));
			spot(13, 20, "^", NORMAL, () -> dish(0, 1));
			put(14, 1, "ARROWS 1 DEG, SHIFT 10, N NEXT DISH", LOW);
		}
		put(16, 0, " TAB ALIGN  ESC QUIT", LOW);
	}

	// -------------------- ALIGN --------------------

	/** Degrees a nudge turns the dish on the ALIGN page, and with shift. */
	private static final double ALIGN_FINE = 0.5D, ALIGN_COARSE = 5.0D;
	/** The graph's rows on the grid, and the levels at its bottom and top, dBm. */
	private static final int GRAPH_TOP = 6, GRAPH_ROWS = 7;
	private static final double GRAPH_LOW = -120.0D, GRAPH_HIGH = -40.0D;

	/**
	 * The ALIGN page: the far end's signal, live, with a bar, the peak of the last half minute and a graph of it, and
	 * nudges for the dish. Rough aim shows a weak reading; turn and tilt a little at a time and watch it climb, then do
	 * the far end. Past the decode line the far end's callsign shows and the link can come up.
	 */
	private void composeAlign() {
		MicrowaveRadioBlockEntity radio = radio();
		List<DishInfo> dishes = radio.getDishInfo();
		int label = 10;
		if(dishes.isEmpty()) {
			put(2, 1, "ALIGN: NO DISH ON THE COAX", INVERSE);
			put(16, 0, " TAB STATUS  ESC QUIT", LOW);
			return;
		}
		dishSel = Math.min(dishSel, dishes.size() - 1);
		DishInfo dish = dishes.get(dishSel);
		put(2, 1, String.format(Locale.ROOT, "%s %d/%d  AZ %05.1f  EL %+05.1f", dish.horn() ? "HORN" : "DISH", dishSel + 1, dishes.size(),
				dish.azimuth(), dish.elevation()), NORMAL);
		double rssi = radio.getRssi();
		boolean none = rssi <= MicrowaveMath.NO_SIGNAL_DBM + 0.5D;
		String far = !radio.getAlignName().isEmpty() ? radio.getAlignName() : none ? "NO SIGNAL" : "SIGNAL, NOT DECODED";
		put(3, 1, dots("FAR END ", label) + " " + far, NORMAL);
		String grade = MicrowaveMath.grade(rssi).label; // All the radio knows: how loud. Why not, see /rcdebug microwave.
		put(4, 1, dots("RSSI ", label) + " " + (none ? "----.-" : String.format(Locale.ROOT, "%6.1f", rssi)) + " DBM  " + grade,
				!none && rssi >= MicrowaveMath.SENSITIVITY_DBM ? NORMAL : INVERSE);
		int width = 26, filled = none ? 0 : (int)Math.round(Math.max(0.0D, Math.min(1.0D, (rssi - GRAPH_LOW) / (GRAPH_HIGH - GRAPH_LOW))) * width);
		float peak = Float.NEGATIVE_INFINITY;
		for(float reading : radio.getRssiHistory())
			peak = Math.max(peak, reading);
		put(5, 1, "[" + "#".repeat(filled) + ".".repeat(width - filled) + "]", NORMAL);
		put(5, width + 4, peak <= MicrowaveMath.NO_SIGNAL_DBM + 0.5F ? "PEAK --" : String.format(Locale.ROOT, "PK %.0f", peak), LOW);
		// Rows GRAPH_TOP .. GRAPH_TOP + GRAPH_ROWS - 1 are the graph, drawn in renderAlignGraph.
		put(13, 1, "TURN", LOW);
		spot(13, 6, "<", NORMAL, () -> dish(-ALIGN_FINE, 0));
		spot(13, 8, ">", NORMAL, () -> dish(ALIGN_FINE, 0));
		put(13, 11, "TILT", LOW);
		spot(13, 16, "v", NORMAL, () -> dish(0, -ALIGN_FINE));
		spot(13, 18, "^", NORMAL, () -> dish(0, ALIGN_FINE));
		spot(13, 21, "[N] NEXT DISH", NORMAL, () -> dishSel = (dishSel + 1) % dishes.size());
		put(14, 1, "ARROWS 0.5 DEG, SHIFT 5. PEAK THE SIGNAL", LOW);
		put(15, 1, "THEN PEAK THE FAR END'S DISH THE SAME", LOW);
		put(16, 0, " TAB STATUS  ESC QUIT", LOW);
	}

	/** The signal over the last half minute, on the CRT: the threshold and good lines, and the trace. */
	private void renderAlignGraph(GuiGraphics guiGraphics, long now) {
		if(page != Page.ALIGN || radio().getDishInfo().isEmpty() || (now - pageAt) / 15L < GRAPH_TOP + GRAPH_ROWS)
			return;
		int x0 = leftPos + GRID_X + CELL_W, y0 = topPos + GRID_Y + GRAPH_TOP * CELL_H;
		int w = (COLS - 2) * CELL_W, h = GRAPH_ROWS * CELL_H;
		guiGraphics.fill(x0, y0 + h - 1, x0 + w, y0 + h, DIM); // The floor.
		guiGraphics.fill(x0, y0, x0 + 1, y0 + h, DIM);
		for(double line : new double[] { MicrowaveMath.SENSITIVITY_DBM, MicrowaveMath.GOOD_DBM }) { // Dashed level lines.
			int y = graphY(line, y0, h);
			for(int x = x0; x < x0 + w; x += 4)
				guiGraphics.fill(x, y, x + 2, y + 1, DIM);
		}
		float[] history = radio().getRssiHistory();
		int lastY = -1;
		for(int i = 0; i < history.length; i++) {
			int x = x0 + 1 + i * (w - 2) / (history.length - 1);
			if(history[i] <= MicrowaveMath.NO_SIGNAL_DBM + 0.5F) {
				lastY = -1;
				continue;
			}
			int y = graphY(history[i], y0, h);
			int from = lastY < 0 ? y : Math.min(lastY, y), to = lastY < 0 ? y : Math.max(lastY, y);
			guiGraphics.fill(x, from, x + 2, to + 1, GREEN); // Joined to the last point, so the trace is a line.
			lastY = y;
		}
	}

	private static int graphY(double dbm, int y0, int h) {
		double f = Math.max(0.0D, Math.min(1.0D, (dbm - GRAPH_LOW) / (GRAPH_HIGH - GRAPH_LOW)));
		return y0 + h - 2 - (int)Math.round(f * (h - 3));
	}

	/** A quiet beep for each new reading on the ALIGN page, higher the stronger the signal, as an alignment meter's. */
	private int heardHistory;

	private void alignTone() {
		float[] history = radio().getRssiHistory();
		int fingerprint = Arrays.hashCode(history); // Changes with each reading the server sends (the history shifts).
		if(fingerprint == heardHistory)
			return;
		heardHistory = fingerprint;
		float latest = history[history.length - 1];
		if(page == Page.ALIGN && latest > MicrowaveMath.NO_SIGNAL_DBM + 0.5F) {
			double f = Math.max(0.0D, Math.min(1.0D, (latest - GRAPH_LOW) / (GRAPH_HIGH - GRAPH_LOW)));
			play(SoundEvents.NOTE_BLOCK_BIT.value(), (float)(0.6D + 1.4D * f), 0.05F);
		}
	}

	private void composeField(long now) {
		String prompt = switch(field) {
			case CALLSIGN -> "CALLSIGN? ";
			case ID -> "ID, 4 HEX DIGITS? ";
			case FREQUENCY -> "FREQUENCY MHZ? ";
			default -> "";
		};
		put(15, 0, " ".repeat(COLS), NORMAL);
		put(16, 0, " ".repeat(COLS), NORMAL);
		put(15, 1, prompt + typed, INVERSE);
		if((now / 400L) % 2L == 0L)
			put(15, 1 + prompt.length() + typed.length(), " ", NORMAL);
		put(16, 1, "ENTER OK  ESC CANCEL", LOW);
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		MicrowaveRadioBlockEntity radio = radio();
		boolean on = radio.isPowered();
		if(on != wasOn) {
			wasOn = on;
			page = on ? Page.BOOT : Page.OFF;
			bootAt = pageAt = now;
			field = Field.NONE;
			bootShown = 0;
			if(on && lastPhase != null) // Switched on with the screen open: the power-on beep.
				tones(0.12F, 1.0F, 1.5F);
		}
		playLater(now);
		if(radio.getPhase() != lastPhase) { // A chime when a master is found, a rising chirp when linked, falling tones when lost.
			if(lastPhase != null) {
				switch(radio.getPhase()) {
					case FOUND -> play(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.6F, 0.3F);
					case LINKED -> tones(0.12F, 1.0F, 1.33F, 1.78F);
					case NOT_FOUND, LOST -> tones(0.12F, 1.1F, 0.8F, 0.6F);
					default -> {}
				}
			}
			lastPhase = radio.getPhase();
		}
		if(on)
			blips(now, radio.getPhase());
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		if(on)
			guiGraphics.blit(WIDGETS, leftPos + LAMP[0], topPos + LAMP[1], LAMP[0], LAMP[1], LAMP[2], LAMP[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		for(int i = 0; i < BUTTONS.length; i++) {
			int[] b = BUTTONS[i];
			boolean down = inside(b, mouseX, mouseY) || (i == 1 && page == Page.STATUS) || (i == 2 && page == Page.SETUP);
			if(down)
				guiGraphics.blit(WIDGETS, leftPos + b[0], topPos + b[1], b[0], b[1], b[2], b[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}
		renderKnobPointer(guiGraphics, radio.getFrequency());
		if(page == Page.OFF)
			return;
		compose(now, mouseX, mouseY);
		renderGrid(guiGraphics);
		renderAlignGraph(guiGraphics, now);
		alignTone();
		renderGlass(guiGraphics);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// All drawn in renderBg.
	}

	/** The FREQ knob's white line, turned with the frequency. */
	private void renderKnobPointer(GuiGraphics guiGraphics, int mhz) {
		double turn = (mhz - MicrowaveMath.MIN_MHZ) / (double)(MicrowaveMath.MAX_MHZ - MicrowaveMath.MIN_MHZ);
		double angle = Math.toRadians(-135.0D + 270.0D * turn);
		double cx = leftPos + KNOB[0] + KNOB[2] / 2.0D, cy = topPos + KNOB[1] + KNOB[3] / 2.0D;
		for(int i = 3; i <= 9; i++) {
			int x = (int)Math.floor(cx + Math.sin(angle) * i), y = (int)Math.floor(cy - Math.cos(angle) * i);
			guiGraphics.fill(x, y, x + 1, y + 1, 0xFFE0E0D8);
		}
	}

	private void renderGrid(GuiGraphics guiGraphics) {
		for(int row = 0; row < ROWS; row++) {
			int y = topPos + GRID_Y + row * CELL_H;
			int col = 0;
			while(col < COLS) {
				boolean lit = attrs[row][col] == INVERSE;
				int end = col + 1;
				while(end < COLS && (attrs[row][end] == INVERSE) == lit)
					end++;
				if(lit)
					guiGraphics.fill(leftPos + GRID_X + col * CELL_W, y, leftPos + GRID_X + end * CELL_W, y + CELL_H, GREEN);
				col = end;
			}
		}
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(leftPos + GRID_X, topPos + GRID_Y, 0.0F);
		guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		Matrix4f pose = guiGraphics.pose().last().pose();
		float cellW = CELL_W / TEXT_SCALE, cellH = CELL_H / TEXT_SCALE;
		for(int row = 0; row < ROWS; row++)
			for(int col = 0; col < COLS; col++) {
				char c = chars[row][col];
				if(c == ' ')
					continue;
				String text = String.valueOf(c);
				int colour = attrs[row][col] == INVERSE ? INK : attrs[row][col] == LOW ? DIM : GREEN;
				font.drawInBatch(text, col * cellW + (cellW - font.width(text) + 1) / 2.0F, row * cellH + 1.5F, colour, false, pose,
						guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			}
		guiGraphics.flush();
		guiGraphics.pose().popPose();
	}

	private void renderGlass(GuiGraphics guiGraphics) {
		int x0 = leftPos + CRT[0], y0 = topPos + CRT[1];
		guiGraphics.fill(x0, y0, x0 + CRT[2], y0 + CRT[3], 0x0840FF70);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x0, y0, 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		for(int y = 1; y < CRT[3] * 2; y += 2)
			guiGraphics.fill(0, y, CRT[2] * 2, y + 1, 0x22000000);
		guiGraphics.pose().popPose();
	}
}

package com.arrl.radiocraft.client.screens;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity.Entry;
import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity.ModuleStatus;
import com.arrl.radiocraft.common.menus.DirectionFinderMenu;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.arrl.radiocraft.common.radio.data.AprsIcon;
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
 * The direction finder's screen: an amber CRT in the H19's manner, with fewer pages. It boots when switched on (the
 * map enabled one also loads its world map off the floppy), then:
 * <ol>
 *     <li>DF: the frequency (arrows tune, up and down change band, F types one in), the signal meter, each module's
 *     state, and once three have locked a compass with the bearing and how far it may be off.</li>
 *     <li>FIX: this station's bearing and those typed in from other direction finders (A adds one, D deletes the
 *     selected), and where they cross: a point in a box on a plain set, or on the map on a map enabled one.</li>
 * </ol>
 * The POWER button on the side panel switches it on and off.
 */
public class DirectionFinderScreen extends StationPanelScreen<DirectionFinderMenu> {

	private static final ResourceLocation TEXTURE = Radiocraft.id("textures/gui/direction_finder.png");
	private static final ResourceLocation WIDGETS = Radiocraft.id("textures/gui/direction_finder_widgets.png");
	private static final ResourceLocation MAP_LOCATION = Radiocraft.id("dynamic/direction_finder_map");
	private static final int TEXTURE_WIDTH = 300, TEXTURE_HEIGHT = 200;
	/** Boxes in the texture, {x, y, width, height}: the glass, the POWER button and the PWR lamp. */
	private static final int[] CRT = {10, 10, 232, 164};
	private static final int[] POWER_BUTTON = {252, 150, 38, 20};
	private static final int[] LAMP = {264, 128, 14, 14};

	private static final int COLS = 74, ROWS = 31, CELL_W = 3, CELL_H = 5;
	private static final float TEXT_SCALE = 0.5F;
	private static final int GRID_X = CRT[0] + (CRT[2] - COLS * CELL_W) / 2, GRID_Y = CRT[1] + (CRT[3] - ROWS * CELL_H) / 2;
	/** Where the fix is plotted, right of the list on the FIX page. */
	private static final int PLOT_COL = 35;
	private static final int[] PLOT = {GRID_X + PLOT_COL * CELL_W, GRID_Y + 2 * CELL_H, (COLS - PLOT_COL - 1) * CELL_W, 27 * CELL_H};
	/** The compass on the DF page: centre and radius in half texels from the panel corner. */
	private static final int COMPASS_X = (GRID_X + 56 * CELL_W) * 2, COMPASS_Y = (GRID_Y + 15 * CELL_H) * 2, COMPASS_R = 92;
	private static final int FOOTER_ROW = ROWS - 1, STATUS_ROW = ROWS - 2;

	private static final int AMBER = 0xFFFFB020;
	private static final int DIM = 0xFF8C5A0C;
	private static final int FAINT = 0xFF3A2606;
	private static final int INK = 0xFF140A00;
	private static final byte NORMAL = 0, LOW = 1, INVERSE = 2;

	private static final String[] ART = {
			" ____    ____     __",
			"/\\  _`\\ /\\  _`\\  /\\ \\",
			"\\ \\ \\/\\ \\ \\ \\L\\_\\\\_\\ \\___",
			" \\ \\ \\ \\ \\ \\  _\\/\\___  __\\",
			"  \\ \\ \\_\\ \\ \\ \\/\\/__/\\ \\_/",
			"   \\ \\____/\\ \\_\\    \\ \\_\\",
			"    \\/___/  \\/_/     \\/_/",
	};

	private enum Page { OFF, BOOT, DF, FIX }

	private final char[][] chars = new char[ROWS][COLS];
	private final byte[][] attrs = new byte[ROWS][COLS];
	private final boolean[][] opaque = new boolean[ROWS][COLS];
	private record Spot(int row, int col, int length, Runnable action) {}
	private final List<Spot> spots = new ArrayList<>();
	private final RandomSource random = RandomSource.create();

	private Page page;
	private long pageAt = System.currentTimeMillis();
	private int revealed = 0;
	private String status = "";
	private long statusUntil = 0L;
	private int entrySel = 0;

	/** Typing a frequency (F on the DF page), or a bearing from another station (A on the FIX page), field by field. */
	private enum Field { NONE, FREQUENCY, X, Z, BEARING, ERROR }
	private Field field = Field.NONE;
	private String typed = "";
	private int newX, newZ;
	private double newBearing;

	@Nullable
	private H19Map map = null;
	private double viewX, viewZ, viewBpp = 1.0D;

	// The boot script, as on the H19.
	private enum StepKind { PRINT, FAST, TYPE, PROMPT, WAIT, SEEK, BEEP, LOAD_MAP }
	private record Step(StepKind kind, String text, int millis) {}
	private final List<String> console = new ArrayList<>();
	private final List<Step> script = new ArrayList<>();
	private String consoleLine = "";
	private int step = 0, stepChars = 0;
	private long stepAt = 0L, nextNoise = 0L;
	private boolean wasOn;

	private record Pending(long at, Runnable play) {}
	private final List<Pending> pending = new ArrayList<>();

	public DirectionFinderScreen(DirectionFinderMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = TEXTURE_WIDTH;
		this.imageHeight = TEXTURE_HEIGHT;
		wasOn = finder().isOn();
		if(!wasOn)
			page = Page.OFF;
		else if(finder().isBootPending())
			startBoot();
		else
			page = Page.DF;
	}

	private DirectionFinderBlockEntity finder() {
		return menu.blockEntity;
	}

	/** The terminal's fan and transformer, louder than the H19's. */
	@Nullable
	private TerminalWhirr whirr;

	@Override
	protected void init() {
		super.init();
		if(whirr == null)
			whirr = TerminalWhirr.start(0.22F);
	}

	@Override
	public void removed() {
		super.removed();
		if(whirr != null) {
			whirr.finish();
			whirr = null;
		}
		if(map != null) {
			map.close();
			map = null;
		}
	}

	private void send(String action, CompoundTag data) {
		SStationPacket.send(finder().getBlockPos(), action, data);
	}

	private void switchPage(Page to) {
		if(page == to)
			return;
		page = to;
		pageAt = System.currentTimeMillis();
		revealed = 0;
		field = Field.NONE;
		seekBurst(3);
	}

	private void setStatus(String text) {
		status = text;
		statusUntil = System.currentTimeMillis() + 3000L;
	}

	// -------------------- BOOT --------------------

	private void startBoot() {
		DirectionFinderBlockEntity finder = finder();
		console.clear();
		script.clear();
		consoleLine = "";
		step = stepChars = 0;
		stepAt = System.currentTimeMillis();
		add(StepKind.WAIT, "", 400);
		add(StepKind.BEEP, "", 0);
		add(StepKind.PRINT, "DF-1 DIRECTION FINDER    FIRMWARE 2.3    (C) 1981", 0);
		add(StepKind.PRINT, "MEMORY TEST .......... 32K OK", 0);
		add(StepKind.PRINT, "RECEIVER ............. " + Math.round(DirectionFinderBlockEntity.bands().getFirst().minFrequency() / 1.0E6F) + "-"
				+ Math.round(DirectionFinderBlockEntity.bands().getLast().maxFrequency() / 1.0E6F) + " MHZ", 0);
		add(StepKind.SEEK, "", 500);
		add(StepKind.PRINT, "", 0);
		for(String line : ART)
			add(StepKind.FAST, "   " + line, 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.PRINT, "      RADIO DIRECTION FINDING SYSTEM", 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.PRINT, "ANTENNA MODULES ...... " + finder.getModules().size() + " (" + DirectionFinding.MIN_MODULES + " TO " + DirectionFinding.MAX_MODULES + ")", 0);
		add(StepKind.SEEK, "", 300);
		add(StepKind.PRINT, "ANTENNA TYPE ......... " + antennaName(), 0);
		if(finder.hasMap()) {
			add(StepKind.PRINT, "WORLD MAP MODULE ..... FOUND IN DRIVE A:", 0);
			add(StepKind.PROMPT, "", 400);
			add(StepKind.TYPE, "LOAD MAP", 0);
			add(StepKind.LOAD_MAP, "", 2600);
		}
		else
			add(StepKind.PRINT, "WORLD MAP MODULE ..... NONE (GRID PLOT ONLY)", 0);
		add(StepKind.PRINT, "", 0);
		add(StepKind.BEEP, "", 0);
		add(StepKind.PRINT, "READY.", 0);
		add(StepKind.PROMPT, "", 800);
		page = Page.BOOT;
		pageAt = stepAt;
	}

	private void add(StepKind kind, String text, int millis) {
		script.add(new Step(kind, text, millis));
	}

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
					grind(now);
					if(elapsed < current.millis())
						return;
				}
				case BEEP -> beep();
				case LOAD_MAP -> { // The floppy grinds while a bar fills: the world map coming off the disk.
					grind(now);
					int width = 30;
					int done = (int)Math.min(width, elapsed * width / Math.max(1, current.millis()));
					String[] sectors = {"CONTOURS", "WATER", "FORESTS", "GRID", "LABELS"};
					consoleLine = "LOADING WORLD MAP [" + "#".repeat(done) + ".".repeat(width - done) + "] "
							+ sectors[Math.min(sectors.length - 1, done * sectors.length / width)];
					if(done < width)
						return;
					console.add("LOADING WORLD MAP [" + "#".repeat(width) + "] DONE");
					consoleLine = "";
				}
			}
			step++;
			stepAt = now;
			stepChars = 0;
		}
		finishBoot();
	}

	private void grind(long now) {
		if(now >= nextNoise) {
			seek();
			nextNoise = now + 30 + random.nextInt(90);
		}
	}

	private void finishBoot() {
		send("booted", new CompoundTag());
		page = Page.BOOT; // So switchPage sees a change.
		switchPage(Page.DF);
	}

	// -------------------- INPUT --------------------

	private static boolean isEnter(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(page == Page.BOOT) {
			if(keyCode == GLFW.GLFW_KEY_ESCAPE)
				return super.keyPressed(keyCode, scanCode, modifiers);
			if(isEnter(keyCode) || keyCode == GLFW.GLFW_KEY_SPACE)
				finishBoot();
			return true;
		}
		if(page == Page.OFF || (keyCode == GLFW.GLFW_KEY_ESCAPE && field == Field.NONE))
			return super.keyPressed(keyCode, scanCode, modifiers);
		keyClick();
		if(field != Field.NONE) {
			fieldKey(keyCode);
			return true;
		}
		boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
		if(keyCode == GLFW.GLFW_KEY_1)
			switchPage(Page.DF);
		else if(keyCode == GLFW.GLFW_KEY_2)
			switchPage(Page.FIX);
		else if(keyCode == GLFW.GLFW_KEY_TAB)
			switchPage(page == Page.DF ? Page.FIX : Page.DF);
		else if(page == Page.DF) {
			switch(keyCode) {
				case GLFW.GLFW_KEY_RIGHT -> tune(shift ? 10 : 1);
				case GLFW.GLFW_KEY_LEFT -> tune(shift ? -10 : -1);
				case GLFW.GLFW_KEY_UP -> band(1);
				case GLFW.GLFW_KEY_DOWN -> band(-1);
				case GLFW.GLFW_KEY_F -> startField(Field.FREQUENCY);
				default -> {}
			}
		}
		else if(page == Page.FIX) {
			int count = finder().getEntries().size();
			switch(keyCode) {
				case GLFW.GLFW_KEY_UP -> entrySel = Math.max(0, entrySel - 1);
				case GLFW.GLFW_KEY_DOWN -> entrySel = Math.min(Math.max(0, count - 1), entrySel + 1);
				case GLFW.GLFW_KEY_A -> addBearing();
				case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_DELETE -> deleteBearing();
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

	private void band(int direction) {
		CompoundTag data = new CompoundTag();
		data.putInt("direction", direction);
		send("band", data);
		seekBurst(2);
	}

	private void addBearing() {
		if(finder().getEntries().size() >= DirectionFinderBlockEntity.MAX_ENTRIES)
			setStatus("BEARING LIST FULL: DELETE ONE FIRST");
		else
			startField(Field.X);
	}

	private void deleteBearing() {
		int count = finder().getEntries().size();
		if(count == 0)
			return;
		CompoundTag data = new CompoundTag();
		data.putInt("index", Math.min(entrySel, count - 1));
		send("remove", data);
		entrySel = Math.max(0, Math.min(entrySel, count - 2));
		setStatus("BEARING DELETED");
	}

	private void startField(Field to) {
		field = to;
		typed = "";
	}

	private void fieldKey(int keyCode) {
		if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
			field = Field.NONE;
			setStatus("CANCELLED");
		}
		else if(keyCode == GLFW.GLFW_KEY_BACKSPACE)
			typed = typed.isEmpty() ? typed : typed.substring(0, typed.length() - 1);
		else if(isEnter(keyCode))
			enterField();
	}

	/** ENTER on a field: check it, then go on to the next one or send what was typed. */
	private void enterField() {
		try {
			switch(field) {
				case FREQUENCY -> {
					float hertz = (float)(Double.parseDouble(typed) * 1.0E6D);
					if(DirectionFinderBlockEntity.bands().stream().noneMatch(band -> hertz >= band.minFrequency() && hertz <= band.maxFrequency())) {
						setStatus("NOT IN A BAND: " + typed + " MHZ");
						field = Field.NONE;
						return;
					}
					CompoundTag data = new CompoundTag();
					data.putFloat("hertz", hertz);
					send("frequency", data);
					field = Field.NONE;
				}
				case X -> {
					newX = Integer.parseInt(typed);
					startField(Field.Z);
				}
				case Z -> {
					newZ = Integer.parseInt(typed);
					startField(Field.BEARING);
				}
				case BEARING -> {
					newBearing = Double.parseDouble(typed);
					if(newBearing < 0.0D || newBearing >= 360.0D)
						throw new NumberFormatException();
					startField(Field.ERROR);
				}
				case ERROR -> {
					double sigma = typed.isEmpty() ? 5.0D : Double.parseDouble(typed);
					if(sigma <= 0.0D)
						throw new NumberFormatException();
					CompoundTag data = new CompoundTag();
					data.putInt("x", newX);
					data.putInt("z", newZ);
					data.putDouble("bearing", newBearing);
					data.putDouble("sigma", sigma);
					send("add", data);
					field = Field.NONE;
					setStatus("BEARING ADDED");
					chirp();
				}
				default -> field = Field.NONE;
			}
		}
		catch(NumberFormatException e) {
			beep();
			setStatus("?SYNTAX ERROR");
			typed = "";
		}
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if(field == Field.NONE || typed.length() >= 10)
			return true;
		boolean ok = Character.isDigit(codePoint) || (codePoint == '-' && typed.isEmpty() && (field == Field.X || field == Field.Z))
				|| (codePoint == '.' && !typed.contains(".") && field != Field.X && field != Field.Z);
		if(ok)
			typed += codePoint;
		return true;
	}

	@Override
	protected boolean panelClicked(double mouseX, double mouseY, int button) {
		if(button != 0)
			return false;
		if(inside(POWER_BUTTON, mouseX, mouseY)) {
			powerBeep();
			send("power", new CompoundTag());
			return true;
		}
		if(page == Page.BOOT || page == Page.OFF)
			return inside(CRT, mouseX, mouseY);
		Spot spot = spotAt(mouseX, mouseY);
		if(spot != null) {
			keyClick();
			spot.action().run();
			return true;
		}
		return false;
	}

	@Override
	protected boolean panelScrolled(double mouseX, double mouseY, double scrollY) {
		if(!inside(CRT, mouseX, mouseY) || page != Page.DF || field != Field.NONE)
			return false;
		tune(scrollY > 0 ? 1 : -1); // The tuning knob.
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

	// -------------------- SOUNDS --------------------

	private void play(SoundEvent sound, float pitch, float volume) {
		if(minecraft != null)
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	private void printTick() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.7F + random.nextFloat() * 0.3F, 0.05F);
	}

	/** A key: the printing's tick, a touch louder. */
	private void keyClick() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.75F + random.nextFloat() * 0.2F, 0.08F);
	}

	/** The drive head stepping: the printing's tick, a little lower. */
	private void seek() {
		play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.5F + random.nextFloat() * 0.15F, 0.05F);
	}

	/** The power switch: two quiet tones. */
	private void powerBeep() {
		play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0F, 0.12F);
		pending.add(new Pending(System.currentTimeMillis() + 90L, () -> play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.5F, 0.12F)));
	}

	private void beep() {
		play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.3F, 0.25F);
	}

	private void seekBurst(int clicks) {
		long at = System.currentTimeMillis();
		for(int i = 0; i < clicks; i++) {
			at += 30 + random.nextInt(60);
			pending.add(new Pending(at, this::seek));
		}
	}

	private void chirp() {
		long at = System.currentTimeMillis();
		for(int i = 0; i < 4; i++)
			pending.add(new Pending(at + i * 60L, () -> play(SoundEvents.NOTE_BLOCK_BIT.value(), 1.6F, 0.12F)));
	}

	private void runPending(long now) {
		List<Pending> due = pending.stream().filter(sound -> sound.at() <= now).toList();
		pending.removeAll(due);
		due.forEach(sound -> sound.play().run());
	}

	// -------------------- TEXT --------------------

	private String antennaName() {
		ResourceLocation type = finder().getAntennaType();
		return type == null ? "NONE" : type.getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
	}

	private static String frequency(float hertz) {
		return String.format(Locale.ROOT, "%.3f MHZ", hertz / 1.0E6D);
	}

	private static String degrees(double bearing) {
		return String.format(Locale.ROOT, "%03d", Math.round(DirectionFinding.normalize(bearing)) % 360);
	}

	private String problemText(DirectionFinding.Problem problem) {
		return switch(problem) {
			case TOO_FEW -> "NEED 2 BEARINGS";
			case TOO_CLOSE -> "STATIONS TOO CLOSE (" + (int)DirectionFinding.MIN_BASELINE + "+ APART)";
			case TOO_PARALLEL -> "BEARINGS TOO NEAR PARALLEL";
			case DIVERGE -> "BEARINGS DO NOT CROSS";
			case OK -> "";
		};
	}

	/** What the set is doing, for the DF page's status line. */
	private String state() {
		DirectionFinderBlockEntity finder = finder();
		List<ModuleStatus> statuses = finder.getStatuses();
		int usable = (int)statuses.stream().filter(s -> s == ModuleStatus.READY || s == ModuleStatus.LOCKED).count();
		if(statuses.isEmpty())
			return "NO ANTENNA MODULES";
		if(usable == 0)
			return statuses.contains(ModuleStatus.MISMATCH) ? "ANTENNAS DO NOT MATCH" : "NO ANTENNA ON ANY MODULE";
		if(usable < DirectionFinding.MIN_MODULES)
			return "NEED " + DirectionFinding.MIN_MODULES + " MODULES WITH MATCHING ANTENNAS";
		if(finder.isHearing())
			return finder.getLocked() < DirectionFinding.MIN_MODULES ? "LOCKING ON..." : finder.getLocked() < usable ? "TRACKING, LOCKING MORE..." : "TRACKING";
		return finder.hasBearing() ? "NO SIGNAL: LAST BEARING HELD" : "LISTENING";
	}

	// -------------------- COMPOSING THE GRID --------------------

	private void clear() {
		for(int row = 0; row < ROWS; row++) {
			Arrays.fill(chars[row], ' ');
			Arrays.fill(attrs[row], NORMAL);
			Arrays.fill(opaque[row], false);
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

	private static String dots(String label, int length) {
		return label.length() >= length ? label : label + ".".repeat(length - label.length());
	}

	private void compose(long now, int mouseX, int mouseY) {
		clear();
		switch(page) {
			case BOOT -> composeBoot(now);
			case DF -> composeDf(now);
			case FIX -> composeFix(now);
			default -> {}
		}
		if(page == Page.BOOT || page == Page.OFF)
			return;
		if(now < statusUntil) {
			fillRow(STATUS_ROW, NORMAL);
			put(STATUS_ROW, 1, status, INVERSE);
			Arrays.fill(opaque[STATUS_ROW], true);
		}
		if(field != Field.NONE)
			composeField(now);
		composeHeader();

		int reveal = (int)Math.min(ROWS + 1L, (now - pageAt) / 12L);
		if(reveal / 4 > revealed / 4 && revealed < ROWS)
			printTick();
		revealed = Math.max(revealed, reveal);
		for(int row = Math.max(1, reveal); row < ROWS; row++) {
			Arrays.fill(chars[row], ' ');
			Arrays.fill(attrs[row], NORMAL);
			Arrays.fill(opaque[row], false);
		}
		spots.removeIf(spot -> spot.row() > 0 && spot.row() >= reveal);

		Spot hover = spotAt(mouseX, mouseY);
		if(hover != null)
			for(int col = hover.col(); col < hover.col() + hover.length() && col < COLS; col++)
				attrs[hover.row()][col] = attrs[hover.row()][col] == INVERSE ? LOW : INVERSE;
	}

	private void composeHeader() {
		fillRow(0, INVERSE);
		DirectionFinderBlockEntity finder = finder();
		put(0, 1, "DF-1  " + frequency(finder.getFrequency()) + "  " + DirectionFinderBlockEntity.bandOf(finder.getFrequency()).name().toUpperCase(Locale.ROOT), INVERSE);
		spot(0, 46, "1 DF", page == Page.DF ? NORMAL : INVERSE, () -> switchPage(Page.DF));
		spot(0, 52, "2 FIX", page == Page.FIX ? NORMAL : INVERSE, () -> switchPage(Page.FIX));
		if(minecraft != null && minecraft.level != null) {
			long day = minecraft.level.getDayTime() % 24000L;
			put(0, 68, String.format(Locale.ROOT, "%02d:%02d", (day / 1000L + 6L) % 24L, day % 1000L * 60L / 1000L), INVERSE);
		}
	}

	private void composeBoot(long now) {
		List<String> lines = new ArrayList<>(console);
		lines.add(consoleLine);
		int first = Math.max(0, lines.size() - ROWS);
		for(int i = first; i < lines.size(); i++)
			put(i - first, 0, lines.get(i), NORMAL);
		cursor(lines.size() - 1 - first, consoleLine.length(), now);
	}

	private void composeDf(long now) {
		DirectionFinderBlockEntity finder = finder();
		int label = 15;
		put(2, 1, dots("FREQUENCY ", label) + " ", NORMAL);
		spot(2, label + 2, "<", NORMAL, () -> tune(-1));
		put(2, label + 4, frequency(finder.getFrequency()), NORMAL);
		spot(2, label + 16, ">", NORMAL, () -> tune(1));
		Band band = DirectionFinderBlockEntity.bandOf(finder.getFrequency());
		put(3, 1, dots("BAND ", label) + " ", NORMAL);
		spot(3, label + 2, "<", NORMAL, () -> band(-1));
		put(3, label + 4, band.name().toUpperCase(Locale.ROOT), NORMAL);
		spot(3, label + 9, ">", NORMAL, () -> band(1));

		int bars = (int)Math.round(finder.getMeter() * 15.0D);
		put(5, 1, dots("SIGNAL ", label) + " [" + "#".repeat(bars) + ".".repeat(15 - bars) + "]", NORMAL);
		put(6, 1, dots("ANTENNA TYPE ", label) + " " + antennaName(), NORMAL);

		List<ModuleStatus> statuses = finder.getStatuses();
		put(8, 1, dots("MODULES ", label) + " " + statuses.size() + " OF " + DirectionFinding.MAX_MODULES + " (" + DirectionFinding.MIN_MODULES + " NEEDED)", NORMAL);
		for(int i = 0; i < DirectionFinding.MAX_MODULES; i++) {
			int row = 9 + i / 2, col = 3 + (i % 2) * 16;
			String text = i >= statuses.size() ? "--" : switch(statuses.get(i)) {
				case LOCKED -> "LOCKED";
				case READY -> "READY";
				case MISMATCH -> "WRONG ANT";
				case NO_ANTENNA -> "NO ANTENNA";
			};
			put(row, col, (i + 1) + " " + text, i >= statuses.size() ? LOW : statuses.get(i) == ModuleStatus.LOCKED ? INVERSE : NORMAL);
		}
		put(14, 1, dots("LOCKED ", label) + " " + finder.getLocked(), NORMAL);
		if(finder.hasBearing()) {
			put(16, 1, dots("BEARING ", label) + " " + degrees(finder.getBearing()) + " DEG", INVERSE);
			put(17, 1, dots("ACCURACY ", label) + " +/- " + String.format(Locale.ROOT, "%.1f", finder.getSigma()) + " DEG", NORMAL);
			BlockPos here = finder.getBlockPos();
			put(18, 1, dots("FROM ", label) + " X " + here.getX() + "  Z " + here.getZ(), LOW);
		}
		else
			put(16, 1, dots("BEARING ", label) + " ---", LOW);
		put(26, 1, dots("STATUS ", label) + " " + state(), NORMAL);
		if(finder.isHearing() && (now / 300L) % 2L == 0L)
			put(2, 36, "*RX*", INVERSE);
		put(FOOTER_ROW, 0, " <> TUNE (SHIFT X10)  UP/DN BAND  F TYPE FREQ  TAB FIX  ESC QUIT", LOW);
	}

	private void composeFix(long now) {
		DirectionFinderBlockEntity finder = finder();
		List<Entry> entries = finder.getEntries();
		put(2, 1, "#  STATION X/Z        BRG  ERR", LOW);
		BlockPos here = finder.getBlockPos();
		if(finder.hasBearing())
			put(3, 1, "H  " + pad(here.getX() + " " + here.getZ(), 15) + " " + degrees(finder.getBearing()) + "  " + Math.round(finder.getSigma()), NORMAL);
		else
			put(3, 1, "H  " + pad(here.getX() + " " + here.getZ(), 15) + " ---  (NO BEARING YET)", LOW);
		for(int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int index = i;
			byte attr = i == entrySel ? INVERSE : NORMAL;
			spot(4 + i, 1, (i + 1) + "  " + pad(entry.x() + " " + entry.z(), 15) + " " + degrees(entry.bearing()) + "  " + Math.round(entry.sigma()), attr, () -> entrySel = index);
		}
		int row = 5 + DirectionFinderBlockEntity.MAX_ENTRIES;
		List<DirectionFinderBlockEntity.Linked> linked = finder.getLinked();
		for(int i = 0; i < linked.size() && i < 3; i++) { // Bearings over the microwave link, kept up to date by themselves.
			DirectionFinding.Line line = linked.get(i).line();
			put(4 + entries.size() + i, 1, "L  " + pad((int)Math.floor(line.x()) + " " + (int)Math.floor(line.z()), 15) + " " + degrees(line.bearing()) + "  "
					+ Math.round(line.sigma()) + " " + linked.get(i).from(), LOW);
		}
		row = Math.max(row, 4 + entries.size() + Math.min(3, linked.size()) + 1);
		spot(row, 1, "[A] ADD", NORMAL, this::addBearing);
		spot(row, 10, "[D] DELETE", NORMAL, this::deleteBearing);

		DirectionFinding.Fix fix = DirectionFinding.triangulate(finder.lines());
		row += 2;
		if(fix.found()) {
			put(row, 1, "FIX", INVERSE);
			put(row + 1, 1, dots("X ", 8) + " " + Math.round(fix.x()), NORMAL);
			put(row + 2, 1, dots("Z ", 8) + " " + Math.round(fix.z()), NORMAL);
			put(row + 3, 1, dots("WITHIN ", 8) + " " + Math.round(fix.radius()) + " BLOCKS", NORMAL);
			double distance = Math.hypot(fix.x() - here.getX() - 0.5D, fix.z() - here.getZ() - 0.5D);
			put(row + 4, 1, dots("RANGE ", 8) + " " + Math.round(distance) + " AT " + degrees(DirectionFinding.bearing(here.getX() + 0.5D, here.getZ() + 0.5D, fix.x(), fix.z())), NORMAL);
		}
		else {
			put(row, 1, "NO FIX", INVERSE);
			put(row + 1, 1, problemText(fix.problem()), NORMAL);
		}
		put(FOOTER_ROW, 0, " UP/DN SELECT  A ADD BEARING  D DELETE  TAB DF  ESC QUIT", LOW);
		if(!finder.hasMap())
			put(28, PLOT_COL + 1, "GRID PLOT", LOW);
	}

	private static String pad(String text, int length) {
		return text.length() >= length ? text.substring(0, length) : text + " ".repeat(length - text.length());
	}

	private void composeField(long now) {
		String prompt = switch(field) {
			case FREQUENCY -> "FREQUENCY (MHZ)? ";
			case X -> "OTHER STATION X? ";
			case Z -> "OTHER STATION Z? ";
			case BEARING -> "ITS BEARING (0-359)? ";
			case ERROR -> "ITS ACCURACY +/- DEG (ENTER = 5)? ";
			default -> "";
		};
		fillRow(STATUS_ROW, NORMAL);
		Arrays.fill(opaque[STATUS_ROW], true);
		put(STATUS_ROW, 1, prompt + typed, NORMAL);
		cursor(STATUS_ROW, 1 + prompt.length() + typed.length(), now);
		put(FOOTER_ROW, 0, " " + pad("ENTER OK  ESC CANCEL", COLS - 1), LOW);
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		runPending(now);
		boolean on = finder().isOn();
		if(on != wasOn) { // Switched on or off while the screen is open.
			wasOn = on;
			if(on)
				startBoot();
			else {
				page = Page.OFF;
				field = Field.NONE;
			}
		}
		if(page == Page.BOOT)
			runBoot(now);
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		if(on)
			guiGraphics.blit(WIDGETS, leftPos + LAMP[0], topPos + LAMP[1], LAMP[0], LAMP[1], LAMP[2], LAMP[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		if(inside(POWER_BUTTON, mouseX, mouseY))
			guiGraphics.blit(WIDGETS, leftPos + POWER_BUTTON[0], topPos + POWER_BUTTON[1], POWER_BUTTON[0], POWER_BUTTON[1], POWER_BUTTON[2], POWER_BUTTON[3], TEXTURE_WIDTH, TEXTURE_HEIGHT);
		if(page == Page.OFF)
			return; // A dark screen.
		compose(now, mouseX, mouseY);
		if(revealed >= ROWS) {
			if(page == Page.DF)
				renderCompass(guiGraphics);
			else if(page == Page.FIX)
				renderPlot(guiGraphics);
		}
		renderGrid(guiGraphics);
		renderGlass(guiGraphics);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// All drawn on the screen in renderBg.
	}

	/** One half-texel dot, in half texels from the panel corner. */
	private void dot(GuiGraphics guiGraphics, int x, int y, int colour) {
		guiGraphics.fill(x, y, x + 1, y + 1, colour);
	}

	/** A line in half texels, solid or dotted (every other dot), clipped to {@code clip} ({x0, y0, x1, y1}, half texels). */
	private void line(GuiGraphics guiGraphics, double x0, double y0, double x1, double y1, int colour, boolean dotted, int[] clip) {
		double length = Math.hypot(x1 - x0, y1 - y0);
		int steps = (int)Math.ceil(length);
		for(int i = 0; i <= steps; i++) {
			if(dotted && i % 3 != 0)
				continue;
			double t = steps == 0 ? 0.0D : (double)i / steps;
			int x = (int)Math.floor(x0 + (x1 - x0) * t), y = (int)Math.floor(y0 + (y1 - y0) * t);
			if(x >= clip[0] && x < clip[2] && y >= clip[1] && y < clip[3])
				dot(guiGraphics, x, y, colour);
		}
	}

	/** The compass rose on the DF page: ticks, the cardinal points, and the needle with its error wedge. */
	private void renderCompass(GuiGraphics guiGraphics) {
		DirectionFinderBlockEntity finder = finder();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(leftPos, topPos, 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		int[] clip = {CRT[0] * 2, CRT[1] * 2, (CRT[0] + CRT[2]) * 2, (CRT[1] + CRT[3]) * 2};
		for(int degree = 0; degree < 360; degree += 2) {
			double a = Math.toRadians(degree);
			dot(guiGraphics, (int)Math.round(COMPASS_X + Math.sin(a) * COMPASS_R), (int)Math.round(COMPASS_Y - Math.cos(a) * COMPASS_R), DIM);
		}
		for(int degree = 0; degree < 360; degree += 30) {
			double a = Math.toRadians(degree);
			int inner = degree % 90 == 0 ? COMPASS_R - 10 : COMPASS_R - 5;
			line(guiGraphics, COMPASS_X + Math.sin(a) * inner, COMPASS_Y - Math.cos(a) * inner, COMPASS_X + Math.sin(a) * COMPASS_R, COMPASS_Y - Math.cos(a) * COMPASS_R, AMBER, false, clip);
		}
		dot(guiGraphics, COMPASS_X, COMPASS_Y, AMBER);
		if(finder.hasBearing()) {
			double bearing = finder.getBearing(), sigma = Math.min(45.0D, finder.getSigma());
			int colour = finder.isHearing() ? AMBER : DIM;
			for(double edge : new double[] { bearing - sigma, bearing + sigma }) {
				double a = Math.toRadians(edge);
				line(guiGraphics, COMPASS_X, COMPASS_Y, COMPASS_X + Math.sin(a) * (COMPASS_R - 2), COMPASS_Y - Math.cos(a) * (COMPASS_R - 2), DIM, true, clip);
			}
			double a = Math.toRadians(bearing);
			for(int w = -1; w <= 1; w++) { // A heavier needle.
				double ox = Math.cos(a) * w * 0.6D, oy = Math.sin(a) * w * 0.6D;
				line(guiGraphics, COMPASS_X + ox, COMPASS_Y + oy, COMPASS_X + ox + Math.sin(a) * (COMPASS_R - 4), COMPASS_Y + oy - Math.cos(a) * (COMPASS_R - 4), colour, false, clip);
			}
		}
		guiGraphics.pose().popPose();
		// The cardinal letters, in the grid's font.
		int cx = COMPASS_X / 2, cy = COMPASS_Y / 2, r = COMPASS_R / 2 + 4;
		text(guiGraphics, "N", leftPos + cx, topPos + cy - r - 2, TEXT_SCALE, AMBER, true);
		text(guiGraphics, "S", leftPos + cx, topPos + cy + r - 2, TEXT_SCALE, AMBER, true);
		text(guiGraphics, "E", leftPos + cx + r, topPos + cy - 2, TEXT_SCALE, AMBER, true);
		text(guiGraphics, "W", leftPos + cx - r, topPos + cy - 2, TEXT_SCALE, AMBER, true);
	}

	/** The FIX page's plot: the stations, their bearing lines, and the fix in its box, on a grid or on the map. */
	private void renderPlot(GuiGraphics guiGraphics) {
		DirectionFinderBlockEntity finder = finder();
		List<DirectionFinding.Line> lines = finder.lines();
		DirectionFinding.Fix fix = DirectionFinding.triangulate(lines);
		int width = PLOT[2] * 2, height = PLOT[3] * 2;
		fitView(finder, lines, fix, width, height);

		guiGraphics.fill(leftPos + PLOT[0] - 1, topPos + PLOT[1] - 1, leftPos + PLOT[0] + PLOT[2] + 1, topPos + PLOT[1] + PLOT[3] + 1, FAINT);
		guiGraphics.fill(leftPos + PLOT[0], topPos + PLOT[1], leftPos + PLOT[0] + PLOT[2], topPos + PLOT[1] + PLOT[3], H19Map.BLACK);
		if(finder.hasMap() && minecraft != null && minecraft.level != null) {
			if(map == null)
				map = new H19Map(width, height, MAP_LOCATION, true);
			map.view(viewX, viewZ, viewBpp);
			List<H19Map.Mark> marks = new ArrayList<>();
			marks.add(new H19Map.Mark("DF", finder.getBlockPos(), AprsIcon.TOWER, true, false));
			for(Entry entry : finder.getEntries())
				marks.add(new H19Map.Mark("", new BlockPos(entry.x(), 64, entry.z()), AprsIcon.PORTABLE, false, false));
			if(map.update(minecraft.level, marks) && random.nextInt(4) == 0)
				seek();
			guiGraphics.blit(map.location(), leftPos + PLOT[0], topPos + PLOT[1], PLOT[2], PLOT[3], 0.0F, 0.0F, width, height, width, height);
		}

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(leftPos + PLOT[0], topPos + PLOT[1], 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		int[] clip = {0, 0, width, height};
		if(!finder.hasMap()) { // A dotted grid, its spacing a round number of blocks.
			double spacing = gridSpacing(viewBpp);
			for(double x = Math.floor((viewX - width / 2.0D * viewBpp) / spacing) * spacing; x <= viewX + width / 2.0D * viewBpp; x += spacing)
				for(int y = 0; y < height; y += 3)
					dotIn(guiGraphics, (int)Math.floor(px(x, width)), y, FAINT, clip);
			for(double z = Math.floor((viewZ - height / 2.0D * viewBpp) / spacing) * spacing; z <= viewZ + height / 2.0D * viewBpp; z += spacing)
				for(int x = 0; x < width; x += 3)
					dotIn(guiGraphics, x, (int)Math.floor(pz(z, height)), FAINT, clip);
		}
		double reach = (width + height) * viewBpp * 2.0D;
		for(int i = 0; i < lines.size(); i++) {
			DirectionFinding.Line line = lines.get(i);
			double sx = px(line.x(), width), sz = pz(line.z(), height);
			double a = Math.toRadians(line.bearing());
			line(guiGraphics, sx, sz, px(line.x() + Math.sin(a) * reach, width), pz(line.z() - Math.cos(a) * reach, height), i == 0 && finder.hasBearing() ? AMBER : DIM, true, clip);
			int cx = (int)Math.floor(sx), cz = (int)Math.floor(sz);
			for(int d = -2; d <= 2; d++) { // The station: a small cross.
				dotIn(guiGraphics, cx + d, cz, AMBER, clip);
				dotIn(guiGraphics, cx, cz + d, AMBER, clip);
			}
		}
		if(fix.found()) {
			int fx = (int)Math.floor(px(fix.x(), width)), fz = (int)Math.floor(pz(fix.z(), height));
			int r = Math.max(3, (int)Math.ceil(fix.radius() / viewBpp));
			boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
			line(guiGraphics, fx - r, fz - r, fx + r, fz - r, AMBER, false, clip);
			line(guiGraphics, fx - r, fz + r, fx + r, fz + r, AMBER, false, clip);
			line(guiGraphics, fx - r, fz - r, fx - r, fz + r, AMBER, false, clip);
			line(guiGraphics, fx + r, fz - r, fx + r, fz + r, AMBER, false, clip);
			if(blink)
				for(int dx = -1; dx <= 1; dx++)
					for(int dz = -1; dz <= 1; dz++)
						dotIn(guiGraphics, fx + dx, fz + dz, AMBER, clip);
		}
		guiGraphics.pose().popPose();
		if(!finder.hasMap())
			text(guiGraphics, "GRID " + Math.round(gridSpacing(viewBpp)), leftPos + PLOT[0] + PLOT[2] - 2, topPos + PLOT[1] + PLOT[3] - 5, TEXT_SCALE, DIM, false);
	}

	private void dotIn(GuiGraphics guiGraphics, int x, int y, int colour, int[] clip) {
		if(x >= clip[0] && x < clip[2] && y >= clip[1] && y < clip[3])
			dot(guiGraphics, x, y, colour);
	}

	private double px(double x, int width) {
		return (x - viewX) / viewBpp + width / 2.0D;
	}

	private double pz(double z, int height) {
		return (z - viewZ) / viewBpp + height / 2.0D;
	}

	/** A round grid spacing (1, 2 or 5 times a power of ten) about 30 half texels apart. */
	private static double gridSpacing(double bpp) {
		double raw = bpp * 30.0D;
		double power = Math.pow(10.0D, Math.floor(Math.log10(raw)));
		for(double step : new double[] { 1.0D, 2.0D, 5.0D, 10.0D })
			if(step * power >= raw)
				return step * power;
		return 10.0D * power;
	}

	/** Frames the stations and the fix (with its box), in steps so a map is not redrawn for every small change. */
	private void fitView(DirectionFinderBlockEntity finder, List<DirectionFinding.Line> lines, DirectionFinding.Fix fix, int width, int height) {
		BlockPos here = finder.getBlockPos();
		double minX = here.getX(), maxX = here.getX(), minZ = here.getZ(), maxZ = here.getZ();
		for(DirectionFinding.Line line : lines) {
			minX = Math.min(minX, line.x());
			maxX = Math.max(maxX, line.x());
			minZ = Math.min(minZ, line.z());
			maxZ = Math.max(maxZ, line.z());
		}
		if(fix.found()) {
			minX = Math.min(minX, fix.x() - fix.radius());
			maxX = Math.max(maxX, fix.x() + fix.radius());
			minZ = Math.min(minZ, fix.z() - fix.radius());
			maxZ = Math.max(maxZ, fix.z() + fix.radius());
		}
		double bpp = Math.max(0.5D, Math.max((maxX - minX) / (width * 0.8D), (maxZ - minZ) / (height * 0.8D)));
		bpp = Math.pow(2.0D, Math.ceil(Math.log(bpp) / Math.log(2.0D))); // Powers of two only.
		double step = bpp * 16.0D; // Centre snapped to a coarse grid.
		viewBpp = bpp;
		viewX = Math.round((minX + maxX) / 2.0D / step) * step;
		viewZ = Math.round((minZ + maxZ) / 2.0D / step) * step;
	}

	/** Lit cells and the black behind text, then the characters, each centred in its cell. */
	private void renderGrid(GuiGraphics guiGraphics) {
		for(int row = 0; row < ROWS; row++) {
			int y = topPos + GRID_Y + row * CELL_H;
			int col = 0;
			while(col < COLS) {
				int colour = attrs[row][col] == INVERSE ? AMBER : opaque[row][col] ? H19Map.BLACK : 0;
				int end = col + 1;
				while(end < COLS && (attrs[row][end] == INVERSE ? AMBER : opaque[row][end] ? H19Map.BLACK : 0) == colour)
					end++;
				if(colour != 0)
					guiGraphics.fill(leftPos + GRID_X + col * CELL_W, y, leftPos + GRID_X + end * CELL_W, y + CELL_H, colour);
				col = end;
			}
		}
		guiGraphics.pose().pushPose();
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
				int colour = attrs[row][col] == INVERSE ? INK : attrs[row][col] == LOW ? DIM : AMBER;
				font.drawInBatch(text, col * cellW + (cellW - font.width(text) + 1) / 2.0F, row * cellH + 1, colour, false, pose,
						guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			}
		}
		guiGraphics.flush();
		guiGraphics.pose().popPose();
	}

	/** The glass: an amber cast, scanlines, and now and then a flicker. */
	private void renderGlass(GuiGraphics guiGraphics) {
		int x0 = leftPos + CRT[0], y0 = topPos + CRT[1];
		guiGraphics.fill(x0 + 1, y0 + 1, x0 + CRT[2] - 1, y0 + CRT[3] - 1, 0x0AFFB020);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x0, y0, 0.0F);
		guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
		for(int y = 2; y < CRT[3] * 2 - 2; y += 2)
			guiGraphics.fill(2, y, CRT[2] * 2 - 2, y + 1, 0x28000000);
		guiGraphics.pose().popPose();
		if(random.nextInt(90) == 0)
			guiGraphics.fill(x0 + 1, y0 + 1, x0 + CRT[2] - 1, y0 + CRT[3] - 1, 0x14000000);
	}
}

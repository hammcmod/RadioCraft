package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.TeletypeMenu;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Teletype Machine (was the solar weather station): an RTTY teleprinter with its own terminal unit, so it works
 * straight through the radios it touches, no radio interface. What is typed goes out a character every {@link #SECONDS_PER_CHAR}
 * characters a second, as the real machine sends, and prints here as it goes; RTTY heard prints a line per sender, a
 * sheet of paper every {@link #LINES_PER_PAPER} lines. Nothing identifies the sender on the paper: as on the air,
 * operators give their callsign in what they type. (The teletype's callsign only tells stations apart on the air.) It cannot print what it hears while
 * it is sending or being typed on (half duplex), and two stations sending at once on a frequency garble each other
 * (see {@code RadioBlockEntity#receiveData}). Moderators listening in see each line sent, with who typed it. SOLAR WX prints the solar weather, which it reads through a radio with a real
 * antenna next to it (the comparator gives the same, 1 for a blackout to 6 for great).
 */
public class TeletypeBlockEntity extends BlockEntity implements MenuProvider {

	/** The band the station reports on by name, the one the proposal calls out as opening and closing with the weather. */
	public static final String REPORTED_BAND = "10m";
	/** The HF bands reported, in order. VHF is reported first as "Normal" since the sun does not touch it. */
	public static final List<String> REPORTED_BANDS = List.of("10m", "20m", "40m", "80m");
	/** RTTY lines one sheet of paper takes. */
	public static final int LINES_PER_PAPER = 20;
	/** RTTY lines kept on the roll (scroll back through them, or tear the roll off and keep it). */
	public static final int RTTY_HISTORY = 400;
	/** Characters across the paper, the sender's callsign included; the printer starts a new line by itself after. */
	public static final int RTTY_LINE_MAX = 32;
	/** Who report lines print as. */
	public static final String WX = "WX";
	/** Characters a second it sends (RTTY at 45.45 baud manages about six). */
	public static final double SECONDS_PER_CHAR = 0.158D; // The shortest keystroke sound (teletype_sound-08), so they do not overlap: about 6.3 a second.
	/** Most characters typed ahead of what has gone out. */
	public static final int TYPE_AHEAD = 256;
	/** Ticks after the last key or character sent that it still cannot print what it hears. */
	private static final int RX_BLOCKED_TICKS = 20;
	/** Ticks the receiving lamp (and whir) stay on after a character heard. */
	private static final int RECEIVING_TICKS = 20;

	@Nullable
	private SolarEventInstance lastSeenEvent;
	private boolean lastSeenRadio;

	private String callsign = "";
	private String callsignSetBy = "";
	private final ItemStackHandler paper = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return stack.is(Items.PAPER);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sync();
		}
	};
	private int paperCredit = 0; // Lines left on the sheet in the printer.
	private final List<String> rttyLines = new ArrayList<>(); // Oldest first.
	private String rttyOpenFrom = null; // Who the line being printed is from; null when no line is open.
	private final StringBuilder rttyOpen = new StringBuilder();
	private long lastPrintSoundGameTime = -1000L;
	private boolean syncPending = false;
	private int fedLines = 0; // Line feeds on the paper in the machine now; 0 for a fresh roll.
	private boolean outOfPaper = true; // No paper threaded: a fresh roll is fed up to the head first (see loadPaper).
	private final StringBuilder typedAhead = new StringBuilder(); // Typed, not yet sent.

	// Server only, not saved.
	private long lastTypedGameTime = -1000L;
	private long secondKeyAt = -1L; // The second keystroke sound of the last character, a little after the first.
	private long receivingUntil = -1L;
	private boolean wasReceiving = false;
	@Nullable
	private java.util.UUID typist = null;
	private String typistName = "";
	private final StringBuilder sentLine = new StringBuilder(); // For moderators: the line going out.
	private float sentFrequency = 0.0F;

	// Server only, not saved.
	private final SeenPackets seen = new SeenPackets();
	/** How long SOLAR WX waits for an Ionospheric Processor to answer over the air. */
	public static final int REQUEST_TICKS = 300;
	private long requestUntil = -1L; // SOLAR WX asked over the air: until when it waits.

	public TeletypeBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.TELETYPE.get(), pos, state);
	}

	private static long charSlot(long gameTime) {
		return (long)Math.floor(gameTime / 20.0D / SECONDS_PER_CHAR);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TeletypeBlockEntity be) {
		if(!level.isClientSide) {
			long now = level.getGameTime(); // A character each time the count of character times goes up: every 3 or 4 ticks.
			if(!be.typedAhead.isEmpty() && charSlot(now) != charSlot(now - 1))
				be.sendNext(level);
			if(now == be.secondKeyAt)
				be.keySound();
			if(be.requestUntil >= 0L && now >= be.requestUntil) { // Nobody answered.
				be.requestUntil = -1L;
				be.print(WX, "NO ANSWER\n");
			}
			if(be.isReceiving() && now % MOTOR_TICKS == 0 && level instanceof net.minecraft.server.level.ServerLevel serverLevel)
				be.motorSound(serverLevel);
			boolean receiving = be.isReceiving();
			if(receiving != be.wasReceiving) {
				be.wasReceiving = receiving;
				be.sync();
			}
		}
		if(be.syncPending && level.getGameTime() % 2 == 0) { // At most ten updates a second, however fast RTTY prints.
			be.syncPending = false;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		SolarEventInstance current = SolarEventManager.getEvent(level);
		boolean radio = level.getGameTime() % 10 == 0 ? be.hasRadio() : be.lastSeenRadio;
		if(current != be.lastSeenEvent || radio != be.lastSeenRadio) {
			be.lastSeenEvent = current;
			be.lastSeenRadio = radio;
			level.updateNeighbourForOutputSignal(pos, state.getBlock()); // Comparators only re-read when told to.
		}
	}

	// -------------------- RTTY --------------------

	/** Radios on any side of the teletype, or on its data wire. */
	public List<RadioBlockEntity> getRadios() {
		if(level == null)
			return List.of();
		return com.arrl.radiocraft.common.be_networks.DataWire.linked(level, worldPosition, RadioBlockEntity.class);
	}

	/**
	 * {@code player} typing: the characters wait their turn to go out, one every {@link #SECONDS_PER_CHAR}. A teletype with no
	 * callsign takes theirs. False if there is still no callsign, nothing printable, or no radio on to send it.
	 */
	public boolean type(ServerPlayer player, String text) {
		if(!typeRtty(text))
			return false;
		typist = player.getUUID();
		typistName = player.getGameProfile().getName();
		return true;
	}

	/** Who it sends as: its callsign, or failing that a tag for this machine (so stations still tell senders apart). */
	private String sender() {
		return callsign.isEmpty() ? "TT" + Integer.toHexString(worldPosition.hashCode() & 0xFFFFFF).toUpperCase(java.util.Locale.ROOT) : callsign;
	}

	/**
	 * Types {@code text}: it goes out a character every {@link #SECONDS_PER_CHAR}, printing here as it does (the local
	 * copy). False if
	 * there is nothing printable or no radio on to send it.
	 */
	public boolean typeRtty(String text) {
		if(level == null || getRadios().stream().noneMatch(RadioBlockEntity::isPowered))
			return false;
		String clean = Rtty.sanitize(text, TncPacket.RTTY_TEXT_MAX);
		if(clean.isEmpty())
			return false;
		long now = level.getGameTime();
		typedAhead.append(clean, 0, Math.min(clean.length(), TYPE_AHEAD - typedAhead.length()));
		lastTypedGameTime = now;
		sync();
		return true;
	}

	/** Sends the next character typed, prints it here, and gives moderators each line as it ends. */
	private void sendNext(Level level) {
		String c = typedAhead.substring(0, 1);
		typedAhead.deleteCharAt(0);
		lastTypedGameTime = level.getGameTime();
		TncPacket packet = TncPacket.rtty(level.getRandom().nextLong(), sender(), c);
		seen.remember(packet.id());
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios()) {
			if(radio.sendData(packet)) {
				sent = true;
				sentFrequency = radio.getFrequency();
			}
		}
		if(!sent) { // The radio went off: what was typed is lost.
			typedAhead.setLength(0);
			sync();
			return;
		}
		print(sender(), c);
		if(c.charAt(0) == '\n' || sentLine.length() >= RTTY_LINE_MAX)
			lineSent(level);
		if(c.charAt(0) != '\n')
			sentLine.append(c);
		if(typedAhead.isEmpty() && !sentLine.isEmpty())
			lineSent(level); // A pause in the typing: what there is so far.
		sync();
	}

	private void lineSent(Level level) {
		if(!sentLine.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel serverLevel)
			com.arrl.radiocraft.common.radio.voice.RadioModeration.rttyLine(serverLevel, sentFrequency, typist, typistName,
					typist == null ? "" : java.util.Objects.requireNonNullElse(com.arrl.radiocraft.common.radio.Callsigns.playerCallsign(serverLevel.getServer(), typist), ""),
					sentLine.toString());
		sentLine.setLength(0);
	}

	/** Characters typed that have not gone out yet. */
	public int getTypedAhead() {
		return typedAhead.length();
	}

	/** True for a second after it printed something it heard (its motor runs). */
	public boolean isReceiving() {
		return level != null && level.getGameTime() < receivingUntil;
	}

	/**
	 * A packet a radio next to it decoded: RTTY prints, unless it is sending or being typed on (half duplex); a solar
	 * bulletin prints if SOLAR WX is waiting for one.
	 */
	public void receive(TncPacket packet) {
		if(packet.kind() == TncPacket.Kind.SOLAR && requestUntil >= 0L && level != null && seen.remember(packet.id())) {
			requestUntil = -1L;
			printLines(packet.from(), packet.solarLines());
			return;
		}
		if(packet.kind() != TncPacket.Kind.RTTY || level == null || !seen.remember(packet.id()))
			return;
		long now = level.getGameTime();
		if(!typedAhead.isEmpty() || now - lastTypedGameTime < RX_BLOCKED_TICKS)
			return;
		receivingUntil = now + RECEIVING_TICKS;
		print(packet.from(), packet.text());
	}

	/**
	 * SOLAR WX. With the server's {@code solar_weather_station_needs_radio} off, the local report; with an Ionospheric
	 * Processor beside it (or beside one of its radios), that processor's bulletin straight away; otherwise it asks over
	 * the air through its radios and prints what a processor answers, or NO ANSWER after {@link #REQUEST_TICKS}.
	 */
	public void printReport() {
		if(level == null)
			return;
		if(!RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get()) {
			for(Component line : getReportLines())
				print(WX, Rtty.sanitize(line.getString(), RTTY_LINE_MAX) + "\n");
			return;
		}
		IonosphericProcessorBlockEntity processor = IonosphericProcessorBlockEntity.nearby(level, worldPosition, getRadios().stream().map(BlockEntity::getBlockPos).toList());
		if(processor != null) {
			if(processor.isOnline())
				printLines(processor.sender(), processor.bulletinLines());
			else
				print(WX, "PROCESSOR HAS NO IONOSONDE\n");
			return;
		}
		if(requestUntil >= 0L)
			return; // Still waiting for the last one.
		TncPacket query = TncPacket.query(level.getRandom().nextLong(), sender(), TncPacket.TOPIC_SOLAR);
		seen.remember(query.id());
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.isPowered() && radio.sendData(query);
		if(!sent) {
			print(WX, "NO RADIO ON\n");
			return;
		}
		requestUntil = level.getGameTime() + REQUEST_TICKS;
		print(WX, "QRU SOLAR?\n");
	}

	/** True while SOLAR WX waits for an answer over the air. */
	public boolean isRequesting() {
		return requestUntil >= 0L;
	}

	/** Prints a report from {@code from}, a line each. */
	private void printLines(String from, List<String> lines) {
		for(String line : lines)
			print(from, Rtty.sanitize(line, RTTY_LINE_MAX) + "\n");
	}

	/**
	 * Prints RTTY from {@code from}: a line per sender, a newline ends it. Starting a line takes paper; with none, the
	 * characters are lost until there is some.
	 */
	private void print(String from, String text) {
		boolean printed = false;
		for(char c : text.toCharArray()) {
			if(c == '\n') { // Carriage return and line feed: the bell, and a blank line if nothing was printed on this one.
				if(rttyOpenFrom == null && takePaperLine()) {
					rttyOpenFrom = from;
					rttyOpen.setLength(0);
				}
				if(rttyOpenFrom != null)
					endLine(true);
				else if(level != null) // No paper: the bell still rings.
					level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
				printed = true;
				continue;
			}
			if(rttyOpenFrom == null || !rttyOpenFrom.equals(from) || rttyOpen.length() >= RTTY_LINE_MAX) {
				if(rttyOpenFrom != null)
					endLine(false);
				if(!takePaperLine())
					continue;
				rttyOpenFrom = from;
				rttyOpen.setLength(0);
			}
			rttyOpen.append(c);
			printed = true;
		}
		if(printed && level != null && level.getGameTime() != lastPrintSoundGameTime) { // Two keystrokes per character, so they run together.
			lastPrintSoundGameTime = level.getGameTime();
			keySound();
			secondKeyAt = level.getGameTime() + 2;
		}
		setChanged();
		sync();
	}

	/** Ticks of teletype_background (0.46 s): played again each time it runs out while the machine prints. */
	private static final int MOTOR_TICKS = 9;

	/**
	 * The motor running while it prints what it hears, for everyone near but whoever has its screen open: the screen
	 * runs the motor for them.
	 */
	private void motorSound(net.minecraft.server.level.ServerLevel level) {
		net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound = net.minecraft.core.Holder.direct(RadiocraftSoundEvents.TELETYPE_BACKGROUND.get());
		long seed = level.getRandom().nextLong();
		for(ServerPlayer player : level.players()) {
			if(player.distanceToSqr(worldPosition.getCenter()) > 16.0D * 16.0D)
				continue;
			if(player.containerMenu instanceof TeletypeMenu menu && menu.blockEntity == this)
				continue;
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, SoundSource.BLOCKS,
					worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 0.3F, 1.0F, seed));
		}
	}

	private void keySound() {
		if(level != null)
			level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_KEY.get(), SoundSource.BLOCKS, 0.7F, 0.95F + level.getRandom().nextFloat() * 0.1F);
	}

	private void endLine(boolean bell) {
		rttyLines.add(rttyOpen.toString());
		while(rttyLines.size() > RTTY_HISTORY)
			rttyLines.removeFirst();
		rttyOpenFrom = null;
		rttyOpen.setLength(0);
		fedLines++;
		if(level != null) { // The paper slides up a line.
			level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.5F, 1.5F + level.getRandom().nextFloat() * 0.2F);
			if(bell)
				level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
		}
	}

	/** Takes one line's worth of paper: off the sheet in the printer, or a new sheet from the supply. */
	private boolean takePaperLine() {
		if(paperCredit <= 0) {
			if(paper.extractItem(0, 1, false).isEmpty()) {
				outOfPaper = true;
				return false;
			}
			paperCredit = LINES_PER_PAPER;
			if(outOfPaper) { // A fresh roll threaded in.
				outOfPaper = false;
				fedLines = 0;
			}
		}
		paperCredit--;
		return true;
	}

	/**
	 * Tears the printed roll off (sneak and right click with an empty hand): everything printed goes with it and the
	 * machine starts a fresh roll. Empty if nothing was printed.
	 */
	public ItemStack tearOff() {
		if(rttyLines.isEmpty())
			return ItemStack.EMPTY;
		ItemStack roll = com.arrl.radiocraft.common.items.TeletypeRollItem.of(com.arrl.radiocraft.common.init.RadiocraftItems.TELETYPE_ROLL.get(), rttyLines, fedLines);
		rttyLines.clear();
		fedLines = 0;
		if(level != null)
			level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 0.6F);
		setChanged();
		sync();
		return roll;
	}

	/** Feeds a torn-off roll back in, when nothing is on the machine's own. */
	public void loadRoll(com.arrl.radiocraft.common.datacomponents.TeletypeRollContents contents) {
		if(!rttyLines.isEmpty())
			return;
		rttyLines.addAll(contents.lines());
		while(rttyLines.size() > RTTY_HISTORY)
			rttyLines.removeFirst();
		fedLines = Math.max(contents.fedLines(), rttyLines.size());
		setChanged();
		sync();
	}

	/** True once paper is threaded up to the print head; false new, and again after it ran out. */
	public boolean isPaperLoaded() {
		return !outOfPaper;
	}

	/** Threads fresh paper up to the print head (the screen feeds it up first). False if there is none. */
	public boolean loadPaper() {
		if(!outOfPaper)
			return true;
		if(getPaperLines() <= 0)
			return false;
		outOfPaper = false;
		fedLines = 0;
		setChanged();
		sync();
		return true;
	}

	/** RTTY lines the paper on hand will print: what is left of the sheet in the printer plus the supply. */
	public int getPaperLines() {
		return paperCredit + paper.getStackInSlot(0).getCount() * LINES_PER_PAPER;
	}

	/** Line feeds on the paper in the machine (the screen slides the paper by these); 0 for a fresh roll. */
	public int getFedLines() {
		return fedLines;
	}

	/** Printed RTTY lines, oldest first, not counting the one being printed. */
	public List<String> getRttyLines() {
		return List.copyOf(rttyLines);
	}

	/** The line being printed, or null when none is open. */
	@Nullable
	public String getRttyOpenLine() {
		return rttyOpenFrom == null ? null : rttyOpen.toString();
	}

	public ItemStackHandler getPaper() {
		return paper;
	}

	/** Puts as much of {@code stack} as fits into the paper supply and returns what is left. */
	public ItemStack insertPaper(ItemStack stack) {
		return paper.insertItem(0, stack, false);
	}

	public void dropPaper() {
		if(level != null)
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), paper.getStackInSlot(0));
	}

	// -------------------- CALLSIGN --------------------

	/** Sets the teletype to {@code player}'s callsign. False (and nothing changes) if they have none. */
	public boolean claim(ServerPlayer player) {
		PlayerCallsignData data = PlayerCallsignSavedData.get(player.serverLevel().getServer().overworld()).getCallsignData(player.getUUID());
		if(data == null || data.callsign() == null || data.callsign().isBlank())
			return false;
		setCallsign(data.callsign(), player.getGameProfile().getName());
		return true;
	}

	public void setCallsign(String value, String setBy) {
		callsign = TncPacket.sanitizeCallsign(value);
		callsignSetBy = setBy == null ? "" : setBy;
		setChanged();
		sync();
	}

	public String getCallsign() {
		return callsign;
	}

	public String getCallsignSetBy() {
		return callsignSetBy;
	}

	// -------------------- SOLAR WEATHER --------------------

	/** The solar event in this station's dimension, null before the manager has picked one. */
	@Nullable
	public SolarEvent getCurrentEvent() {
		return level == null ? null : currentEvent(level);
	}

	@Nullable
	private static SolarEvent currentEvent(Level level) {
		SolarEventInstance instance = SolarEventManager.getEvent(level);
		return instance == null ? null : instance.getEvent();
	}

	/** Data pack id of the current event ("radiocraft:good"), null if none. */
	@Nullable
	public ResourceLocation getCurrentEventKey() {
		SolarEvent event = getCurrentEvent();
		return event == null ? null : RadiocraftData.SOLAR_EVENTS.getKey(event);
	}

	/**
	 * Like a real station, it hears the solar weather through a radio: one of any kind next to it, with a real antenna
	 * on its coax (a dummy load radiates nothing, so it does not count). Always true when the server config turns this
	 * off.
	 */
	public boolean hasRadio() {
		if(!RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get())
			return true;
		if(level == null)
			return false;
		for(Direction direction : Direction.values()) {
			if(IBENetworks.getObject(level, worldPosition.relative(direction)) instanceof RadioNetworkObject radio
					&& radio.getAntennas().stream().anyMatch(antenna -> !(antenna instanceof DummyLoadNetworkObject) && antenna.getAntenna() != null))
				return true;
		}
		return false;
	}

	/**
	 * Comparator output: 1 for the noisiest conditions (blackout) up to 6 for the quietest (great), 0 with no event or
	 * without a radio ({@link #hasRadio()}).
	 */
	public int getConditionLevel() {
		return hasRadio() ? RadiocraftData.SOLAR_EVENTS.conditionLevel(getCurrentEvent()) : 0;
	}

	/**
	 * How {@code band} carries skip here right now, from the event and the time of day, one step worse under an aurora;
	 * null with no event.
	 */
	@Nullable
	public BandCondition getBandCondition(String band) {
		return level == null ? null : bandCondition(level, worldPosition, band);
	}

	@Nullable
	private static BandCondition bandCondition(Level level, BlockPos pos, String band) {
		SolarEvent event = currentEvent(level);
		if(event == null)
			return null;
		BandCondition condition = event.getCondition(band, level.isDay());
		return Aurora.affects(level, pos) ? condition.worse() : condition;
	}

	/** Whether {@link #REPORTED_BAND} propagates at all right now (any condition but closed). */
	public boolean isReportedBandOpen() {
		BandCondition condition = getBandCondition(REPORTED_BAND);
		return condition != null && condition != BandCondition.CLOSED;
	}

	/** "Aurora: active beyond Z ±10000" or "Aurora expected ...", null when there is none. */
	@Nullable
	public Component auroraLine() {
		return level == null ? null : auroraLine(level);
	}

	@Nullable
	private static Component auroraLine(Level level) {
		int latitude = Aurora.latitude();
		if(Aurora.isActive(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_active", latitude).withStyle(ChatFormatting.GREEN);
		if(Aurora.isExpected(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_expected", latitude).withStyle(ChatFormatting.LIGHT_PURPLE);
		if(com.arrl.radiocraft.common.radio.solar.SporadicE.isActive(level))
			return Component.translatable("message.radiocraft.solar_weather_station.sporadic_e").withStyle(ChatFormatting.AQUA);
		return null;
	}

	/** "Good", "Fair", "Poor" or "Closed". */
	public static MutableComponent conditionName(BandCondition condition) {
		return Component.translatable("message.radiocraft.solar_weather_station." + condition.getSerializedName());
	}

	/** The event's display name: a translation if the pack has one, else the id's path with a capital ("Good"). */
	public static MutableComponent eventName(@Nullable ResourceLocation key) {
		if(key == null)
			return Component.translatable("solar_event.radiocraft.none");
		String path = key.getPath();
		String fallback = path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
		return Component.translatableWithFallback("solar_event." + key.getNamespace() + "." + path, fallback);
	}

	/** The report, one line per entry: header, overall conditions, noise floor, then each band. */
	public List<Component> getReportLines() {
		if(!hasRadio())
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_radio"));
		return level == null ? List.of() : reportLines(level, worldPosition);
	}

	/**
	 * The solar weather report at {@code pos}, one line per entry: header, overall conditions, noise floor, then each
	 * band. Shared with the WEFAX receiver's SOLR WX.
	 */
	public static List<Component> reportLines(Level level, BlockPos worldPosition) {
		SolarEvent event = currentEvent(level);
		if(event == null)
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_data"));
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.overall", eventName(RadiocraftData.SOLAR_EVENTS.getKey(event))));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.noise", Math.round(event.getNoise() * 100.0F)));
		boolean underAurora = Aurora.affects(level, worldPosition);
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", "VHF",
				Component.translatable(underAurora ? "message.radiocraft.solar_weather_station.aurora" : "message.radiocraft.solar_weather_station.normal")));
		for(String band : REPORTED_BANDS)
			lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", band, conditionName(bandCondition(level, worldPosition, band))));
		Component aurora = auroraLine(level);
		if(aurora != null)
			lines.add(aurora);
		SolarEventManager.Upcoming upcoming = SolarEventManager.getUpcoming(level);
		if(upcoming != null && SolarEventManager.isBlackout(upcoming.event()) && upcoming.startGameTime() > level.getGameTime())
			lines.add(Component.translatable("message.radiocraft.solar_weather_station.blackout_expected",
					(long)Math.ceil((upcoming.startGameTime() - level.getGameTime()) / 1200.0D)).withStyle(ChatFormatting.RED));
		return lines;
	}

	/** The one-line report, kept for tools and tests; SOLAR WX prints {@link #getReportLines()}. */
	public Component getReport() {
		if(!hasRadio())
			return Component.translatable("message.radiocraft.solar_weather_station.no_radio");
		SolarEvent event = getCurrentEvent();
		if(event == null)
			return Component.translatable("message.radiocraft.solar_weather_station.no_data");
		return Component.translatable("message.radiocraft.solar_weather_station.report",
				eventName(getCurrentEventKey()),
				Math.round(event.getNoise() * 100.0F),
				REPORTED_BAND,
				conditionName(getBandCondition(REPORTED_BAND)));
	}

	// -------------------- MENU, SAVE & SYNC --------------------

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("container.radiocraft.teletype");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new TeletypeMenu(id, this);
	}

	/** Sends the new state to clients on the next even tick, so a burst of changes goes as one update. */
	private void sync() {
		if(level != null && !level.isClientSide)
			syncPending = true;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("callsign", callsign);
		tag.putString("callsignSetBy", callsignSetBy);
		tag.put("paper", paper.serializeNBT(registries));
		tag.putInt("paperCredit", paperCredit);
		tag.putInt("fedLines", fedLines);
		tag.putBoolean("outOfPaper", outOfPaper);
		tag.putString("typedAhead", typedAhead.toString());
		ListTag rttyTags = new ListTag();
		for(String line : rttyLines)
			rttyTags.add(StringTag.valueOf(line));
		tag.put("rtty", rttyTags);
		if(rttyOpenFrom != null) {
			tag.putString("rttyOpenFrom", rttyOpenFrom);
			tag.putString("rttyOpen", rttyOpen.toString());
		}
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		callsign = TncPacket.sanitizeCallsign(tag.getString("callsign"));
		callsignSetBy = tag.getString("callsignSetBy");
		if(tag.contains("paper"))
			paper.deserializeNBT(registries, tag.getCompound("paper"));
		paperCredit = tag.getInt("paperCredit");
		fedLines = tag.getInt("fedLines");
		outOfPaper = !tag.contains("outOfPaper") || tag.getBoolean("outOfPaper");
		typedAhead.setLength(0);
		typedAhead.append(tag.getString("typedAhead"));
		receivingUntil = tag.getBoolean("receiving") && level != null ? level.getGameTime() + RECEIVING_TICKS : receivingUntil; // Client copy.
		rttyLines.clear();
		for(Tag entry : tag.getList("rtty", Tag.TAG_STRING))
			rttyLines.add(entry.getAsString());
		rttyOpenFrom = tag.contains("rttyOpenFrom") ? tag.getString("rttyOpenFrom") : null;
		rttyOpen.setLength(0);
		rttyOpen.append(tag.getString("rttyOpen"));
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("receiving", isReceiving());
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}

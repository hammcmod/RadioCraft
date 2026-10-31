package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.weather.StormReport;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.WefaxReceiverBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import com.arrl.radiocraft.common.menus.WefaxReceiverMenu;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.wefax.WefaxChart;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import com.arrl.radiocraft.common.radio.wefax.WefaxPrints;
import com.arrl.radiocraft.common.radio.voice.WefaxAudio;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
 * A WEFAX receiver: through the radio next to it (one with a real antenna, as the teletype needs) it prints weather
 * charts, a sheet of paper each. WX REPT prints the next chart of the broadcast schedule (radar, forecast, surface
 * analysis, round again); SOLR WX the last solar bulletin an Ionospheric Processor sent that it heard (through its radio,
 * the station gear, or straight from a processor beside it; see {@link #receive}); AUTO prints a forecast by itself when dangerous weather is
 * on its way (at most every {@link #AUTO_GAP_TICKS}). A sheet takes as long to print as a weather fax takes to send
 * ({@code wefax_bulletin_seconds}), each line printed as its tones come in ({@link WefaxAudio}): from a radio that
 * stops copying the signal part way, those lines come out as speckle. WX REPT comes from a coast station off the map:
 * its tones play through the radio beside the receiver, which shows it receiving.
 */
public class WefaxReceiverBlockEntity extends BlockEntity implements MenuProvider, StationTerminal {

	/** Where the sheet being printed comes from. */
	public enum Source {
		/** Straight from a processor beside it, or the server's always-works report: no radio involved. */
		LOCAL,
		/** Over the air from a station in the world: lines print clean only while a radio beside it copies the tones. */
		OVER_AIR,
		/** The coast station's broadcast: the tones play through the radios beside it. */
		BROADCAST
	}
	public static final int AUTO_GAP_TICKS = 5 * 60 * 20;
	public static final int PAPER_MAX = 64;
	/** How long SOLR WX waits for an Ionospheric Processor to answer over the air. */
	/** How long SOLR WX waits for an answer: the query's burst, the processor's fax-tone bulletin ({@code wefax_bulletin_seconds}) and some slack. */
	public static final int REQUEST_TICKS = 900;
	/** Alerts closer together than this are one warning: printed once. */
	public static final int ALERT_GAP_TICKS = 2400;
	/** Blocks around the station the forecast sheet lists storms in. */
	public static final int FORECAST_RANGE = 1500;
	/** The sheet to print once the one in hand is out (WX REPT prints two). */
	@Nullable
	private WefaxPrints.Print queuedPrint = null;
	/** A printed sheet waiting in the tray (shown on the screen and the model) until someone takes it. */
	private ItemStack ready = ItemStack.EMPTY;
	/**
	 * The redstone signal (maintainer, 2026-10-30): 1 for a message, 2 for a severe thunderstorm, 3 for a tornado watch,
	 * 4 for a hurricane. It comes on when a message starts printing (so only with paper), stays at the highest of the
	 * messages since, and goes off when the printed sheet is taken from the tray.
	 */
	private int signal;
	/** The signal of the sheet being printed, which is what is left when the one before it is taken. */
	private int printingSignal;

	private final ItemStackHandler paper = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return stack.is(Items.PAPER);
		}

		@Override
		protected void onContentsChanged(int slot) {
			sync();
		}
	};
	private boolean auto;
	private int scheduleIndex;
	private long lastAuto = -AUTO_GAP_TICKS;
	// The sheet being printed: its picture (synced, for the screen), when it began, and the item it becomes.
	private byte[] pixels = new byte[0];
	private long printStart = -1L;
	private ItemStack printing = ItemStack.EMPTY;
	private int printTicks = 240;
	private Source printSource = Source.LOCAL;
	private Source queuedSource = Source.LOCAL;
	/** Lines of the sheet in hand printed so far, and whether any came out as speckle. */
	private int rowsDone = 0;
	private boolean anyLost = false;
	private boolean pixelsDirty = false;
	/** The broadcast's tones, made when it starts (not saved: a reloaded broadcast prints on in silence). */
	@Nullable
	private List<short[]> broadcastFrames = null;
	private int broadcastFrame = 0;
	// The last solar bulletin heard: who sent it, its lines, when, and its id (each arrives once per radio that heard it).
	private String solarFrom = "";
	private List<String> solarLines = List.of();
	private long solarHeardAt = -1L;
	private long lastPacketId = 0L;
	// SOLR WX asked over the air: until when it waits for an answer, and who to tell if none comes.
	private long requestUntil = -1L;
	@Nullable
	private java.util.UUID requester = null;
	// Blackout alerts: when the last was heard, its beeps, and one waiting for the printer.
	private long lastAlertAt = -1L;
	private long beepsFrom = -100L;
	@Nullable
	private List<String> queuedAlert = null;

	public WefaxReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.WEFAX_RECEIVER.get(), pos, state);
	}

	// -------------------- PAPER --------------------

	/** Puts as much of {@code stack} as fits into the paper supply and returns what is left. */
	public ItemStack insertPaper(ItemStack stack) {
		return paper.insertItem(0, stack, false);
	}

	public int getPaper() {
		return paper.getStackInSlot(0).getCount();
	}

	public void dropContents() {
		if(level == null)
			return;
		Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), paper.getStackInSlot(0));
		if(!printing.isEmpty())
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), printing);
		if(!ready.isEmpty() && level != null)
			Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, ready);
	}

	// -------------------- RADIO --------------------

	/**
	 * A radio with a real antenna on its coax, next to it or reached through the station gear it touches (a TNC and
	 * terminals beside it, see {@link StationBus}). Always true with the teletype's needs-radio rule off.
	 */
	public boolean hasRadio() {
		if(!RadiocraftServerConfig.TELETYPE_NEEDS_RADIO.get())
			return true;
		if(level == null)
			return false;
		for(Direction direction : Direction.values())
			if(hasAntenna(worldPosition.relative(direction)))
				return true;
		for(RadioInterfaceBlockEntity radioInterface : StationBus.find(level, worldPosition).interfaces())
			for(com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity radio : radioInterface.getRadios())
				if(hasAntenna(radio.getBlockPos()))
					return true;
		return false;
	}

	/** True if a radio at {@code pos} has a real antenna (not a dummy load) on its coax. */
	private boolean hasAntenna(BlockPos pos) {
		return IBENetworks.getObject(level, pos) instanceof RadioNetworkObject radio
				&& radio.getAntennas().stream().anyMatch(antenna -> !(antenna instanceof DummyLoadNetworkObject) && antenna.getAntenna() != null);
	}

	// -------------------- PRINTING --------------------

	public boolean isPrinting() {
		return printStart >= 0;
	}

	/** Whether it receives the solar report or the weather charts: with a radio beside it, or always if the server says so. */
	public boolean receives(boolean solar) {
		return (solar ? RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS : RadiocraftServerConfig.WEFAX_WEATHER_ALWAYS).get() || hasRadio();
	}

	/** Why it cannot print the solar report or a weather chart now (a translation key), or null if it can. */
	@Nullable
	public String cannotPrint(boolean solar) {
		if(isPrinting() || isRequesting())
			return "screen.radiocraft.wefax.busy";
		if(!solar && !receives(false))
			return "screen.radiocraft.wefax.no_radio";
		if(getPaper() <= 0)
			return "screen.radiocraft.wefax.no_paper";
		if(solar && !RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.get()) {
			IonosphericProcessorBlockEntity processor = localProcessor();
			if(processor != null)
				return processor.isOnline() ? null : "screen.radiocraft.wefax.processor_offline";
			if(getRadios().stream().noneMatch(RadioBlockEntity::isPowered))
				return "screen.radiocraft.wefax.no_radio";
		}
		return null;
	}

	/**
	 * WX REPT: two sheets, the radar chart (rain, each storm named and numbered with an arrow where it is going, YOU in
	 * the middle) and then a forecast sheet (the report, and every storm by number: how far and which way, where it is
	 * heading, when it arrives). The forecast map and the surface analysis are still there for {@code /rcdebug wefax}.
	 */
	public void printWeather() {
		if(!(level instanceof ServerLevel serverLevel) || cannotPrint(false) != null)
			return;
		WeatherSource source = WeatherSources.get(serverLevel);
		BlockPos here = com.arrl.radiocraft.compat.sable.Sublevels.world(serverLevel, worldPosition); // On a Sable ship: the weather where the ship is.
		List<String> lines = new ArrayList<>(WeatherReports.lines(serverLevel, source, here, ""));
		lines.add("");
		lines.addAll(WeatherReports.stormList(serverLevel, source, here, FORECAST_RANGE));
		queuedPrint = WefaxPrints.printText(serverLevel, here, Component.translatable("item.radiocraft.wefax_chart.forecast"), "FORECAST", lines);
		queuedSource = Source.BROADCAST;
		start(WefaxPrints.printChart(serverLevel, source, here, WefaxChart.Type.RADAR, ""), alertLevel(serverLevel, source, here), Source.BROADCAST);
	}

	/**
	 * The redstone level a weather report from {@code pos} gives: 4 for a hurricane, 3 for a tornado or a tornado
	 * watch, 2 for a severe thunderstorm (or a supercell, hail or damaging wind), else 1. Storms within
	 * {@link #FORECAST_RANGE}, forecast ones included, count.
	 */
	public static int alertLevel(ServerLevel level, WeatherSource source, BlockPos pos) {
		WeatherReport now = source.report(level, pos);
		if(now.has(WeatherReport.Hazard.HURRICANE_EYE))
			return 4;
		int alert = 1;
		for(StormReport storm : source.storms(level, pos, FORECAST_RANGE, true)) {
			switch(storm.kind()) {
				case HURRICANE -> { return 4; }
				case TORNADO -> alert = Math.max(alert, 3);
				case SEVERE_THUNDERSTORM, SUPERCELL -> alert = Math.max(alert, 2);
				default -> {}
			}
		}
		if(now.has(WeatherReport.Hazard.TORNADO) || source.tornadoWatch(level, pos))
			alert = Math.max(alert, 3);
		if(now.has(WeatherReport.Hazard.DAMAGING_WIND) || now.has(WeatherReport.Hazard.HAIL))
			alert = Math.max(alert, 2);
		return alert;
	}

	/** The redstone signal, 0 to 4 (see {@link #signal}). */
	public int getSignal() {
		return signal;
	}

	private void setSignal(int value) {
		if(signal == value)
			return;
		signal = value;
		setChanged();
		if(level != null && !level.isClientSide)
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
	}

	/** Radios beside it or on its data wire, and those reached through the station gear it is linked to. */
	public List<RadioBlockEntity> getRadios() {
		List<RadioBlockEntity> radios = new ArrayList<>();
		if(level == null)
			return radios;
		radios.addAll(com.arrl.radiocraft.common.be_networks.DataWire.linked(level, worldPosition, RadioBlockEntity.class));
		for(RadioInterfaceBlockEntity radioInterface : StationBus.find(level, worldPosition).interfaces())
			for(RadioBlockEntity radio : radioInterface.getRadios())
				if(!radios.contains(radio))
					radios.add(radio);
		return radios;
	}

	/** An Ionospheric Processor touching it or one of its radios: SOLR WX reads it straight off, null if there is none. */
	@Nullable
	public IonosphericProcessorBlockEntity localProcessor() {
		return level == null ? null : IonosphericProcessorBlockEntity.nearby(level, worldPosition, getRadios().stream().map(BlockEntity::getBlockPos).toList());
	}

	/**
	 * SOLR WX. With the server's {@code wefax_solar_report_always_works} on, the local report; with an Ionospheric
	 * Processor beside it (or its radio), that processor's bulletin straight away; otherwise it asks over the air (a
	 * {@link TncPacket.Kind#QUERY}) and prints the bulletin a processor answers with, or gives up after
	 * {@link #REQUEST_TICKS} with a low beep.
	 *
	 * @param player Who pressed it, told if nothing answers; may be null.
	 */
	public void printSolar(@Nullable ServerPlayer player) {
		if(!(level instanceof ServerLevel serverLevel) || cannotPrint(true) != null)
			return;
		if(RadiocraftServerConfig.WEFAX_SOLAR_ALWAYS.get()) {
			List<String> lines = new ArrayList<>();
			for(Component line : TeletypeBlockEntity.reportLines(serverLevel, worldPosition))
				lines.add(line.getString());
			start(WefaxPrints.printText(serverLevel, worldPosition, Component.translatable("item.radiocraft.wefax_chart.solar"), "SOLAR WEATHER", lines, true));
			return;
		}
		IonosphericProcessorBlockEntity processor = localProcessor();
		if(processor != null) {
			printBulletin(serverLevel, processor.sender(), processor.bulletinLines(), Source.LOCAL);
			return;
		}
		TncPacket query = TncPacket.query(serverLevel.getRandom().nextLong(), sender(), TncPacket.TOPIC_SOLAR);
		boolean sent = false;
		for(RadioBlockEntity radio : getRadios())
			sent |= radio.isPowered() && radio.sendData(query);
		if(!sent)
			return;
		requestUntil = serverLevel.getGameTime() + REQUEST_TICKS;
		requester = player == null ? null : player.getUUID();
		if(player != null)
			player.displayClientMessage(Component.translatable("screen.radiocraft.wefax.requesting"), true);
		sync();
	}

	/** Who it asks as: the station's callsign if the gear it touches has one, else a tag for this machine. */
	private String sender() {
		String callsign = level == null ? "" : StationBus.find(level, worldPosition).callsign();
		return callsign.isEmpty() ? "WFX" + Integer.toHexString(worldPosition.hashCode() & 0xFFFF).toUpperCase(java.util.Locale.ROOT) : callsign;
	}

	/** True while it waits for an answer to SOLR WX. */
	public boolean isRequesting() {
		return requestUntil >= 0L;
	}

	private void printBulletin(ServerLevel serverLevel, String from, List<String> lines, Source source) {
		start(WefaxPrints.printText(serverLevel, worldPosition, Component.translatable("item.radiocraft.wefax_chart.solar"),
				WefaxPrints.bulletinTitle(from), WefaxPrints.bulletinSheet(from, lines), true), 1, source);
		if(source == Source.OVER_AIR) // The packet came in as the start tone ended: the print is that far along already.
			printStart -= Math.round(WefaxAudio.START_SHARE * printTicks);
	}

	/** True once it has heard a solar bulletin from an Ionospheric Processor. */
	public boolean hasSolarBulletin() {
		return solarHeardAt >= 0L && !solarLines.isEmpty();
	}

	/** Who sent the last solar bulletin heard, empty for none. */
	public String getSolarFrom() {
		return solarFrom;
	}

	public List<String> getSolarLines() {
		return solarLines;
	}

	/** True once it has heard a blackout alert. */
	public boolean hasHeardAlert() {
		return lastAlertAt >= 0L;
	}

	/**
	 * A packet decoded by a radio beside it, by the station gear, or handed over by an Ionospheric Processor beside it.
	 * A solar bulletin answers SOLR WX if it is waiting (or prints with AUTO on); a blackout alert always prints, three
	 * beeps first. Another alert within {@link #ALERT_GAP_TICKS} (from a second processor, say) only beeps.
	 */
	@Override
	public void receive(TncPacket packet) {
		receive(packet, Source.LOCAL);
	}

	/** A packet a radio beside it decoded off the air: a bulletin prints line by line as its tones go on coming in. */
	public void receiveOverAir(TncPacket packet) {
		receive(packet, Source.OVER_AIR);
	}

	private void receive(TncPacket packet, Source source) {
		if(!(level instanceof ServerLevel serverLevel) || packet.id() == lastPacketId)
			return;
		if(packet.kind() != TncPacket.Kind.SOLAR && packet.kind() != TncPacket.Kind.ALERT)
			return;
		List<String> lines = packet.solarLines();
		if(lines.isEmpty())
			return;
		lastPacketId = packet.id();
		if(packet.kind() == TncPacket.Kind.SOLAR) {
			solarFrom = packet.from();
			solarLines = List.copyOf(lines);
			solarHeardAt = serverLevel.getGameTime();
			if(isRequesting()) {
				requestUntil = -1L;
				requester = null;
				if(!isPrinting() && getPaper() > 0)
					printBulletin(serverLevel, packet.from(), lines, source);
			}
			else if(auto && cannotPrint(true) == null)
				printBulletin(serverLevel, packet.from(), lines, source);
		}
		else {
			long now = serverLevel.getGameTime();
			beepsFrom = now;
			boolean fresh = lastAlertAt < 0L || now - lastAlertAt >= ALERT_GAP_TICKS;
			lastAlertAt = now;
			if(fresh) {
				if(isPrinting() || getPaper() <= 0)
					queuedAlert = List.copyOf(lines); // Printed as soon as the sheet in hand is out (and there is paper).
				else
					start(WefaxPrints.printText(serverLevel, worldPosition, Component.translatable("item.radiocraft.wefax_chart.solar"), "SOLAR ALERT", lines));
			}
		}
		sync();
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean value) {
		auto = value;
		setChanged();
		sync();
	}

	private void start(WefaxPrints.Print print) {
		start(print, 1, Source.LOCAL);
	}

	/** How long a sheet takes: as long as a weather fax takes to send. */
	public static int printDuration() {
		return RadiocraftServerConfig.WEFAX_BULLETIN_SECONDS.get() * 20;
	}

	/** Starts printing a message whose redstone level is {@code alert} (see {@link #signal}), coming from {@code source}. */
	private void start(WefaxPrints.Print print, int alert, Source source) {
		printingSignal = alert;
		setSignal(Math.max(signal, alert));
		if(!ready.isEmpty() && level instanceof ServerLevel serverLevel)
			eject(serverLevel); // The next sheet pushes the one nobody took out of the front.
		paper.extractItem(0, 1, false);
		pixels = print.pixels().clone();
		printing = print.stack();
		printStart = level.getGameTime();
		printTicks = printDuration();
		printSource = source;
		rowsDone = 0;
		anyLost = false;
		broadcastFrames = source == Source.BROADCAST ? WefaxAudio.frames(pixels, WefaxChart.SIZE, printTicks) : null;
		broadcastFrame = 0;
		setChanged();
		sync();
	}

	/** How far down the sheet the print has come, 0 to 1 (1 when not printing): the lines whose tones have come in. */
	public float progress(long gameTime) {
		return printStart < 0 ? 1.0F : WefaxAudio.imageRows(gameTime - printStart, printTicks, WefaxChart.SIZE) / (float)WefaxChart.SIZE;
	}

	/** True if a radio beside it is copying a signal right now. */
	private boolean copying(long gameTime) {
		for(RadioBlockEntity radio : getRadios())
			if(radio.isCopying(gameTime))
				return true;
		return false;
	}

	/** The line just printed, and the coast station's tones through the radios beside it. */
	private void tickPrint(ServerLevel level, long now) {
		int rows = WefaxAudio.imageRows(now - printStart, printTicks, WefaxChart.SIZE);
		while(rowsDone < rows) {
			if(printSource == Source.OVER_AIR && !copying(now)) { // Lost under the noise: the stylus prints static.
				WefaxChart.speckle(pixels, rowsDone, level.getRandom().nextLong());
				anyLost = true;
				pixelsDirty = true;
			}
			if(rowsDone % 2 == 0) // The stylus across the paper, line after line.
				level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_KEY.get(), SoundSource.BLOCKS, 0.12F, 1.8F);
			rowsDone++;
		}
		if(pixelsDirty && now % 10 == 0) {
			pixelsDirty = false;
			sync();
		}
		List<short[]> frames = broadcastFrames;
		if(frames != null) {
			int count = now % 2 == 0 ? 3 : 2;
			for(int i = 0; i < count && broadcastFrame < frames.size(); i++) {
				short[] frame = frames.get(broadcastFrame++);
				for(RadioBlockEntity radio : getRadios())
					radio.playBroadcast(this, frame, BROADCAST_STRENGTH);
			}
		}
	}

	/** True if lines of the sheet in hand (or the last one) came out as speckle, lost under the noise. */
	public boolean hasLostLines() {
		return anyLost;
	}

	/** How strong the coast station comes in. */
	private static final double BROADCAST_STRENGTH = 0.7D;

	/** The picture of the sheet being printed (or the last one), {@link WefaxChart#SIZE} square; empty for none. */
	public byte[] getPixels() {
		return pixels;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WefaxReceiverBlockEntity receiver) {
		if(level instanceof ServerLevel serverLevel)
			receiver.serverTick(serverLevel);
	}

	private boolean radioWas;

	private void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		long sinceAlert = now - beepsFrom;
		if(sinceAlert == 0 || sinceAlert == 6 || sinceAlert == 12) // An alert: three sharp beeps.
			level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.9F, 1.6F);
		if(isRequesting() && now >= requestUntil) { // Nobody answered.
			requestUntil = -1L;
			level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
			if(requester != null && level.getPlayerByUUID(requester) instanceof ServerPlayer player && player.distanceToSqr(worldPosition.getCenter()) < 32 * 32)
				player.displayClientMessage(Component.translatable("screen.radiocraft.wefax.no_answer"), true);
			requester = null;
			sync();
		}
		if(now % 40 == 0 && hasRadio() != radioWas) { // The power lamp follows the radio beside it.
			radioWas = !radioWas;
			sync();
		}
		if(isPrinting()) {
			if(now % 9 == 0) // The drum motor, a little lower than the teletype's.
				level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_BACKGROUND.get(), SoundSource.BLOCKS, 0.35F, 0.7F);
			tickPrint(level, now);
			if(now - printStart >= printTicks)
				finish(level);
			return;
		}
		if(queuedAlert != null && getPaper() > 0) {
			List<String> sheet = queuedAlert;
			queuedAlert = null;
			start(WefaxPrints.printText(level, worldPosition, Component.translatable("item.radiocraft.wefax_chart.solar"), "SOLAR ALERT", sheet));
			return;
		}
		if(queuedPrint != null) {
			if(getPaper() > 0) {
				WefaxPrints.Print next = queuedPrint;
				queuedPrint = null;
				start(next, 1, queuedSource);
				queuedSource = Source.LOCAL;
			}
			return;
		}
		if(auto && now % 100 == 0 && now - lastAuto >= AUTO_GAP_TICKS && cannotPrint(false) == null && dangerAhead(level)) {
			lastAuto = now;
			printWeather();
		}
	}

	/** True if dangerous weather is here, or due within the weather mod's forecast horizon. */
	private boolean dangerAhead(ServerLevel level) {
		WeatherSource source = WeatherSources.get(level);
		BlockPos here = com.arrl.radiocraft.compat.sable.Sublevels.world(level, worldPosition);
		if(!source.report(level, here).hazards().isEmpty())
			return true;
		return source.nextArrival(level, here, source.forecastHorizon(level))
				.map(arrival -> !arrival.peak().hazards().isEmpty()).orElse(false);
	}

	/** Printed: the sheet waits in the tray, on the screen and sticking out of the model, until someone takes it. */
	private void finish(ServerLevel level) {
		if(anyLost) // The sheet was made clean when the print started: put the lost lines' speckle on it.
			WefaxPrints.repaint(level, printing, pixels);
		broadcastFrames = null;
		ready = printing;
		printing = ItemStack.EMPTY;
		printStart = -1L;
		setReady(level, !ready.isEmpty());
		level.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 0.4F, 1.4F);
		setChanged();
		sync();
	}

	/** True while a printed sheet waits in the tray. */
	public boolean hasReadySheet() {
		return !ready.isEmpty();
	}

	/** Takes the sheet in the tray: into {@code player}'s inventory (or at their feet), empty if there is none. */
	public ItemStack takeSheet(@Nullable ServerPlayer player) {
		if(ready.isEmpty() || !(level instanceof ServerLevel serverLevel))
			return ItemStack.EMPTY;
		ItemStack sheet = ready;
		ready = ItemStack.EMPTY;
		setReady(serverLevel, false);
		setSignal(isPrinting() ? printingSignal : 0); // Read: the alarm stops, unless the next message is coming out.
		if(player != null && !player.getInventory().add(sheet))
			player.drop(sheet, false);
		else if(player == null)
			eject(serverLevel, sheet);
		sync();
		return sheet;
	}

	/** The sheet in the tray comes out of the front. */
	private void eject(ServerLevel level) {
		ItemStack sheet = ready;
		ready = ItemStack.EMPTY;
		setReady(level, false);
		eject(level, sheet);
	}

	private void eject(ServerLevel level, ItemStack sheet) {
		Direction front = getBlockState().hasProperty(WefaxReceiverBlock.FACING) ? getBlockState().getValue(WefaxReceiverBlock.FACING) : Direction.NORTH;
		BlockPos out = worldPosition.relative(front);
		Containers.dropItemStack(level, out.getX() + 0.5D, worldPosition.getY() + 0.4D, out.getZ() + 0.5D, sheet);
	}

	private void setReady(ServerLevel level, boolean ready) {
		BlockState state = getBlockState();
		if(state.hasProperty(WefaxReceiverBlock.READY) && state.getValue(WefaxReceiverBlock.READY) != ready)
			level.setBlock(worldPosition, state.setValue(WefaxReceiverBlock.READY, ready), Block.UPDATE_CLIENTS);
	}

	// -------------------- MENU, SAVE & SYNC --------------------

	private void sync() {
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public @NotNull Component getDisplayName() {
		return Component.translatable("block.radiocraft.wefax_receiver");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
		return new WefaxReceiverMenu(id, this);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("paper", paper.serializeNBT(registries));
		tag.putBoolean("auto", auto);
		tag.putInt("schedule", scheduleIndex);
		tag.putByteArray("pixels", pixels);
		tag.putLong("printStart", printStart);
		tag.putInt("printTicks", printTicks);
		tag.putString("printSource", printSource.name());
		tag.putInt("rowsDone", rowsDone);
		tag.putBoolean("anyLost", anyLost);
		if(!printing.isEmpty())
			tag.put("printing", printing.save(registries));
		if(!ready.isEmpty())
			tag.put("ready", ready.save(registries));
		if(hasSolarBulletin()) {
			tag.putString("solarFrom", solarFrom);
			tag.putString("solarLines", String.join(TncPacket.SOLAR_LINE_BREAK, solarLines));
			tag.putLong("solarHeardAt", solarHeardAt);
		}
		tag.putLong("lastAlertAt", lastAlertAt);
		tag.putLong("requestUntil", requestUntil);
		tag.putInt("signal", signal);
		tag.putInt("printingSignal", printingSignal);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		paper.deserializeNBT(registries, tag.getCompound("paper"));
		auto = tag.getBoolean("auto");
		scheduleIndex = tag.getInt("schedule");
		pixels = tag.getByteArray("pixels");
		printStart = tag.contains("printStart") ? tag.getLong("printStart") : -1L;
		printTicks = tag.contains("printTicks") ? Math.max(1, tag.getInt("printTicks")) : 240;
		try {
			printSource = Source.valueOf(tag.getString("printSource"));
		}
		catch(IllegalArgumentException e) {
			printSource = Source.LOCAL;
		}
		rowsDone = tag.getInt("rowsDone");
		anyLost = tag.getBoolean("anyLost");
		printing = tag.contains("printing") ? ItemStack.parseOptional(registries, tag.getCompound("printing")) : ItemStack.EMPTY;
		ready = tag.contains("ready") ? ItemStack.parseOptional(registries, tag.getCompound("ready")) : ItemStack.EMPTY;
		solarFrom = tag.getString("solarFrom");
		solarLines = tag.getString("solarLines").isEmpty() ? List.of()
				: List.of(tag.getString("solarLines").split(java.util.regex.Pattern.quote(TncPacket.SOLAR_LINE_BREAK)));
		solarHeardAt = tag.contains("solarHeardAt") ? tag.getLong("solarHeardAt") : -1L;
		lastAlertAt = tag.contains("lastAlertAt") ? tag.getLong("lastAlertAt") : -1L;
		requestUntil = tag.contains("requestUntil") ? tag.getLong("requestUntil") : -1L;
		signal = tag.getInt("signal");
		printingSignal = tag.getInt("printingSignal");
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putBoolean("hasRadio", hasRadio());
		return tag;
	}

	private boolean clientHasRadio;

	@Override
	public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.handleUpdateTag(tag, registries);
		clientHasRadio = tag.getBoolean("hasRadio");
	}

	/** On the client: whether the server last said a radio is beside it (the power lamp). */
	public boolean clientHasRadio() {
		return clientHasRadio;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(@NotNull net.minecraft.network.Connection connection, @NotNull ClientboundBlockEntityDataPacket packet, HolderLookup.@NotNull Provider registries) {
		handleUpdateTag(packet.getTag(), registries);
	}
}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.init.RadiocraftSoundEvents;
import net.minecraft.ChatFormatting;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.DummyLoadNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the solar weather of its dimension for the {@link com.arrl.radiocraft.common.blocks.SolarWeatherStationBlock}:
 * the current event's name, its rank for a comparator, and how each HF band is doing right now. The only reason this is a block entity
 * is the comparator: it has to be told when the event changes, which the server tick notices by watching the event
 * instance. The printed paper report from the proposal is still to come.
 */
public class SolarWeatherStationBlockEntity extends BlockEntity {

	/** The band the station reports on by name, the one the proposal calls out as opening and closing with the weather. */
	public static final String REPORTED_BAND = "10m";

	@Nullable
	private SolarEventInstance lastSeenEvent;
	private boolean lastSeenRadio;

	/** Ticks the printing sound lasts (2.79 s); the lines are spread over it and the bell rings at the end. */
	public static final int PRINT_TICKS = 56;
	/** The HF bands reported, in order. VHF is reported first as "Normal" since the sun does not touch it. */
	public static final List<String> REPORTED_BANDS = List.of("10m", "20m", "40m", "80m");

	/** A chat line waiting to be printed for a player, with the game time to print it at; a ding instead of a line rings the bell. */
	private record PendingLine(UUID player, @Nullable Component line, long at, boolean ding) {}
	private final List<PendingLine> pending = new ArrayList<>();

	public SolarWeatherStationBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_WEATHER_STATION.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SolarWeatherStationBlockEntity be) {
		be.printPending(level);
		SolarEventInstance current = SolarEventManager.getEvent(level);
		boolean radio = level.getGameTime() % 10 == 0 ? be.hasRadio() : be.lastSeenRadio;
		if(current != be.lastSeenEvent || radio != be.lastSeenRadio) {
			be.lastSeenEvent = current;
			be.lastSeenRadio = radio;
			level.updateNeighbourForOutputSignal(pos, state.getBlock()); // Comparators only re-read when told to.
		}
	}

	/** The solar event in this station's dimension, null before the manager has picked one. */
	@Nullable
	public SolarEvent getCurrentEvent() {
		if(level == null)
			return null;
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
		if(!RadiocraftServerConfig.SOLAR_WEATHER_STATION_NEEDS_RADIO.get())
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
		SolarEvent event = getCurrentEvent();
		if(event == null || level == null)
			return null;
		BandCondition condition = event.getCondition(band, level.isDay());
		return Aurora.affects(level, worldPosition) ? condition.worse() : condition;
	}

	/** Whether {@link #REPORTED_BAND} propagates at all right now (any condition but closed). */
	public boolean isReportedBandOpen() {
		BandCondition condition = getBandCondition(REPORTED_BAND);
		return condition != null && condition != BandCondition.CLOSED;
	}

	/** "Aurora: active beyond Z ±10000" or "Aurora expected ...", null when there is none. */
	@Nullable
	public Component auroraLine() {
		if(level == null)
			return null;
		int latitude = RadiocraftServerConfig.AURORA_LATITUDE.get();
		if(Aurora.isActive(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_active", latitude).withStyle(ChatFormatting.GREEN);
		if(Aurora.isExpected(level))
			return Component.translatable("message.radiocraft.solar_weather_station.aurora_expected", latitude).withStyle(ChatFormatting.LIGHT_PURPLE);
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

	/**
	 * Starts the report for {@code player}: the teletype starts printing at the block and the lines of the report
	 * appear in chat one by one over the length of the sound, then the bell rings.
	 */
	public void startReport(ServerPlayer player) {
		if(!(level instanceof ServerLevel serverLevel))
			return;
		List<Component> lines = getReportLines();
		serverLevel.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
		long now = serverLevel.getGameTime();
		int spacing = Math.max(1, PRINT_TICKS / lines.size());
		for(int i = 0; i < lines.size(); i++)
			pending.add(new PendingLine(player.getUUID(), lines.get(i), now + (long)i * spacing, false));
		pending.add(new PendingLine(player.getUUID(), null, now + PRINT_TICKS, true));
	}

	/** How many lines are still to be printed (for tests). */
	public int getPendingCount() {
		return pending.size();
	}

	private void printPending(Level level) {
		if(pending.isEmpty() || !(level instanceof ServerLevel serverLevel))
			return;
		long now = serverLevel.getGameTime();
		for(Iterator<PendingLine> it = pending.iterator(); it.hasNext(); ) {
			PendingLine next = it.next();
			if(next.at() > now)
				continue;
			it.remove();
			if(next.ding())
				serverLevel.playSound(null, worldPosition, RadiocraftSoundEvents.TELETYPE_DING.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
			ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(next.player());
			if(player != null && next.line() != null)
				player.displayClientMessage(next.line(), false);
		}
	}

	/** The report, one chat line per entry: header, overall conditions, noise floor, then each band. */
	public List<Component> getReportLines() {
		if(!hasRadio())
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_radio"));
		SolarEvent event = getCurrentEvent();
		if(event == null)
			return List.of(Component.translatable("message.radiocraft.solar_weather_station.no_data"));
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.overall", eventName(getCurrentEventKey())));
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.noise", Math.round(event.getNoise() * 100.0F)));
		boolean underAurora = level != null && Aurora.affects(level, worldPosition);
		lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", "VHF",
				Component.translatable(underAurora ? "message.radiocraft.solar_weather_station.aurora" : "message.radiocraft.solar_weather_station.normal")));
		for(String band : REPORTED_BANDS)
			lines.add(Component.translatable("message.radiocraft.solar_weather_station.band", band, conditionName(getBandCondition(band))));
		Component aurora = auroraLine();
		if(aurora != null)
			lines.add(aurora);
		return lines;
	}

	/** The one-line report, kept for tools and tests; the block itself prints {@link #getReportLines()}. */
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
}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A weather station: once a second it reads the weather where it stands (from whichever weather mod is in charge, see
 * {@link WeatherSources}), keeping the highest gust of the last ten minutes and the rain of the last hour (real
 * minutes, as APRS counts them). Clients are told the wind, and turn the vane and spin the cups smoothly with it
 * ({@link com.arrl.radiocraft.client.render.WeatherStationRenderer}). It has no radio of its own: like the
 * wireless console of a real one, any H19 within {@link #RANGE} blocks reports its weather in its beacons.
 *
 * <p>The gust and rain history is not saved; a station starts afresh when its chunk loads.
 */
public class WeatherStationBlockEntity extends BlockEntity {

	/** How far an H19 hears a weather station, blocks. */
	public static final int RANGE = 64;
	/** Rain in the heaviest downpour, millimetres a second (6 mm a minute; storms only last minutes). */
	static final double HEAVIEST_MM_PER_SECOND = 0.1;
	private static final int MINUTE = 1200;
	private static final int GUST_MINUTES = 10;
	private static final int RAIN_MINUTES = 60;

	private static final Map<ResourceKey<Level>, Map<BlockPos, WeatherStationBlockEntity>> LOADED = new ConcurrentHashMap<>();

	/** Highest wind each minute, metres per second, and the minute (game time / {@link #MINUTE}) each slot is for. */
	private final double[] gusts = new double[GUST_MINUTES];
	private final long[] gustMinute = new long[GUST_MINUTES];
	/** Rain each minute, millimetres, and the minute each slot is for. */
	private final double[] rain = new double[RAIN_MINUTES];
	private final long[] rainMinute = new long[RAIN_MINUTES];
	private WeatherReport report = WeatherReport.CALM;
	/** The wind clients were last told: the bearing it comes from, degrees, and its speed, metres per second. */
	private float syncedFrom = Float.NaN;
	private float syncedSpeed;
	// Client side: the wind as told, and the vane and cups turning toward and with it (this tick and the last, degrees).
	private float windFrom, windSpeed, vane, vaneBefore, cups, cupsBefore, cupSpeed;
	private boolean vaneSet;

	public WeatherStationBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.WEATHER_STATION.get(), pos, state);
		Arrays.fill(gustMinute, -1);
		Arrays.fill(rainMinute, -1);
	}

	/** The loaded weather station nearest {@code pos} within {@link #RANGE} blocks, or null. */
	public static @Nullable WeatherStationBlockEntity nearest(Level level, BlockPos pos) {
		Map<BlockPos, WeatherStationBlockEntity> map = LOADED.get(level.dimension());
		if(map == null)
			return null;
		WeatherStationBlockEntity best = null;
		double bestDistance = (double)RANGE * RANGE;
		for(WeatherStationBlockEntity station : map.values()) {
			double distance = com.arrl.radiocraft.compat.sable.Sublevels.distSqr(level, station.worldPosition, pos);
			if(!station.isRemoved() && distance <= bestDistance) {
				best = station;
				bestDistance = distance;
			}
		}
		return best;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			LOADED.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(worldPosition, this);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if(level != null && !level.isClientSide) {
			Map<BlockPos, WeatherStationBlockEntity> map = LOADED.get(level.dimension());
			if(map != null)
				map.remove(worldPosition, this);
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		setRemoved();
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if(t instanceof WeatherStationBlockEntity station && level instanceof ServerLevel serverLevel)
			station.serverTick(serverLevel, state);
	}

	private void serverTick(ServerLevel level, BlockState state) {
		long now = level.getGameTime();
		if(now % 20 != 0)
			return;
		read(level, now);
		// Tell clients when the wind has changed enough to see: a few degrees, or half a metre a second.
		float from = (float)report.windFrom(), speed = (float)report.windSpeed();
		if(Float.isNaN(syncedFrom) || Math.abs(Mth.wrapDegrees(from - syncedFrom)) > 4 || Math.abs(speed - syncedSpeed) > 0.5F) {
			syncedFrom = from;
			syncedSpeed = speed;
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	/** Client side, each tick: the vane eases round to the wind, the cups spin up or down to its speed. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, WeatherStationBlockEntity station) {
		station.vaneBefore = station.vane;
		station.cupsBefore = station.cups;
		if(!station.vaneSet) {
			station.vane = station.vaneBefore = station.windFrom;
			station.vaneSet = true;
		}
		if(station.windSpeed >= 0.5F) // In a calm the vane stays where it was.
			station.vane += Mth.wrapDegrees(station.windFrom - station.vane) * 0.06F;
		// Cups run at about a third of the wind speed round a 5.5 pixel arm: degrees a tick, with some inertia.
		float target = Math.min(60.0F, station.windSpeed * 6.0F);
		station.cupSpeed += (target - station.cupSpeed) * 0.05F;
		station.cups = (station.cups + station.cupSpeed) % 360.0F;
		if(station.cups < station.cupsBefore) // Wrapped: keep the interpolation going forward.
			station.cupsBefore -= 360.0F;
	}

	/** Degrees clockwise from north the vane points, between the last tick and this one. */
	public float vaneAngle(float partialTick) {
		return vaneBefore + Mth.wrapDegrees(vane - vaneBefore) * partialTick;
	}

	/** How far round the cups are, degrees, between the last tick and this one. */
	public float cupAngle(float partialTick) {
		return Mth.lerp(partialTick, cupsBefore, cups);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putFloat("windFrom", Float.isNaN(syncedFrom) ? 0 : syncedFrom);
		tag.putFloat("windSpeed", syncedSpeed);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag, registries);
		windFrom = tag.getFloat("windFrom");
		windSpeed = tag.getFloat("windSpeed");
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		handleUpdateTag(packet.getTag(), registries);
	}

	private void read(ServerLevel level, long now) {
		report = WeatherSources.get(level).report(level, com.arrl.radiocraft.compat.sable.Sublevels.world(level, worldPosition));
		long minute = now / MINUTE;
		int g = (int)(minute % GUST_MINUTES);
		if(gustMinute[g] != minute) {
			gustMinute[g] = minute;
			gusts[g] = 0;
		}
		gusts[g] = Math.max(gusts[g], report.windSpeed());
		int r = (int)(minute % RAIN_MINUTES);
		if(rainMinute[r] != minute) {
			rainMinute[r] = minute;
			rain[r] = 0;
		}
		if(level.canSeeSky(worldPosition.above()))
			rain[r] += rainPerSecond(report);
	}

	/** Millimetres of water a second falling in {@code report}: snow and hail as what they melt to. */
	static double rainPerSecond(WeatherReport report) {
		double melt = switch(report.precipitation()) {
			case RAIN -> 1;
			case HAIL -> 0.8;
			case SNOW -> 0.1;
			default -> 0;
		};
		return report.rain() * melt * HEAVIEST_MM_PER_SECOND;
	}

	/** The highest wind of the last ten minutes, metres per second. */
	public double gust() {
		long minute = level == null ? 0 : level.getGameTime() / MINUTE;
		double max = report.windSpeed();
		for(int i = 0; i < GUST_MINUTES; i++)
			if(gustMinute[i] > minute - GUST_MINUTES)
				max = Math.max(max, gusts[i]);
		return max;
	}

	/** Rain in the last hour, millimetres. */
	public double rainLastHour() {
		long minute = level == null ? 0 : level.getGameTime() / MINUTE;
		double sum = 0;
		for(int i = 0; i < RAIN_MINUTES; i++)
			if(rainMinute[i] > minute - RAIN_MINUTES)
				sum += rain[i];
		return sum;
	}

	public WeatherReport report() {
		return report;
	}

	/** The APRS weather data for a beacon, stamped with the level's date and time. */
	public String aprsWeather() {
		return level instanceof ServerLevel serverLevel ? WeatherReports.aprsWeather(serverLevel, report, gust(), rainLastHour()) : "";
	}

	/** What right clicking the station shows. */
	public String describe() {
		String wind = report.windSpeed() < 0.5 ? "calm" : String.format(Locale.ROOT, "wind from %03.0f at %.1f m/s, gusts %.1f",
				report.windFrom(), report.windSpeed(), gust());
		return String.format(Locale.ROOT, "WX: %s; %.0f °C, %.0f %%, %.0f hPa; rain last hour %.1f mm. H19s within %d blocks beacon it.",
				wind, report.temperature(), report.humidity(), report.pressure(), rainLastHour(), RANGE);
	}
}

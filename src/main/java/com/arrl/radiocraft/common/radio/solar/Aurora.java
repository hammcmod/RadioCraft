package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.network.clientbound.CAuroraPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auroras, per dimension. A solar event with an {@link AuroraForecast} (the blackout) may schedule one some days after
 * it starts. While it is on, stations far enough from the equator ({@link #inZone}) get a long VHF skip off the aurora
 * and lose one condition step on every HF band; their HF radios also hear extra static, and players there see it in
 * the night sky. Like the solar events themselves this lives in memory only.
 */
public final class Aurora {

	/** An aurora from game time {@code start} (inclusive) to {@code end} (exclusive). */
	public record Window(long start, long end) {}

	private static final Map<ResourceKey<Level>, Window> windows = new ConcurrentHashMap<>(); // Read from the voice chat thread.
	private static final Map<ResourceKey<Level>, Boolean> lastSentActive = new ConcurrentHashMap<>();

	private Aurora() {}

	/** A solar event just started in {@code level}: roll its forecast, unless an aurora is already on its way. */
	public static void onEventStarted(ServerLevel level, SolarEvent event) {
		AuroraForecast forecast = event.getAurora();
		if(forecast == null || !level.dimensionType().hasSkyLight())
			return;
		Window current = windows.get(level.dimension());
		long now = level.getGameTime();
		if(current != null && current.end() > now)
			return;
		if(forecast.hits(level.getRandom().nextDouble())) {
			schedule(level, now + forecast.delay(), forecast.duration());
			Radiocraft.LOGGER.info("Dimension: {} aurora expected in {} ticks", level.dimension(), forecast.delay());
		}
	}

	/** Puts an aurora on the calendar of {@code level}, replacing any other. */
	public static void schedule(ServerLevel level, long start, int duration) {
		windows.put(level.dimension(), new Window(start, start + duration));
		tick(level);
	}

	/** Ends or cancels the aurora of {@code level}. */
	public static void clear(ServerLevel level) {
		windows.remove(level.dimension());
		tick(level);
	}

	/** Forgets every aurora; the server is stopping. */
	public static void clearAll() {
		windows.clear();
		lastSentActive.clear();
	}

	@Nullable
	public static Window getWindow(Level level) {
		return windows.get(level.dimension());
	}

	/** True while an aurora is lit in {@code level}. */
	public static boolean isActive(Level level) {
		Window window = windows.get(level.dimension());
		long now = level.getGameTime();
		return window != null && window.start() <= now && now < window.end();
	}

	/** True when an aurora has been forecast for {@code level} but has not started yet. */
	public static boolean isExpected(Level level) {
		Window window = windows.get(level.dimension());
		return window != null && window.start() > level.getGameTime();
	}

	/** True if a station at Z = {@code z} is far enough from the equator to be under an aurora. */
	public static boolean inZone(int z, int latitude) {
		return Math.abs((long)z) >= latitude;
	}

	public static boolean inZone(BlockPos pos) {
		return inZone(pos.getZ(), RadiocraftServerConfig.AURORA_LATITUDE.get());
	}

	/** True if an aurora is on in {@code level} and {@code pos} is under it. */
	public static boolean affects(Level level, BlockPos pos) {
		return isActive(level) && inZone(pos);
	}

	/**
	 * Strength of a VHF signal carried off the aurora between two stations, 0 unless the band is VHF, an aurora is on and
	 * both ends are under it.
	 */
	public static double vhfStrength(Level level, @Nullable Band band, BlockPos from, BlockPos to, double distance) {
		if(band == null || !band.isVhf() || !isActive(level) || !inZone(from) || !inZone(to))
			return 0.0D;
		return BandUtils.getAuroraStrength(distance, RadiocraftServerConfig.AURORA_VHF_MIN_RANGE.get(),
				RadiocraftServerConfig.AURORA_VHF_MAX_RANGE.get(), RadiocraftServerConfig.AURORA_VHF_STRENGTH.get());
	}

	/** Tells the players of {@code level} when its aurora lights up or goes out. Called every tick. */
	public static void tick(ServerLevel level) {
		boolean active = isActive(level);
		Boolean sent = lastSentActive.put(level.dimension(), active);
		if(sent == null || sent != active)
			CAuroraPacket.sendToLevel(level);
	}
}

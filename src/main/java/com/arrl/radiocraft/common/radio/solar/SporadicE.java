package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sporadic E (proposal; maintainer, 2026-10-24: only in good or great conditions): now and then a patch of the E layer
 * reflects VHF, opening long-range VHF contacts for up to a day. While the sun is good or better there is about a
 * {@code sporadic_e_chance_per_day} chance a day of an opening, which then lasts a random part of a day. Unlike the
 * aurora it covers the whole dimension. In memory only, like the solar events.
 */
public final class SporadicE {

	/** Solar events good enough for an opening. */
	private static final Set<String> OPEN_EVENTS = Set.of("good", "great");
	/** How often an opening is rolled for, in ticks. */
	private static final int ROLL_TICKS = 1200;
	private static final int DAY_TICKS = 24000;

	/** An opening from game time {@code start} (inclusive) to {@code end} (exclusive). */
	public record Window(long start, long end) {}

	private static final Map<ResourceKey<Level>, Window> windows = new ConcurrentHashMap<>(); // Read from the voice chat thread.

	private SporadicE() {}

	/** True while VHF is open on sporadic E in {@code level}. */
	public static boolean isActive(Level level) {
		Window window = windows.get(level.dimension());
		long now = level.getGameTime();
		return window != null && window.start() <= now && now < window.end();
	}

	/** Opens sporadic E in {@code level} for {@code duration} ticks from now (debug, tests). */
	public static void open(ServerLevel level, int duration) {
		windows.put(level.dimension(), new Window(level.getGameTime(), level.getGameTime() + duration));
	}

	public static void close(ServerLevel level) {
		windows.remove(level.dimension());
	}

	public static void clearAll() {
		windows.clear();
	}

	/** True when the solar event in {@code level} is good enough for sporadic E. */
	public static boolean conditionsAllow(Level level) {
		SolarEvent.SolarEventInstance event = SolarEventManager.getEvent(level);
		if(event == null)
			return false;
		var id = com.arrl.radiocraft.common.init.RadiocraftData.SOLAR_EVENTS.getKey(event.getEvent());
		return id != null && OPEN_EVENTS.contains(id.getPath());
	}

	/** Rolls for an opening now and then while the sun allows it; closes it early if conditions drop. */
	public static void tick(ServerLevel level) {
		if(!RadiocraftServerConfig.SPORADIC_E_ENABLED.get() || !level.dimensionType().hasSkyLight()) {
			windows.remove(level.dimension());
			return;
		}
		long now = level.getGameTime();
		if(now % ROLL_TICKS != 0)
			return;
		Window window = windows.get(level.dimension());
		if(window != null && now >= window.end())
			windows.remove(level.dimension());
		if(isActive(level) || !conditionsAllow(level))
			return;
		double perDay = RadiocraftServerConfig.SPORADIC_E_CHANCE_PER_DAY.get();
		double perRoll = 1.0D - Math.pow(1.0D - perDay, ROLL_TICKS / (double)DAY_TICKS); // perDay spread over the day's rolls.
		if(level.getRandom().nextDouble() < perRoll) {
			int duration = DAY_TICKS / 4 + level.getRandom().nextInt(DAY_TICKS * 3 / 4); // A quarter of a day to a day.
			windows.put(level.dimension(), new Window(now, now + duration));
			Radiocraft.LOGGER.info("Dimension: {} sporadic E open for {} ticks", level.dimension(), duration);
		}
	}

	/** Strength of a VHF signal carried by sporadic E over {@code distance} blocks, 0 when closed or not VHF. */
	public static double vhfStrength(Level level, @Nullable Band band, double distance) {
		if(band == null || !band.isVhf() || !isActive(level))
			return 0.0D;
		return BandUtils.getAuroraStrength(distance, RadiocraftServerConfig.SPORADIC_E_MIN_RANGE.get(),
				RadiocraftServerConfig.SPORADIC_E_MAX_RANGE.get(), RadiocraftServerConfig.SPORADIC_E_STRENGTH.get());
	}
}

package com.arrl.radiocraft.common.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is on the air right now: every antenna that sends voice, CW or data notes where it is, on what frequency and
 * when. Cheap to write from the voice chat thread (one map put per frame); read by things that only need to know a
 * signal exists and where it comes from, like the handheld direction finder. Entries older than {@link #KEEP_TICKS}
 * are dropped as the log is read.
 */
public final class TransmitterLog {

	private TransmitterLog() {}

	/** How long, in ticks, a transmission is remembered after its last frame. */
	public static final long KEEP_TICKS = 40L;

	/** One transmitter's latest frame. */
	public record Entry(ResourceKey<Level> dimension, BlockPos pos, @Nullable Band band, float frequency, long gameTime) {}

	private static final Map<Object, Entry> ENTRIES = new ConcurrentHashMap<>();

	/** {@code source} (the antenna, any object that stands for one transmitter) is on the air this tick. */
	public static void record(Object source, Level level, BlockPos pos, @Nullable Band band, float frequency) {
		if(source == null || level == null || pos == null)
			return;
		// Where it really is: a transmitter on a Sable ship is heard from the ship, not from its plot.
		ENTRIES.put(source, new Entry(level.dimension(), com.arrl.radiocraft.compat.sable.Sublevels.world(level, pos).immutable(), band, frequency, level.getGameTime()));
	}

	/** As {@link #record(Object, Level, BlockPos, Band, float)}, from an antenna's position (ignored when null). */
	public static void record(Object source, @Nullable com.arrl.radiocraft.api.antenna.IAntenna.AntennaPos pos, @Nullable Band band, float frequency) {
		if(pos != null)
			record(source, pos.level(), pos.position(), band, frequency);
	}

	/**
	 * The nearest transmitter to {@code from} heard in the last {@code withinTicks} on {@code frequency} (within
	 * {@code toleranceHz}) and no further than {@code maxDistance}; null if none.
	 */
	@Nullable
	public static Entry nearest(Level level, BlockPos from, float frequency, double toleranceHz, double maxDistance, long withinTicks) {
		long now = level.getGameTime();
		from = com.arrl.radiocraft.compat.sable.Sublevels.world(level, from);
		Entry best = null;
		double bestDistance = maxDistance * maxDistance;
		for(Map.Entry<Object, Entry> slot : ENTRIES.entrySet()) {
			Entry entry = slot.getValue();
			if(now - entry.gameTime() > KEEP_TICKS || entry.gameTime() > now + KEEP_TICKS) {
				ENTRIES.remove(slot.getKey(), entry);
				continue;
			}
			if(now - entry.gameTime() > withinTicks || !entry.dimension().equals(level.dimension()) || Math.abs(entry.frequency() - frequency) > toleranceHz)
				continue;
			double distance = entry.pos().distSqr(from);
			if(distance <= bestDistance) {
				bestDistance = distance;
				best = entry;
			}
		}
		return best;
	}

	/** Forgets everything (tests). */
	public static void clear() {
		ENTRIES.clear();
	}
}

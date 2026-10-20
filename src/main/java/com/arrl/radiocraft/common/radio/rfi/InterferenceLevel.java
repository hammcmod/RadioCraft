package com.arrl.radiocraft.common.radio.rfi;

/**
 * A noise level that is bumped by discrete events (a redstone component switching near the antenna) and dies away on
 * its own. The decay is applied lazily from the game time, so nothing has to tick it: both the server and the client
 * keep one of these per radio and only touch it when a pop arrives or the value is read.
 * <p>
 * With {@link #DECAY_PER_TICK} a single pop is almost gone a quarter of a second later, while a redstone clock keeps the
 * level up: at a steady {@code r} pops per tick of size {@code p} the level settles at {@code r * p / (1 - DECAY)}.
 */
public class InterferenceLevel {

	/** Fraction of the level that survives each tick. 0.8 means a time constant of about 4.5 ticks. */
	public static final double DECAY_PER_TICK = 0.8D;
	/** Levels below this read as 0, so a decayed-out radio costs nothing to query. */
	private static final double FLOOR = 1e-3D;

	private double level = 0.0D;
	private long levelTick = Long.MIN_VALUE;

	/** Adds {@code amount} at game time {@code now}, after decaying what was there. The total is capped at 1. */
	public synchronized void add(long now, double amount) {
		level = Math.min(1.0D, get(now) + Math.max(0.0D, amount));
		levelTick = now;
	}

	/** The level at game time {@code now}, 0 to 1. */
	public synchronized double get(long now) {
		if(level <= 0.0D)
			return 0.0D;
		long elapsed = now - levelTick;
		if(elapsed > 0) {
			level *= Math.pow(DECAY_PER_TICK, elapsed);
			levelTick = now;
			if(level < FLOOR)
				level = 0.0D;
		}
		return level;
	}

	public synchronized void clear() {
		level = 0.0D;
	}
}

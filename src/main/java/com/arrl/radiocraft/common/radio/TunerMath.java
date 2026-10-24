package com.arrl.radiocraft.common.radio;

/**
 * The antenna tuners' arithmetic: what a manual tuner's three dials give, how its lamp blinks, and the automatic
 * tuner's click pattern. Pure functions, so they can be unit tested without a game bootstrap.
 * <p>
 * A tuner can bring an antenna up to {@code maxMatchable} all the way to SWR 1. Past that it can still take
 * {@code maxMatchable - 1} off: an SWR 9 antenna through a tuner rated 7 is at best SWR 3 ({@link #bestSWR}).
 */
public class TunerMath {

    /** A manual tuner's dials read 0 to this. */
    public static final int DIAL_MAX = 50;
    /** The lowest and highest dial reading a match can need, so a dial is never pinned against its stop. */
    public static final int TARGET_MIN = 8;
    public static final int TARGET_MAX = 42;
    /** How far apart the dials can be from their targets at worst, in dial steps (the scale of {@link #mismatch}). */
    private static final double WORST_DISTANCE = TARGET_MAX - 0.0D;
    /** Shapes the curve so the first steps towards a match already show on the lamp (below 1 is steeper near 0). */
    private static final double CURVE = 0.7D;

    /** At or below this SWR the lamp is solid green: a perfect tune. */
    public static final double PERFECT_SWR = 1.05D;
    /** Below this SWR the lamp blinks yellow-green instead of red. */
    public static final double GOOD_SWR = 3.0D;

    /** The best SWR a tuner rated {@code maxMatchable} can get out of an antenna at {@code rawSwr}; 0 (no antenna) stays 0. */
    public static double bestSWR(double rawSwr, double maxMatchable) {
        if(rawSwr <= 0.0D)
            return rawSwr;
        return Math.max(1.0D, 1.0D + rawSwr - maxMatchable);
    }

    /**
     * The three dial readings that match an antenna. {@code seed} picks them for the antenna and band (so every
     * antenna needs its own settings), and they drift a little across the band with {@code bandFraction}, 0 at the
     * bottom of the band and 1 at the top, so a big move up the band needs a retune.
     */
    public static int[] targets(long seed, double bandFraction) {
        int[] targets = new int[3];
        long state = seed;
        for(int i = 0; i < 3; i++) {
            state = mix(state + 0x9E3779B97F4A7C15L);
            double base = (state >>> 11) * 0x1.0p-53; // 0 to 1
            double drift = (i == 0 ? -0.25D : 0.15D) * (Math.max(0.0D, Math.min(1.0D, bandFraction)) - 0.5D);
            double u = Math.max(0.0D, Math.min(1.0D, base + drift));
            targets[i] = TARGET_MIN + (int)Math.round(u * (TARGET_MAX - TARGET_MIN));
        }
        return targets;
    }

    /** A seed for {@link #targets} from anything that identifies the antenna and band. */
    public static long seed(long... parts) {
        long state = 0x2545F4914F6CDD1DL;
        for(long part : parts)
            state = mix(state ^ part);
        return state;
    }

    /** 0 with every dial on its target, rising to 1 with the dials as far off as they can be. */
    public static double mismatch(int[] dials, int[] targets) {
        double sum = 0.0D;
        for(int i = 0; i < 3; i++) {
            double d = dials[i] - targets[i];
            sum += d * d;
        }
        double distance = Math.sqrt(sum / 3.0D) / WORST_DISTANCE;
        return Math.pow(Math.min(1.0D, distance), CURVE);
    }

    /**
     * The SWR a radio sees through a manual tuner: {@link #bestSWR} with every dial on its target, the antenna's own
     * SWR with them as far off as they go, and in between the closer the dials, the lower.
     */
    public static double manualSWR(double rawSwr, double maxMatchable, int[] dials, int[] targets) {
        if(rawSwr <= 0.0D)
            return rawSwr;
        double best = bestSWR(rawSwr, maxMatchable);
        return best + (rawSwr - best) * mismatch(dials, targets);
    }

    /**
     * Share of the transmit power that reaches the antenna through a tuner: whatever the SWR the radio sees still
     * reflects, less {@code lossFraction} of the antenna's own mismatch loss, which the tuner burns. With a perfect
     * match this is {@link SWRHelper#tunedEfficiency}.
     */
    public static double efficiency(double rawSwr, double seenSwr, double lossFraction) {
        if(rawSwr <= 0.0D)
            return SWRHelper.getEfficiencyMultiplier(rawSwr);
        return SWRHelper.getEfficiencyMultiplier(seenSwr) * (1.0D - (1.0D - SWRHelper.getEfficiencyMultiplier(rawSwr)) * lossFraction);
    }

    // -------------------- LAMP --------------------

    public enum Lamp { OFF, RED, YELLOW_GREEN, GREEN }

    /**
     * The lamp on a manual tuner's panel at {@code timeTicks} (any running clock in ticks, fractions allowed): red
     * while the radio sees {@link #GOOD_SWR} or worse, blinking faster the closer it gets; yellow-green below that,
     * faster again towards 1; solid green at {@link #PERFECT_SWR}.
     */
    public static Lamp lamp(double rawSwr, double seenSwr, double timeTicks) {
        if(seenSwr <= 0.0D)
            return Lamp.OFF;
        if(seenSwr <= PERFECT_SWR)
            return Lamp.GREEN;
        double period = blinkPeriodTicks(rawSwr, seenSwr);
        boolean lit = (timeTicks % period) < period / 2.0D;
        if(!lit)
            return Lamp.OFF;
        return seenSwr < GOOD_SWR ? Lamp.YELLOW_GREEN : Lamp.RED;
    }

    /** Slowest and fastest blink, in ticks for a full on-off cycle. */
    public static final double SLOWEST_BLINK = 24.0D;
    public static final double FASTEST_BLINK = 3.0D;

    /**
     * How long one on-off cycle of the lamp takes. Red runs from {@link #SLOWEST_BLINK} with the dials doing nothing
     * to {@link #FASTEST_BLINK} at SWR 3; yellow-green runs the same way again from SWR 3 down to a perfect tune.
     */
    public static double blinkPeriodTicks(double rawSwr, double seenSwr) {
        double progress;
        if(seenSwr >= GOOD_SWR)
            progress = rawSwr <= GOOD_SWR ? 1.0D : (rawSwr - seenSwr) / (rawSwr - GOOD_SWR);
        else
            progress = (GOOD_SWR - seenSwr) / (GOOD_SWR - PERFECT_SWR);
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return SLOWEST_BLINK + (FASTEST_BLINK - SLOWEST_BLINK) * progress;
    }

    // -------------------- AUTOMATIC TUNER --------------------

    /** A move of this many Hz or more (or a new band) makes the automatic tuner tune again. */
    public static final float RETUNE_HZ = 1_000_000.0F;
    /** The SWR a radio sees while the automatic tuner is still searching. */
    public static final double TUNING_SWR = 3.0D;
    /** Ticks the automatic tuner takes to find a match it can make. */
    public static final int TUNE_TICKS = 20;
    /** Ticks it hunts for a match it cannot make before it gives up. */
    public static final int FAIL_SEARCH_TICKS = 120;
    /** The relays clicking as the tuner closes in: slow, then faster, then a rattle ("click... click... clickclickclick"). */
    private static final int[] TUNE_CLICKS = {0, 6, 11, 14, 16, 17, 18, 19};
    /** Hunting for a match it cannot make, the clicks never speed up. */
    public static final int FAIL_CLICK_INTERVAL = 6;

    /** True on the ticks of a tuning run (counted from 0) that the relays click. */
    public static boolean clicksAt(int tick, boolean failing) {
        if(failing)
            return tick >= 0 && tick < FAIL_SEARCH_TICKS && tick % FAIL_CLICK_INTERVAL == 0;
        for(int click : TUNE_CLICKS) {
            if(click == tick)
                return true;
        }
        return false;
    }

    /** True if the automatic tuner, last tuned on {@code lastHz} in {@code lastBand}, must tune again for {@code hz}. */
    public static boolean needsRetune(float lastHz, String lastBand, float hz, String band) {
        if(lastBand == null || !lastBand.equals(band))
            return true;
        return Math.abs(hz - lastHz) >= RETUNE_HZ;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

}

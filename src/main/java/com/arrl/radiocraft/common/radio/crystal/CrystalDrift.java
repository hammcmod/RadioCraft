package com.arrl.radiocraft.common.radio.crystal;

/**
 * The slow wander of a poor crystal: two slow waves at unrelated periods, so the radio creeps off frequency and back
 * over a few minutes instead of swinging back and forth on a fixed beat. Worked out from the game time, so the server
 * and every client agree without syncing anything.
 */
public final class CrystalDrift {

    private CrystalDrift() {}

    /** Period of the slow wave, in ticks (about 3 minutes). */
    static final double SLOW_PERIOD_TICKS = 3_700.0D;
    /** Period of the faster wave, in ticks (about 70 seconds). */
    static final double FAST_PERIOD_TICKS = 1_390.0D;
    static final double SLOW_SHARE = 0.65D;

    /**
     * Hz off frequency at {@code gameTime}, between -{@code maxHz} and +{@code maxHz}.
     *
     * @param seed Anything that tells radios apart (a block position, a player), so they do not drift in step.
     */
    public static double offsetHz(long gameTime, long seed, double maxHz) {
        if(maxHz <= 0.0D)
            return 0.0D;
        double slowPhase = (seed & 0xFFFF) / 65_536.0D * Math.PI * 2.0D;
        double fastPhase = ((seed >>> 16) & 0xFFFF) / 65_536.0D * Math.PI * 2.0D;
        double wave = SLOW_SHARE * Math.sin(gameTime / SLOW_PERIOD_TICKS * Math.PI * 2.0D + slowPhase)
                + (1.0D - SLOW_SHARE) * Math.sin(gameTime / FAST_PERIOD_TICKS * Math.PI * 2.0D + fastPhase);
        return wave * maxHz;
    }
}

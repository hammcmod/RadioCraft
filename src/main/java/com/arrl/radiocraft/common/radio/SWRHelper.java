package com.arrl.radiocraft.common.radio;

/**
 * SWR arithmetic shared by antennas, radios and the antenna tuner. Pure functions: the config values they need are
 * passed in so the maths can be unit tested without a game bootstrap.
 */
public class SWRHelper {

    /** Used for "no antenna" and "more than one antenna" (overdraw): nothing a tuner can fix. */
    public static final double OVERDRAW_SWR = 10.0D;

    /**
     * Fraction of the forward power that is not reflected back down the feedline, {@code 1 - rho^2}: 1.0 at SWR 1,
     * 0.89 at SWR 2, 0.75 at SWR 3, 0.33 at SWR 10.
     */
    public static double getEfficiencyMultiplier(double swr) {
        return 1.0D - ((swr-1) / (swr+1)) * ((swr-1) / (swr+1));
    }

    /**
     * The SWR a radio sees through an antenna tuner. A tuner brings any antenna whose own SWR is at most
     * {@code maxMatchable} to a flat 1:1 match; an antenna further off resonance is beyond the tuner's range and the
     * radio sees the antenna's own SWR, as if the tuner were not there. 0 (no antenna) is passed through.
     *
     * @param rawSwr The SWR the antenna itself presents at the frequency in use.
     * @param maxMatchable The highest SWR the tuner can match.
     */
    public static double tunedSWR(double rawSwr, double maxMatchable) {
        if(rawSwr <= 0.0D)
            return rawSwr;
        return rawSwr <= maxMatchable ? 1.0D : rawSwr;
    }

    /**
     * Fraction of the transmit power that reaches the antenna through an antenna tuner. Within the tuner's range the
     * power that a mismatch would otherwise reflect is mostly recovered, but not all of it: {@code lossFraction} of the
     * mismatch loss is still lost in the tuner, so an antenna further off resonance still costs more. Beyond the
     * tuner's range this is just {@link #getEfficiencyMultiplier(double)}.
     *
     * @param rawSwr The SWR the antenna itself presents at the frequency in use.
     * @param maxMatchable The highest SWR the tuner can match.
     * @param lossFraction 0 to 1, the share of the mismatch loss the tuner cannot recover (0.25 = it recovers 3/4).
     */
    public static double tunedEfficiency(double rawSwr, double maxMatchable, double lossFraction) {
        double raw = getEfficiencyMultiplier(rawSwr);
        if(rawSwr <= 0.0D || rawSwr > maxMatchable)
            return raw;
        return 1.0D - (1.0D - raw) * lossFraction;
    }

    /**
     * Chance per second that a transmitter keyed into {@code swr} burns out its finals. Nothing below
     * {@code threshold}; from there it follows the reflected power ({@code 1 - efficiency}), scaled so that SWR
     * {@link #OVERDRAW_SWR} gives {@code chanceAtWorst}: with the defaults (3, 0.05) about 0.019 at SWR 3, 0.038 at
     * SWR 6 and 0.05 at SWR 10. 0 (no antenna, off) never blows.
     *
     * @param swr The SWR the radio sees (after any tuner).
     * @param threshold The lowest SWR that can blow a radio.
     * @param chanceAtWorst The chance per second at SWR {@link #OVERDRAW_SWR}.
     */
    public static double blowChancePerSecond(double swr, double threshold, double chanceAtWorst) {
        if(swr <= 0.0D || swr < threshold)
            return 0.0D;
        double reflected = 1.0D - getEfficiencyMultiplier(swr);
        double worst = 1.0D - getEfficiencyMultiplier(OVERDRAW_SWR);
        return Math.min(1.0D, chanceAtWorst * reflected / worst);
    }

}

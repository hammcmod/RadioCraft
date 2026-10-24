package com.arrl.radiocraft.common.radio;

/**
 * Util class for helper methods regarding bands and ranges.
 */
public class BandUtils {

	public static double getBaseStrength(Band band, double distance, double losEfficiency, double skipEfficiency, boolean isDay) {
		return getBaseStrength(band, distance, losEfficiency, skipEfficiency, isDay, 1.0D, 1.0D);
	}

	/**
	 * {@link #getBaseStrength(Band, double, double, double, boolean)} for an antenna whose pattern changes the band's
	 * reach: line of sight reaches {@code losScale} times as far, and skip reaches {@code maxSkipScale} times as far
	 * (the far edge of the skip zone moves out; the near edge stays).
	 */
	public static double getBaseStrength(Band band, double distance, double losEfficiency, double skipEfficiency, boolean isDay,
	                                     double losScale, double maxSkipScale) {
		if(band == null)
			return 0.0D;

		double los = band.losRange() * losScale;
		if(distance < los)
			return (1 - (distance / los)) * losEfficiency; // If within LoS scale the strength linearly to 0 as it reaches LoS

		double minSkip = isDay ? band.minSkipDay() : band.minSkipNight();
		double maxSkip = (isDay ? band.maxSkipDay() : band.maxSkipNight()) * maxSkipScale;

		if(distance > minSkip && distance < maxSkip) { // If within skip range
			double skipRadius = (maxSkip - minSkip) / 2;
			double skipMid = minSkip + skipRadius;
			double distFromMid = Math.abs(distance - skipMid);

			return (1 - (distFromMid / skipRadius)) * skipEfficiency; // If in skip range, scale strength linearly to 0 as it reaches radius.
		}

		return 0.0D;
	}

	/**
	 * Strength of a VHF signal scattered off an aurora: {@code peak} across most of the zone from {@code minRange} to
	 * {@code maxRange} blocks, fading to 0 over the outer 15% at each edge. 0 outside the zone.
	 */
	public static double getAuroraStrength(double distance, double minRange, double maxRange, double peak) {
		if(distance <= minRange || distance >= maxRange)
			return 0.0D;
		double fade = (maxRange - minRange) * 0.15D;
		double edge = Math.min(distance - minRange, maxRange - distance);
		return peak * Math.min(1.0D, edge / fade);
	}

    /**
     * How much further than a band's own ranges any antenna, mode or path can carry it: the longest antenna reach
     * (a Yagi divides distance by 2), CW (by 1.5) and the biggest skip scale, with room to spare.
     */
    private static final double REACH_MARGIN = 4.0D;

    /**
     * Furthest a signal on {@code band} can possibly be heard, in blocks: beyond it nothing works out a strength, so a
     * transmission skips far stations with one distance check. Generous on purpose: it only saves work, never decides
     * whether a signal arrives.
     */
    public static double maxReach(Band band) {
        if(band == null)
            return 0.0D;
        double reach = Math.max(band.losRange(), Math.max(band.maxSkipDay(), band.maxSkipNight()));
        if(band.isVhf())
            reach = Math.max(reach, Math.max(com.arrl.radiocraft.RadiocraftServerConfig.AURORA_VHF_MAX_RANGE.get(),
                    com.arrl.radiocraft.RadiocraftServerConfig.SPORADIC_E_MAX_RANGE.get()));
        return reach * REACH_MARGIN;
    }

    /** True if two antennas are too far apart for anything on {@code band} to reach (see {@link #maxReach}). */
    public static boolean outOfReach(Band band, net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        double reach = maxReach(band);
        return a.distSqr(b) > reach * reach;
    }

    public static double getWavelengthMetersFromFrequencyHertz(double frequencyHertz) {
        // The constant c represents the speed of light in a vacuum, which is exactly 299,792,458 meters per second.
        final double SPEED_OF_LIGHT_METERS_PER_SECOND = 299792458D;
        return SPEED_OF_LIGHT_METERS_PER_SECOND / frequencyHertz;
    }

    /**
     * Check if two frequencies are within a tolerance of each other.
     * @param a Frequency in Hz
     * @param b Frequency in Hz
     * @param tolerance Tolerance in Hz
     * @return Whether the frequencies are within tolerance of each other given the tolerance value (Hz)
     */
    public static boolean areFrequenciesEqualWithTolerance(float a, float b, float tolerance) {
        return Math.abs(a - b) < tolerance;
    }
}
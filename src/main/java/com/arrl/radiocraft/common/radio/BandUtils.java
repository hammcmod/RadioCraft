package com.arrl.radiocraft.common.radio;

/**
 * Util class for helper methods regarding bands and ranges.
 */
public class BandUtils {

	public static double getBaseStrength(Band band, double distance, double losEfficiency, double skipEfficiency, boolean isDay) {
		if(band == null)
			return 0.0D;

		int los = band.losRange();
		if(distance < los)
			return (1 - (distance / los)) * losEfficiency; // If within LoS scale the strength linearly to 0 as it reaches LoS

		double minSkip = isDay ? band.minSkipDay() : band.minSkipNight();
		double maxSkip = isDay ? band.maxSkipDay()  : band.maxSkipNight();

		if(distance > minSkip && distance < maxSkip) { // If within skip range
			double skipRadius = (maxSkip - minSkip) / 2;
			double skipMid = minSkip + skipRadius;
			double distFromMid = Math.abs(distance - skipMid);

			return (1 - (distFromMid / skipRadius)) * skipEfficiency; // If in skip range, scale strength linearly to 0 as it reaches radius.
		}

		return 0.0D;
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
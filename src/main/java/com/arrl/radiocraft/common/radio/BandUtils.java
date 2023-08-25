package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.common.init.RadiocraftData;

/**
 * Util class for helper methods regarding bands and ranges.
 */
public class BandUtils {

	public static double getSSBBaseStrength(int wavelength, double distance, double losEfficiency, double skipEfficiency, boolean isDay) {
		Band band = RadiocraftData.BANDS.getValue(wavelength);
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

	public static double getCWBaseStrength(int wavelength, double distance, double losEfficiency, double skipEfficiency, boolean isDay) {
		Band band = RadiocraftData.BANDS.getValue(wavelength);
		if(band == null)
			return 0.0D;

		int los = (int)(band.losRange() * 1.5D);
		if(distance < los)
			return (1 - (distance / los)) * losEfficiency; // If within LoS scale the strength linearly to 0 as it reaches LoS

		double minSkip = (isDay ? band.minSkipDay() : band.minSkipNight()) * 1.5D;
		double maxSkip = (isDay ? band.maxSkipDay()  : band.maxSkipNight()) * 1.5D;

		if(distance > minSkip && distance < maxSkip) { // If within skip range
			double skipRadius = (maxSkip - minSkip) / 2;
			double skipMid = minSkip + skipRadius;
			double distFromMid = Math.abs(distance - skipMid);

			return (1 - (distFromMid / skipRadius)) * skipEfficiency; // If in skip range, scale strength linearly to 0 as it reaches radius.
		}

		return 0.0D;
	}

}

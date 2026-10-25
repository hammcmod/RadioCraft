package com.arrl.radiocraft.api.weather;

import java.util.Set;

/**
 * The weather at one place and time, as a {@link WeatherSource} reports it: what a weather station would measure.
 *
 * @param precipitation what is falling (or blowing)
 * @param rain          0..1, how hard
 * @param thunder       0..1
 * @param windSpeed     metres per second
 * @param windFrom      compass degrees the wind comes from (0 north, 90 east), as weather reports give it
 * @param temperature   degrees Celsius
 * @param pressure      hectopascals
 * @param humidity      relative humidity, percent
 * @param hazards       dangerous weather here (never null)
 */
public record WeatherReport(Precipitation precipitation, float rain, float thunder, double windSpeed, double windFrom,
		double temperature, double pressure, double humidity, Set<Hazard> hazards) {

	/** What is falling or blowing. */
	public enum Precipitation { NONE, RAIN, SNOW, HAIL, DUST }

	/** Dangerous weather. */
	public enum Hazard { LIGHTNING, HAIL, BLIZZARD, SANDSTORM, DAMAGING_WIND, HURRICANE_EYE }

	public static final WeatherReport CALM = new WeatherReport(Precipitation.NONE, 0, 0, 0, 0, 15, 1013.25, 50, Set.of());

	public boolean has(Hazard hazard) {
		return hazards.contains(hazard);
	}

	/** Wind force on the Beaufort scale, 0 (calm) to 12 (hurricane). */
	public int beaufort() {
		double[] limits = { 0.5, 1.5, 3.3, 5.5, 7.9, 10.7, 13.8, 17.1, 20.7, 24.4, 28.4, 32.6 };
		for(int force = 0; force < limits.length; force++)
			if(windSpeed < limits[force])
				return force;
		return 12;
	}
}

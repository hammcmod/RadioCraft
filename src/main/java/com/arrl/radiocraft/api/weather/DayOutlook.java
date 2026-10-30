package com.arrl.radiocraft.api.weather;

/**
 * What a day holds at a place, as a weather source forecasts it ahead of time: "rain likely tomorrow".
 *
 * @param type         the kind of day
 * @param tornadoWatch a tornado is possible that day, though probably will not happen
 */
public record DayOutlook(Type type, boolean tornadoWatch) {

	/** For sources that cannot say. */
	public static final DayOutlook UNKNOWN = new DayOutlook(Type.UNKNOWN, false);

	public enum Type {
		/** The source does not forecast days. */
		UNKNOWN,
		/** No rain: clear skies, a few clouds. */
		DRY,
		/** A chance of rain. */
		CHANCE,
		/** Rain likely: storms more often and longer. */
		RAINY,
		/** Cloudy with no storms (where two kinds of day meet). */
		CLOUDY;

		/** How a forecast puts it, in capitals: "RAIN LIKELY". Empty for {@link #UNKNOWN}. */
		public String words() {
			return switch(this) {
				case UNKNOWN -> "";
				case DRY -> "DRY";
				case CHANCE -> "CHANCE OF RAIN";
				case RAINY -> "RAIN LIKELY";
				case CLOUDY -> "CLOUDY, NO STORMS";
			};
		}
	}
}

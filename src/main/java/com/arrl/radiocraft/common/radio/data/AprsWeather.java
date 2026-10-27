package com.arrl.radiocraft.common.radio.data;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * An APRS weather report read back from a beacon: the positionless weather data (APRS 1.0.1, chapter 12) that a
 * station with a weather station puts at the start of its beacon text, as {@link
 * com.arrl.radiocraft.common.radio.weather.WeatherReports#aprsWeather} writes it, e.g.
 * {@code _10241430c270s012g018t064r000h85b10040}. A field the station does not report is null.
 *
 * @param windFrom   degrees the wind comes from
 * @param speed      wind speed, mph
 * @param gust       highest gust in the last few minutes, mph
 * @param fahrenheit temperature, °F
 * @param rainHour   rain in the last hour, hundredths of an inch
 * @param humidity   relative humidity, percent
 * @param pressure   barometric pressure, millibars
 */
public record AprsWeather(@Nullable Integer windFrom, @Nullable Integer speed, @Nullable Integer gust,
		@Nullable Integer fahrenheit, @Nullable Integer rainHour, @Nullable Integer humidity, @Nullable Double pressure) {

	/** True if {@code text} starts with weather data. */
	public static boolean isWeather(String text) {
		return text.length() > 9 && text.charAt(0) == '_' && Character.isDigit(text.charAt(1));
	}

	/** The weather data at the start of a beacon's text: up to the first space. */
	public static String dataOf(String text) {
		if(!isWeather(text))
			return "";
		int space = text.indexOf(' ');
		return space < 0 ? text : text.substring(0, space);
	}

	/** What follows the weather data in a beacon's text: the station's own comment. */
	public static String commentOf(String text) {
		if(!isWeather(text))
			return text;
		int space = text.indexOf(' ');
		return space < 0 ? "" : text.substring(space + 1).strip();
	}

	/** The weather at the start of {@code text}, or null if there is none. */
	public static @Nullable AprsWeather parse(String text) {
		String data = dataOf(text);
		if(data.isEmpty())
			return null;
		return new AprsWeather(field(data, 'c', 3), field(data, 's', 3), field(data, 'g', 3), field(data, 't', 3),
				field(data, 'r', 3), humidity(field(data, 'h', 2)), pressure(field(data, 'b', 5)));
	}

	/** The number of {@code digits} after the letter {@code key} (past the timestamp), or null if absent or dotted out. */
	private static @Nullable Integer field(String data, char key, int digits) {
		int at = data.indexOf(key, 9);
		if(at < 0 || at + 1 + digits > data.length())
			return null;
		String value = data.substring(at + 1, at + 1 + digits);
		try {
			return Integer.parseInt(value);
		}
		catch(NumberFormatException e) {
			return null; // "..." means not reported.
		}
	}

	private static @Nullable Integer humidity(@Nullable Integer h) {
		return h == null ? null : h == 0 ? 100 : h;
	}

	private static @Nullable Double pressure(@Nullable Integer b) {
		return b == null ? null : b / 10.0;
	}

	/** One short line for the station list: {@code WX 270/12G18 64F 85% 1004MB}. */
	public String summary() {
		StringBuilder out = new StringBuilder("WX");
		if(windFrom != null && speed != null)
			out.append(String.format(Locale.ROOT, " %03d/%d", windFrom, speed)).append(gust != null && gust > speed ? "G" + gust : "");
		if(fahrenheit != null)
			out.append(' ').append(fahrenheit).append('F');
		if(humidity != null)
			out.append(' ').append(humidity).append('%');
		if(pressure != null)
			out.append(String.format(Locale.ROOT, " %.0fMB", pressure));
		if(rainHour != null && rainHour > 0)
			out.append(String.format(Locale.ROOT, " R%.2f", rainHour / 100.0));
		return out.toString();
	}

	/** Wind, for a station's details: {@code 270 AT 12 MPH, GUSTS 18}. */
	public String windLine() {
		if(windFrom == null || speed == null)
			return "-";
		if(speed == 0)
			return "CALM";
		return String.format(Locale.ROOT, "%03d AT %d MPH", windFrom, speed) + (gust != null && gust > speed ? ", GUSTS " + gust : "");
	}

	/** Temperature, humidity, pressure and rain, for a station's details: {@code 64F 85% 1004.0MB RAIN 0.00IN}. */
	public String airLine() {
		return (fahrenheit == null ? "" : fahrenheit + "F ") + (humidity == null ? "" : humidity + "% ")
				+ (pressure == null ? "" : String.format(Locale.ROOT, "%.1fMB ", pressure))
				+ (rainHour == null ? "" : String.format(Locale.ROOT, "RAIN %.2fIN", rainHour / 100.0));
	}
}

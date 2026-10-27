package com.arrl.radiocraft.common.radio.weather;

import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Weather in words and numbers, the way stations send it: a teletype-style text report (also printed on WEFAX
 * charts' backs) and the APRS positionless weather format.
 */
public final class WeatherReports {

	private WeatherReports() {
	}

	/** Vanilla biome temperature to degrees Celsius: plains (0.8) is 20, desert (2) is 40, snowy plains (0) is 0. */
	public static double celsius(float biomeTemperature) {
		return biomeTemperature <= 1 ? 25 * biomeTemperature : 25 + 15 * (biomeTemperature - 1);
	}

	/** 0 at noon, 1 at midnight. */
	public static double night(long dayTime) {
		double phase = (((dayTime - 6000) % 24000) + 24000) % 24000 / 24000.0;
		return 0.5 * (1 - Math.cos(2 * Math.PI * phase));
	}

	/** "D12 1430": the in-game day and time, as fax headers and reports give it. */
	public static String timestamp(long dayTime) {
		long day = dayTime / 24000 + 1;
		long t = ((dayTime % 24000) + 24000) % 24000;
		long hour = (t / 1000 + 6) % 24;
		long minute = (t % 1000) * 60 / 1000;
		return String.format(Locale.ROOT, "D%d %02d%02d", day, hour, minute);
	}

	/** "12 MIN", "45 S", "2 HR": a time ahead, in report style. */
	public static String duration(long ticks) {
		long seconds = Math.max(0, ticks / 20);
		if(seconds < 90)
			return seconds + " S";
		long minutes = Math.round(seconds / 60.0);
		if(minutes < 120)
			return minutes + " MIN";
		return Math.round(minutes / 60.0) + " HR";
	}

	/** "HEAVY RAIN, WIND 270/12 MS": what is falling and the wind. */
	public static String brief(WeatherReport report) {
		String what = report.precipitation() == WeatherReport.Precipitation.NONE ? report.rain() > 0.2 ? "OVERCAST" : "FAIR"
				: report.precipitation() == WeatherReport.Precipitation.DUST ? "BLOWING DUST" : report.precipitation().name();
		if(report.rain() > 0.75 && report.precipitation() != WeatherReport.Precipitation.NONE)
			what = "HEAVY " + what;
		String wind = report.windSpeed() < 0.5 ? "CALM"
				: String.format(Locale.ROOT, "WIND %03d/%d MS", Math.round(report.windFrom()) % 360, Math.round(report.windSpeed()));
		return what + ", " + wind;
	}

	/** "HEAVY RAIN, WIND 270/12 MS, 18C, 1004 HPA, 85 PCT": one line for the weather now. */
	public static String summary(WeatherReport report) {
		return String.format(Locale.ROOT, "%s, %dC, %d HPA, %d PCT", brief(report), Math.round(report.temperature()),
				Math.round(report.pressure()), Math.round(report.humidity()));
	}

	/** "HAIL, DAMAGING WIND", or null when there is nothing dangerous. */
	public static String hazards(WeatherReport report) {
		if(report.hazards().isEmpty())
			return null;
		List<String> names = new ArrayList<>();
		for(WeatherReport.Hazard hazard : WeatherReport.Hazard.values())
			if(report.has(hazard))
				names.add(hazard.name().replace('_', ' '));
		return String.join(", ", names);
	}

	/**
	 * A full text report for {@code pos}: a header, the weather now, any warning, the next storm and how many storms
	 * are about. Short lines, capitals, like a teletype weather broadcast.
	 */
	public static List<String> lines(ServerLevel level, WeatherSource source, BlockPos pos, String station) {
		long now = level.getGameTime();
		List<String> lines = new ArrayList<>();
		lines.add("WX " + (station.isEmpty() ? "" : station + " ") + timestamp(level.getDayTime()));
		lines.add(String.format(Locale.ROOT, "AT %d %d", pos.getX(), pos.getZ()));
		WeatherReport here = source.report(level, pos);
		lines.add("NOW " + summary(here));
		String warning = hazards(here);
		if(warning != null)
			lines.add("WARNING: " + warning);
		long horizon = source.forecastHorizon(level);
		Optional<WeatherSource.Arrival> next = source.nextArrival(level, pos, horizon);
		if(next.isPresent()) {
			WeatherSource.Arrival a = next.get();
			StormReport storm = a.storm();
			String name = storm.label() + (storm.worldWide() ? "" : " S" + storm.id());
			if(a.arrivalTick() <= now)
				lines.add(name + " OVERHEAD, ENDS IN " + duration(a.departureTick() - now));
			else
				lines.add(name + " ARRIVES IN " + duration(a.arrivalTick() - now) + ", LASTS "
						+ duration(a.departureTick() - a.arrivalTick()));
			String peakHazards = hazards(a.peak());
			lines.add("WORST " + brief(a.peak()) + (peakHazards == null ? "" : ", " + peakHazards));
		}
		else
			lines.add("NO STORMS EXPECTED IN NEXT " + duration(horizon));
		if(source.isLocal()) {
			int count = source.storms(level, pos, 1000, false).size();
			lines.add(count == 0 ? "NO STORMS WITHIN 1000 BLOCKS" : count + (count == 1 ? " STORM" : " STORMS") + " WITHIN 1000 BLOCKS");
		}
		return lines;
	}

	/**
	 * {@link #aprsWeather(WeatherReport, int, int, int, int, double, double)} stamped with the level's date and time
	 * (28-day months, the day starting at 6:00 like a Minecraft morning).
	 */
	public static String aprsWeather(ServerLevel level, WeatherReport report, double gust, double rainHour) {
		long t = ((level.getDayTime() % 24000) + 24000) % 24000;
		long days = level.getDayTime() / 24000;
		return aprsWeather(report, (int)(days / 28 % 12) + 1, (int)(days % 28) + 1, (int)((t / 1000 + 6) % 24),
				(int)(t % 1000 * 60 / 1000), gust, rainHour);
	}

	/**
	 * The APRS positionless weather report (APRS 1.0.1, chapter 12), e.g. {@code _10241430c270s012g018t064r000h85b10040}:
	 * month, day, hour and minute, wind direction (degrees), speed and gust (mph), temperature (Fahrenheit), rain in
	 * the last hour (hundredths of an inch), humidity (percent, 00 for 100) and pressure (tenths of a millibar).
	 *
	 * @param month    1..12
	 * @param gust     metres per second
	 * @param rainHour rain in the last hour, millimetres
	 */
	public static String aprsWeather(WeatherReport report, int month, int day, int hour, int minute, double gust, double rainHour) {
		int dir = (int)Math.round(report.windFrom()) % 360;
		int speed = (int)Math.round(report.windSpeed() * 2.23694);
		int gustMph = (int)Math.round(Math.max(gust, report.windSpeed()) * 2.23694);
		int fahrenheit = (int)Math.round(report.temperature() * 9 / 5 + 32);
		int rain = (int)Math.round(rainHour / 0.254);
		int humidity = (int)Math.round(report.humidity());
		int pressure = (int)Math.round(report.pressure() * 10);
		String temp = fahrenheit < 0 ? String.format(Locale.ROOT, "-%02d", -fahrenheit) : String.format(Locale.ROOT, "%03d", fahrenheit);
		return String.format(Locale.ROOT, "_%02d%02d%02d%02dc%03ds%03dg%03dt%sr%03dh%02db%05d", month, day, hour, minute, dir,
				Math.min(999, speed), Math.min(999, gustMph), temp, Math.min(999, rain), humidity >= 100 ? 0 : humidity, pressure);
	}
}

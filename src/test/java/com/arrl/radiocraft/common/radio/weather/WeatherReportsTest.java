package com.arrl.radiocraft.common.radio.weather;

import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeatherReportsTest {

	private static final WeatherReport STORMY = new WeatherReport(WeatherReport.Precipitation.HAIL, 0.9f, 1, 12.0, 270, 18.0,
			1004.2, 85, Set.of(WeatherReport.Hazard.HAIL, WeatherReport.Hazard.LIGHTNING));

	@Test
	void aprsWeatherPacket() {
		// 12 m/s is 27 mph; 18 C is 64 F; 2.54 mm is 0.10 in.
		assertEquals("_10241430c270s027g035t064r010h85b10042", WeatherReports.aprsWeather(STORMY, 10, 24, 14, 30, 15.6, 2.54));
		WeatherReport cold = new WeatherReport(WeatherReport.Precipitation.SNOW, 0.5f, 0, 0, 0, -20, 1013, 100, Set.of());
		assertEquals("_01010000c000s000g000t-04r000h00b10130", WeatherReports.aprsWeather(cold, 1, 1, 0, 0, 0, 0));
	}

	@Test
	void textLines() {
		assertEquals("HEAVY HAIL, WIND 270/12 MS, 18C, 1004 HPA, 85 PCT", WeatherReports.summary(STORMY));
		assertEquals("LIGHTNING, HAIL", WeatherReports.hazards(STORMY));
		assertNull(WeatherReports.hazards(WeatherReport.CALM));
		assertEquals("FAIR, CALM", WeatherReports.brief(WeatherReport.CALM));
	}

	@Test
	void timesReadLikeReports() {
		assertEquals("D1 0600", WeatherReports.timestamp(0));
		assertEquals("D2 1430", WeatherReports.timestamp(24000 + 8500));
		assertEquals("45 S", WeatherReports.duration(900));
		assertEquals("12 MIN", WeatherReports.duration(12 * 1200));
		assertEquals("3 HR", WeatherReports.duration(3 * 72000));
		assertEquals(0, WeatherReports.night(6000), 1e-9);
		assertEquals(20, WeatherReports.celsius(0.8f), 1e-6);
	}

	@Test
	void stormTrackInterpolates() {
		StormReport storm = new StormReport(3, StormReport.Kind.THUNDERSTORM, false, 0, List.of(
				new StormReport.TrackPoint(100, 0, 0, 100, 0.5f), new StormReport.TrackPoint(200, 100, -50, 200, 1f)));
		StormReport.TrackPoint mid = storm.at(150);
		assertEquals(50, mid.x(), 1e-9);
		assertEquals(-25, mid.z(), 1e-9);
		assertEquals(150, mid.radius(), 1e-9);
		assertEquals(0, storm.at(0).x(), 1e-9);
		assertEquals(100, storm.at(999).x(), 1e-9);
		assertTrue(storm.formed(0));
	}
}

package com.arrl.radiocraft.common.radio.data;

import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.common.radio.weather.WeatherReports;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AprsWeatherTest {

	@Test
	void readsBackWhatWeatherReportsWrites() {
		WeatherReport stormy = new WeatherReport(WeatherReport.Precipitation.HAIL, 1, 1, 12, 270, 18, 1004.2, 85, Set.of());
		String data = WeatherReports.aprsWeather(stormy, 10, 24, 14, 30, 15.6, 2.54);
		AprsWeather weather = AprsWeather.parse(data + " ROOF WX");
		assertEquals(new AprsWeather(270, 27, 35, 64, 10, 85, 1004.2), weather);
		assertEquals("WX 270/27G35 64F 85% 1004MB R0.10", weather.summary());
		assertEquals("270 AT 27 MPH, GUSTS 35", weather.windLine());
		assertEquals("ROOF WX", AprsWeather.commentOf(data + " ROOF WX"));
	}

	@Test
	void coldAndSaturated() {
		WeatherReport cold = new WeatherReport(WeatherReport.Precipitation.SNOW, 0.5f, 0, 0, 0, -20, 1013, 100, Set.of());
		AprsWeather weather = AprsWeather.parse(WeatherReports.aprsWeather(cold, 1, 1, 0, 0, 0, 0));
		assertEquals(-4, weather.fahrenheit());
		assertEquals(100, weather.humidity());
		assertEquals("CALM", weather.windLine());
	}

	@Test
	void plainCommentsAreNotWeather() {
		assertNull(AprsWeather.parse("HELLO FROM THE HILL"));
		assertNull(AprsWeather.parse("_UNDERSCORE"));
		assertEquals("HELLO", AprsWeather.commentOf("HELLO"));
	}
}

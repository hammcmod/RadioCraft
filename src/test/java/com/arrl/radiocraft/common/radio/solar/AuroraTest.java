package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.common.radio.BandUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AuroraTest {

	@Test
	void zoneIsBothSidesOfTheEquator() {
		assertTrue(Aurora.inZone(10000, 10000));
		assertTrue(Aurora.inZone(-10000, 10000));
		assertTrue(Aurora.inZone(-250000, 10000));
		assertFalse(Aurora.inZone(9999, 10000));
		assertFalse(Aurora.inZone(-9999, 10000));
		assertFalse(Aurora.inZone(0, 10000));
		assertTrue(Aurora.inZone(Integer.MIN_VALUE, 10000), "no overflow at the world's edge");
	}

	@Test
	void auroraCostsOneConditionStep() {
		assertEquals(BandCondition.FAIR, BandCondition.GOOD.worse());
		assertEquals(BandCondition.POOR, BandCondition.FAIR.worse());
		assertEquals(BandCondition.CLOSED, BandCondition.POOR.worse());
		assertEquals(BandCondition.CLOSED, BandCondition.CLOSED.worse());
	}

	@Test
	void vhfAuroraStrengthIsFlatWithSoftEdges() {
		assertEquals(0.0D, BandUtils.getAuroraStrength(400, 500, 4000, 0.8D));
		assertEquals(0.0D, BandUtils.getAuroraStrength(500, 500, 4000, 0.8D));
		assertEquals(0.0D, BandUtils.getAuroraStrength(4500, 500, 4000, 0.8D));
		assertEquals(0.8D, BandUtils.getAuroraStrength(2000, 500, 4000, 0.8D), 1e-9);
		assertEquals(0.8D, BandUtils.getAuroraStrength(3400, 500, 4000, 0.8D), 1e-9);
		double nearEdge = BandUtils.getAuroraStrength(3900, 500, 4000, 0.8D);
		assertTrue(nearEdge > 0.0D && nearEdge < 0.8D, "fades near the far edge, was " + nearEdge);
	}

	@Test
	void forecastRollsAgainstTheChance() {
		AuroraForecast forecast = new AuroraForecast(0.3D, 72000, 24000);
		assertTrue(forecast.hits(0.0D));
		assertTrue(forecast.hits(0.29D));
		assertFalse(forecast.hits(0.3D));
		assertFalse(forecast.hits(0.99D));
		assertFalse(new AuroraForecast(0.0D, 0, 1).hits(0.0D));
	}

	@Test
	void badForecastIsAnError() {
		assertThrows(JsonParseException.class, () -> new AuroraForecast(1.5D, 0, 1));
		assertThrows(JsonParseException.class, () -> new AuroraForecast(0.5D, -1, 1));
		assertThrows(JsonParseException.class, () -> new AuroraForecast(0.5D, 0, 0));
	}

	/** The shipped blackout: a 30% chance of an aurora three days after it starts, lasting a day. */
	@Test
	void shippedBlackoutForecastsAnAurora() throws IOException {
		JsonObject json;
		try(InputStream in = AuroraTest.class.getResourceAsStream("/data/radiocraft/solar_events/blackout.json")) {
			assertNotNull(in, "blackout.json is not on the classpath");
			json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		AuroraForecast forecast = AuroraForecast.parse(json.getAsJsonObject("aurora"));
		assertEquals(new AuroraForecast(0.3D, 72000, 24000), forecast);
	}
}

package com.arrl.radiocraft.common.radio.solar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SolarEventTest {

	// The six shipped events, by noise floor.
	private static final SolarEvent GREAT = new SolarEvent(0.1F, 1, 1);
	private static final SolarEvent GOOD = new SolarEvent(0.2F, 1, 1);
	private static final SolarEvent AVERAGE = new SolarEvent(0.3F, 1, 1);
	private static final SolarEvent FAIR = new SolarEvent(0.5F, 1, 1);
	private static final SolarEvent POOR = new SolarEvent(0.7F, 1, 1);
	private static final SolarEvent BLACKOUT = new SolarEvent(1.0F, 1, 1);
	private static final List<SolarEvent> ALL = List.of(AVERAGE, BLACKOUT, GREAT, POOR, GOOD, FAIR);

	@Test
	void conditionLevelRunsFromBlackoutToGreat() {
		assertEquals(1, SolarEvent.conditionLevel(ALL, BLACKOUT));
		assertEquals(2, SolarEvent.conditionLevel(ALL, POOR));
		assertEquals(3, SolarEvent.conditionLevel(ALL, FAIR));
		assertEquals(4, SolarEvent.conditionLevel(ALL, AVERAGE));
		assertEquals(5, SolarEvent.conditionLevel(ALL, GOOD));
		assertEquals(6, SolarEvent.conditionLevel(ALL, GREAT));
	}

	@Test
	void noEventIsLevelZero() {
		assertEquals(0, SolarEvent.conditionLevel(ALL, null));
	}

	@Test
	void eventsWithTheSameNoiseShareALevel() {
		SolarEvent twin = new SolarEvent(0.3F, 1, 1);
		List<SolarEvent> withTwin = List.of(BLACKOUT, AVERAGE, twin, GREAT);
		assertEquals(SolarEvent.conditionLevel(withTwin, AVERAGE), SolarEvent.conditionLevel(withTwin, twin));
		assertEquals(4, SolarEvent.conditionLevel(withTwin, GREAT));
	}

	@Test
	void bandsLeftOutOfTheDataAreGood() {
		SolarEvent event = new SolarEvent(0.5F, 1, 1, Map.of("10m", BandCondition.CLOSED), Map.of());
		assertEquals(BandCondition.CLOSED, event.getCondition("10m", true));
		assertEquals(BandCondition.GOOD, event.getCondition("20m", true));
		assertEquals(BandCondition.GOOD, event.getCondition("10m", false), "the night table is separate");
		assertFalse(event.isBandOpen("10m", true));
		assertTrue(event.isBandOpen("10m", false));
	}

	@Test
	void parsesConditionsInAnyCase() {
		JsonObject json = JsonParser.parseString("{\"40m\": \"Poor\", \"80m\": \"closed\"}").getAsJsonObject();
		assertEquals(Map.of("40m", BandCondition.POOR, "80m", BandCondition.CLOSED), SolarEvent.parseConditions(json));
	}

	@Test
	void unknownConditionIsAnError() {
		JsonObject json = JsonParser.parseString("{\"40m\": \"open\"}").getAsJsonObject();
		assertThrows(JsonParseException.class, () -> SolarEvent.parseConditions(json));
	}

	@Test
	void conditionsGetWeakerInOrder() {
		BandCondition[] order = { BandCondition.GOOD, BandCondition.FAIR, BandCondition.POOR, BandCondition.CLOSED };
		for(int i = 1; i < order.length; i++)
			assertTrue(order[i].getSkipMultiplier() < order[i - 1].getSkipMultiplier(), order[i] + " should be weaker than " + order[i - 1]);
		assertTrue(BandCondition.CLOSED.getSkipMultiplier() > 0, "closed weakens rather than cuts off");
	}

	/** The shipped blackout: every band closed by day, 40m and 80m still workable (poor) at night. */
	@Test
	void shippedBlackoutClosesEverythingByDay() throws IOException {
		JsonObject bands;
		try(InputStream in = SolarEventTest.class.getResourceAsStream("/data/radiocraft/solar_events/blackout.json")) {
			assertNotNull(in, "blackout.json is not on the classpath");
			bands = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("bands");
		}
		Map<String, BandCondition> day = SolarEvent.parseConditions(bands.getAsJsonObject("day"));
		Map<String, BandCondition> night = SolarEvent.parseConditions(bands.getAsJsonObject("night"));
		for(String band : List.of("10m", "20m", "40m", "80m"))
			assertEquals(BandCondition.CLOSED, day.get(band), band + " by day");
		assertEquals(BandCondition.CLOSED, night.get("10m"));
		assertEquals(BandCondition.CLOSED, night.get("20m"));
		assertEquals(BandCondition.POOR, night.get("40m"));
		assertEquals(BandCondition.POOR, night.get("80m"));
	}
}

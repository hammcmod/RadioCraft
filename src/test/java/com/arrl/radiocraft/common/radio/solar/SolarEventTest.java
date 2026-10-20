package com.arrl.radiocraft.common.radio.solar;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolarEventTest {

	// The six shipped events, by noise floor.
	private static final SolarEvent GREAT = new SolarEvent(0.1F, 1, 1, Set.of("10m"));
	private static final SolarEvent GOOD = new SolarEvent(0.2F, 1, 1, Set.of("10m"));
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
	void tenMetersOpensOnlyInGoodWeather() {
		assertTrue(GREAT.isBandOpen("10m"));
		assertTrue(GOOD.isBandOpen("10m"));
		assertFalse(AVERAGE.isBandOpen("10m"));
		assertFalse(BLACKOUT.isBandOpen("10m"));
		assertFalse(GOOD.isBandOpen("20m"), "only normally closed bands are listed");
	}
}

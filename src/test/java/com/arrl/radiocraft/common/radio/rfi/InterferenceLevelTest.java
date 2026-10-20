package com.arrl.radiocraft.common.radio.rfi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterferenceLevelTest {

	@Test
	void startsSilent() {
		assertEquals(0.0D, new InterferenceLevel().get(100L));
	}

	@Test
	void popDecaysOverTicks() {
		InterferenceLevel level = new InterferenceLevel();
		level.add(10L, 0.5D);
		assertEquals(0.5D, level.get(10L), 1e-9);
		assertEquals(0.5D * InterferenceLevel.DECAY_PER_TICK, level.get(11L), 1e-9);
		assertEquals(0.5D * Math.pow(InterferenceLevel.DECAY_PER_TICK, 5), level.get(15L), 1e-9);
		assertEquals(0.0D, level.get(200L), "a pop should be gone after a few seconds");
	}

	@Test
	void readingEarlierTimeDoesNotGrowBack() {
		InterferenceLevel level = new InterferenceLevel();
		level.add(10L, 0.5D);
		double later = level.get(15L);
		assertEquals(later, level.get(12L), 1e-9, "a stale read must not undo the decay");
	}

	@Test
	void clocksSettleBelowOne() {
		InterferenceLevel level = new InterferenceLevel();
		for(long tick = 0; tick < 200; tick++)
			level.add(tick, 0.3D); // A 1-tick clock right on the wire: one full pop every tick.
		double steady = 0.3D / (1.0D - InterferenceLevel.DECAY_PER_TICK);
		assertEquals(Math.min(1.0D, steady), level.get(199L), 1e-6);
	}

	@Test
	void everyOtherTickClockIsSteadyBuzz() {
		InterferenceLevel level = new InterferenceLevel();
		for(long tick = 0; tick < 200; tick += 2)
			level.add(tick, 0.3D);
		assertTrue(level.get(199L) > 0.5D, "a fast clock should keep the level well up, got " + level.get(199L));
	}
}

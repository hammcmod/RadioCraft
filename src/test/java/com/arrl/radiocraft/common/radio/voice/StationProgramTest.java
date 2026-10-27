package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationProgramTest {

	/** Ticks out of {@code ticks} the program spends on the air, sending frames as the module does. */
	private static int ticksOnAir(StationProgram program, long ticks) {
		int on = 0;
		for(long t = 0; t < ticks; t++)
			if(!program.frames(t, t % 2 == 0 ? 3 : 2).isEmpty())
				on++;
		return on;
	}

	@Test
	void beeperBeepsHalfASecondEveryThreeSeconds() {
		StationProgram beeper = new StationProgram(StationProgram.Kind.DF_BEEPER, 1, "W1AW", () -> null);
		int on = ticksOnAir(beeper, StationProgram.BEEPER_PERIOD * 10L);
		assertEquals(10 * 10, on, 10, "about ten ticks on in every sixty");
	}

	@Test
	void deadAirNeverDrops() {
		StationProgram dead = new StationProgram(StationProgram.Kind.DEAD_AIR, 2, "W1AW", () -> null);
		assertEquals(2000, ticksOnAir(dead, 2000));
		assertTrue(dead.onAir());
	}

	@Test
	void numbersStationSendsThenWaits() {
		StationProgram numbers = new StationProgram(StationProgram.Kind.NUMBERS, 3, "W1AW", () -> null);
		int on = ticksOnAir(numbers, StationProgram.NUMBERS_PERIOD);
		assertTrue(on > 20 * 30 && on < StationProgram.NUMBERS_PERIOD / 2, "a broadcast of a minute or so, then quiet: " + on);
		assertFalse(numbers.onAir());
	}

	@Test
	void aVoiceTapeReplacesTheMorse() {
		java.util.List<short[]> tape = java.util.Collections.nCopies(100, RepeaterAudio.silence());
		StationProgram voiced = new StationProgram(StationProgram.Kind.NUMBERS, 3, "W1AW", () -> tape);
		StationProgram morse = new StationProgram(StationProgram.Kind.NUMBERS, 3, "W1AW", () -> null);
		assertTrue(ticksOnAir(voiced, StationProgram.NUMBERS_PERIOD) < ticksOnAir(morse, StationProgram.NUMBERS_PERIOD),
				"two seconds of tape after the interval tune is shorter than ten Morse groups");
	}

	@Test
	void staticAndCwBeaconsTakeTurns() {
		assertTrue(ticksOnAir(new StationProgram(StationProgram.Kind.STATIC_BEACON, 4, "W1AW", () -> null), StationProgram.STATIC_PERIOD * 5L) > 0);
		int cw = ticksOnAir(new StationProgram(StationProgram.Kind.CW_BEACON, 5, "W1AW", () -> null), StationProgram.CW_PERIOD);
		assertTrue(cw > 60 && cw < StationProgram.CW_PERIOD, "the beacon and its dash, then a gap: " + cw);
	}
}

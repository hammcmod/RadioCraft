package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquelchTailTest {

	private static final UUID SOURCE = UUID.randomUUID();
	private static final long MS = 1_000_000L;

	@Test
	void weakSignalsLeaveALongerLouderTail() {
		assertEquals(SquelchTail.SHORTEST_FRAMES, SquelchTail.tailFrames(0.9D), "a strong signal: short and crisp");
		assertEquals(SquelchTail.LONGEST_FRAMES, SquelchTail.tailFrames(0.0D));
		assertTrue(SquelchTail.tailLevel(0.05D) > SquelchTail.tailLevel(0.5D), "weaker is louder");
		int ms = SquelchTail.tailFrames(0.1D) * 20;
		assertTrue(ms >= 120 && ms <= 200, "a tail is 120 to 200 ms, got " + ms);
	}

	@Test
	void theTailIsStaticThatDiesAway() {
		List<short[]> frames = SquelchTail.frames(0.1D, 1.0D, 42L);
		assertEquals(SquelchTail.tailFrames(0.1D), frames.size());
		assertTrue(energy(frames.get(1)) > 0L, "no static");
		assertTrue(energy(frames.get(frames.size() - 1)) < energy(frames.get(1)), "it should die away at the end");
		assertEquals(0L, energy(SquelchTail.frames(0.1D, 0.0D, 42L).get(1)), "a silent receiver plays a silent tail");
	}

	@Test
	void playsOnlyOnceTheSignalHasGoneAndThenOnce() {
		SquelchTail tail = new SquelchTail();
		long t = 1_000 * MS;
		assertTrue(tail.drain(t, 3).isEmpty(), "nothing heard, nothing to play");
		tail.heard(0.2D, 1.0D, SOURCE, t);
		assertTrue(tail.drain(t + 50 * MS, 3).isEmpty(), "still within the gap: the signal may carry on");
		int played = 0;
		for(int tick = 0; tick < 10; tick++) {
			List<short[]> frames = tail.drain(t + (100 + 50L * tick) * MS, 3);
			assertTrue(frames.size() <= 3, "paced a tick's worth at a time");
			played += frames.size();
		}
		assertEquals(SquelchTail.tailFrames(0.2D), played);
		assertEquals(SOURCE, tail.source());
		assertTrue(tail.drain(t + 2_000 * MS, 3).isEmpty(), "one tail per signal");
	}

	@Test
	void aSignalBackInTimeCutsTheTail() {
		SquelchTail tail = new SquelchTail();
		tail.heard(0.2D, 1.0D, SOURCE, 0L);
		assertEquals(3, tail.drain(100 * MS, 3).size());
		tail.heard(0.2D, 1.0D, SOURCE, 120 * MS); // It came back.
		assertTrue(tail.drain(150 * MS, 3).isEmpty(), "the squelch never shut");
	}

	private static long energy(short[] frame) {
		long sum = 0L;
		for(short s : frame)
			sum += Math.abs(s);
		return sum;
	}
}

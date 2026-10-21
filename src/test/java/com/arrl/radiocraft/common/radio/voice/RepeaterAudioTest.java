package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepeaterAudioTest {

	@Test
	void morseTimingFollowsTheStandard() {
		// E = dot; T = dash; "ET": dot, 3 gap, dash.
		assertEquals(List.of(true, false, false, false, true, true, true), RepeaterAudio.morseUnits("ET"));
		// A = dot, gap, dash.
		assertEquals(List.of(true, false, true, true, true), RepeaterAudio.morseUnits("A"));
		// A word gap is seven units.
		assertEquals(1 + 7 + 1, RepeaterAudio.morseUnits("E E").size());
	}

	@Test
	void callsignIsCleanedUp() {
		assertEquals("W1AW/R", RepeaterAudio.sanitizeCallsign(" w1aw/r! "));
		assertEquals(10, RepeaterAudio.sanitizeCallsign("ABCDEFGHIJKLMNOP").length());
	}

	@Test
	void morseFramesAreWholeAndLastAboutAsLongAsTheCode() {
		List<short[]> frames = RepeaterAudio.morse("W1AW");
		int units = RepeaterAudio.morseUnits("W1AW").size();
		double seconds = 0.2D + units * 1.2D / RepeaterAudio.ID_WPM;
		assertEquals(Math.ceil(seconds / 0.02D), frames.size(), 1.0D);
		for(short[] frame : frames)
			assertEquals(RepeaterAudio.FRAME, frame.length);
		assertTrue(RepeaterAudio.morse("").isEmpty());
	}

	@Test
	void beepAndNoiseHaveSound() {
		List<short[]> beep = RepeaterAudio.courtesyBeep();
		assertTrue(beep.size() > 5);
		short peak = 0;
		for(short[] frame : beep)
			for(short s : frame)
				peak = (short)Math.max(peak, Math.abs(s));
		assertTrue(peak > 5000);
		short[] noise = RepeaterAudio.noise(0.3D, 42L);
		assertArrayEquals(noise, RepeaterAudio.noise(0.3D, 42L));
		assertNotEquals(0, noise[100]);
	}
}

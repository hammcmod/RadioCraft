package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.common.radio.data.TncPacket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfskTest {

	@Test
	void burstLastsAsLongAsItsBitsAt1200Baud() {
		int bytes = 40;
		List<short[]> frames = Afsk.burst(bytes, 42L);
		assertEquals(Afsk.frameCount(bytes), frames.size());
		double seconds = (Afsk.PREAMBLE_FLAGS + bytes + Afsk.TAIL_FLAGS) * 8 / Afsk.BAUD;
		assertEquals(seconds, frames.size() * 0.02D, 0.02D);
	}

	@Test
	void burstIsLoudButDoesNotClip() {
		int peak = 0;
		for(short[] frame : Afsk.burst(20, 7L))
			for(short sample : frame)
				peak = Math.max(peak, Math.abs(sample));
		assertTrue(peak > 8000 && peak < 32767, "peak " + peak);
	}

	@Test
	void callsignsAndTextAreCleanedUp() {
		assertEquals("W1BTR-9", TncPacket.sanitizeCallsign(" w1btr-9!"));
		assertEquals("ABCDEFGHIJKL", TncPacket.sanitizeCallsign("abcdefghijklmnop"));
		assertEquals("hi there", TncPacket.sanitizeText("  hi§ there \n", 100));
		assertEquals(TncPacket.BEACON_TEXT_MAX, TncPacket.sanitizeText("x".repeat(50), TncPacket.BEACON_TEXT_MAX).length());
	}
}

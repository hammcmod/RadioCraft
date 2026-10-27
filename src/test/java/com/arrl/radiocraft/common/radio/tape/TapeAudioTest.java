package com.arrl.radiocraft.common.radio.tape;

import com.arrl.radiocraft.common.items.TapeItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TapeAudioTest {

	/** A second of a 440 Hz tone, stereo at 44.1 kHz, comes out a second long at 48 kHz, mono, still at 440 Hz. */
	@Test
	void decodesVorbisToTheRadiosFormat() throws IOException {
		short[] audio;
		try(InputStream in = getClass().getResourceAsStream("/tape/tone_440.ogg")) {
			assertNotNull(in, "the test tape is missing");
			audio = OggDecoder.decode(in);
		}
		assertEquals(48000, audio.length, 480, "a second at 48 kHz");
		int crossings = 0;
		for(int i = 1; i < audio.length; i++)
			if(audio[i - 1] < 0 && audio[i] >= 0)
				crossings++;
		assertEquals(440, crossings, 10, "the tone's pitch");
		int peak = 0;
		for(short s : audio)
			peak = Math.max(peak, Math.abs(s));
		assertTrue(peak > 12000 && peak < 20000, "half scale, mixed down without doubling: " + peak);
	}

	@Test
	void muLawRoundTripIsClose() {
		short[] audio = new short[4800];
		for(int i = 0; i < audio.length; i++)
			audio[i] = (short)(12000 * Math.sin(2 * Math.PI * 300 * i / 48000.0));
		short[] back = TapeRecordings.expand(TapeRecordings.compress(audio));
		assertEquals(audio.length, back.length);
		double error = 0;
		for(int i = 0; i < audio.length; i++)
			error += Math.abs(audio[i] - back[i]);
		assertTrue(error / audio.length < 600, "telephone quality, not noise: " + error / audio.length);
	}

	@Test
	void labelsFromFileNames() {
		assertEquals("Station A", TapeItem.label("numbers/station_a.ogg"));
		assertEquals("Old Broadcast", TapeItem.label("old-broadcast.OGG"));
	}
}

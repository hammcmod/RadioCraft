package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadioVoiceProcessorTest {

	private static final RadioVoiceProcessor.Settings SSB = RadioVoiceProcessor.Settings.SSB;
	private static final int FRAME = 960;

	/** RMS (0 to 1) of {@code frames} 20 ms frames of a sine tone after the processor, ignoring the first half to settle. */
	private static double toneRms(double hz, double amplitude, RadioVoiceProcessor.Settings settings, double level) {
		RadioVoiceProcessor processor = new RadioVoiceProcessor();
		int frames = 50;
		double sum = 0.0D;
		int count = 0;
		for(int f = 0; f < frames; f++) {
			short[] pcm = new short[FRAME];
			for(int i = 0; i < FRAME; i++)
				pcm[i] = (short)Math.round(Math.sin(2.0D * Math.PI * hz * (f * FRAME + i) / RadioVoiceProcessor.SAMPLE_RATE) * amplitude * 32767.0D);
			processor.process(pcm, settings, level, 0.0D);
			if(f >= frames / 2)
				for(short s : pcm) {
					sum += (s / 32768.0D) * (s / 32768.0D);
					count++;
				}
		}
		return Math.sqrt(sum / count);
	}

	@Test
	void passesTheVoiceBandAndCutsBassAndTreble() {
		double voice = toneRms(1000, 0.3D, SSB, 1.0D);
		assertTrue(toneRms(80, 0.3D, SSB, 1.0D) < voice * 0.15D, "80 Hz should be cut");
		assertTrue(toneRms(8000, 0.3D, SSB, 1.0D) < voice * 0.15D, "8 kHz should be cut");
	}

	@Test
	void agcBringsQuietAndLoudTalkersTogether() {
		double quiet = toneRms(1000, 0.05D, SSB, 1.0D);
		double loud = toneRms(1000, 0.8D, SSB, 1.0D);
		assertTrue(loud / quiet < 2.5D, "16x louder input should come out under 2.5x louder, was " + loud / quiet);
	}

	@Test
	void levelScalesTheOutput() {
		double full = toneRms(1000, 0.3D, SSB, 1.0D);
		double half = toneRms(1000, 0.3D, SSB, 0.5D);
		assertEquals(0.5D, half / full, 0.01D);
		assertEquals(0.0D, toneRms(1000, 0.3D, SSB, 0.0D));
	}

	@Test
	void disabledOnlyAppliesTheLevel() {
		RadioVoiceProcessor.Settings off = new RadioVoiceProcessor.Settings(300, 2700, 1.6, false);
		short[] pcm = { 1000, -2000, 30000 };
		new RadioVoiceProcessor().process(pcm, off, 0.5D, 0.0D);
		assertArrayEquals(new short[] { 500, -1000, 15000 }, pcm);
	}

	@Test
	void neverClipsPastFullScale() {
		RadioVoiceProcessor processor = new RadioVoiceProcessor();
		short[] pcm = new short[FRAME];
		for(int i = 0; i < FRAME; i++)
			pcm[i] = (short)(i % 2 == 0 ? 32767 : -32768);
		processor.process(pcm, new RadioVoiceProcessor.Settings(300, 2700, 10.0, true), 1.0D, 1.0D);
		for(short s : pcm)
			assertTrue(s >= Short.MIN_VALUE && s <= Short.MAX_VALUE);
	}

	@Test
	void strongSignalDucksTheStatic() {
		assertEquals(1.0D, ReceiverMix.noiseDuck(0.0D, 0.3D), "no signal leaves the static alone");
		assertTrue(ReceiverMix.noiseDuck(0.97D, 0.2D) < 0.25D, "a strong signal over a quiet band ducks the static well down");
		assertTrue(ReceiverMix.noiseDuck(0.1D, 1.0D) > 0.95D, "a weak signal in a blackout barely touches it");
	}

	@Test
	void voiceLevelFollowsSignalToNoise() {
		assertEquals(0.0D, ReceiverMix.voiceLevel(0.0D, 0.3D));
		assertTrue(ReceiverMix.voiceLevel(0.97D, 0.2D) > 0.95D);
		assertTrue(ReceiverMix.voiceLevel(0.1D, 1.0D) < 0.12D);
		assertTrue(ReceiverMix.voiceLevel(0.5D, 0.3D) < ReceiverMix.voiceLevel(0.9D, 0.3D));
		double s = 0.6D, n = 0.4D; // AGC keeps signal plus noise power at full scale.
		assertEquals(1.0D, Math.pow(ReceiverMix.voiceLevel(s, n), 2) + Math.pow(ReceiverMix.noiseDuck(s, n), 2), 1e-9);
	}

	@Test
	void fadingStaysInItsRange() {
		double min = 1.0D, max = 0.0D;
		for(int t = 0; t < 2000; t++) {
			double f = ReceiverMix.fading(t * 0.05D, 12345L, 0.5D);
			min = Math.min(min, f);
			max = Math.max(max, f);
		}
		assertTrue(min >= 0.5D - 1e-9 && max <= 1.0D + 1e-9, "range " + min + ".." + max);
		assertTrue(max - min > 0.3D, "it should actually fade");
		assertEquals(1.0D, ReceiverMix.fading(3.0D, 7L, 0.0D), "depth 0 is steady");
	}
}

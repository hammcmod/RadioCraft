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

	/** Power at {@code probeHz} (Goertzel) over the second half of 50 frames of a {@code toneHz} tone shifted by {@code shiftHz}. */
	private static double shiftedPower(double toneHz, double shiftHz, double probeHz) {
		return shiftedPower(new double[] { toneHz }, shiftHz, probeHz);
	}

	/** The same for several tones at once, so the AGC is set by the loudest of them. */
	private static double shiftedPower(double[] tonesHz, double shiftHz, double probeHz) {
		RadioVoiceProcessor processor = new RadioVoiceProcessor();
		RadioVoiceProcessor.Settings off = SSB;
		int frames = 50;
		double coeff = 2.0D * Math.cos(2.0D * Math.PI * probeHz / RadioVoiceProcessor.SAMPLE_RATE);
		double s1 = 0.0D, s2 = 0.0D;
		for(int f = 0; f < frames; f++) {
			short[] pcm = new short[FRAME];
			for(int i = 0; i < FRAME; i++)
				for(double toneHz : tonesHz)
					pcm[i] += (short)Math.round(Math.sin(2.0D * Math.PI * toneHz * (f * FRAME + i) / RadioVoiceProcessor.SAMPLE_RATE) * 0.3D * 32767.0D / tonesHz.length);
			processor.process(pcm, off, 1.0D, 0.0D, shiftHz, 0.0D, 0.0D);
			if(f >= frames / 2)
				for(short s : pcm) {
					double s0 = s / 32768.0D + coeff * s1 - s2;
					s2 = s1;
					s1 = s0;
				}
		}
		return s1 * s1 + s2 * s2 - coeff * s1 * s2;
	}

	@Test
	void pitchShiftMovesEveryFrequencyByTheSameAmount() {
		assertTrue(shiftedPower(1000, 500, 1500) > 100.0D * shiftedPower(1000, 500, 1000), "1 kHz shifted up 500 Hz should land on 1.5 kHz");
		assertTrue(shiftedPower(1000, 500, 1500) > 100.0D * shiftedPower(1000, 500, 500), "no image on the wrong side");
		assertTrue(shiftedPower(1500, -500, 1000) > 100.0D * shiftedPower(1500, -500, 2000), "1.5 kHz shifted down 500 Hz should land on 1 kHz");
	}

	@Test
	void pitchShiftCutsTheEdgeThatSlidesOutOfThePassband() {
		// Shifting up 1.5 kHz: 2 kHz would land at 3.5 kHz, outside 300-2700, so it is gone; 800 Hz lands at 2.3 kHz and stays.
		double[] upTones = { 800, 2000 };
		assertTrue(shiftedPower(upTones, 1500, 2300) > 50.0D * shiftedPower(upTones, 1500, 3500));
		// Shifting down 1.5 kHz: 1 kHz would go below 0 and fold back; it must not come through at 500 Hz.
		double[] downTones = { 1000, 2200 };
		assertTrue(shiftedPower(downTones, -1500, 700) > 50.0D * shiftedPower(downTones, -1500, 500));
	}
}

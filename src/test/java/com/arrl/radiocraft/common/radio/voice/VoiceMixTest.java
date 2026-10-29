package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoiceMixTest {

	private static final int FRAME = 960;
	private static final long MS = 1_000_000L;
	private static final RadioVoiceProcessor.Settings CLEAN = new RadioVoiceProcessor.Settings(300, 2700, 1.6, false);
	private static final Object A = VoiceMix.key(UUID.nameUUIDFromBytes("a".getBytes()), null);
	private static final Object B = VoiceMix.key(UUID.nameUUIDFromBytes("b".getBytes()), null);

	private static short[] tone(double hz, double amplitude, int frame) {
		short[] pcm = new short[FRAME];
		for(int i = 0; i < FRAME; i++)
			pcm[i] = (short)Math.round(Math.sin(2.0D * Math.PI * hz * (frame * FRAME + i) / RadioVoiceProcessor.SAMPLE_RATE) * amplitude * 32767.0D);
		return pcm;
	}

	private static VoiceMix.Signal ssb(Object key, short[] pcm, double level) {
		return new VoiceMix.Signal(key, null, pcm, level, 0.0D, 0.0D, 0.0D, 0.0D, -1.0D);
	}

	private static VoiceMix.Signal fm(Object key, short[] pcm, double strength) {
		return new VoiceMix.Signal(key, null, pcm, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, strength);
	}

	/** The share of {@code pcm} that is the {@code hz} tone (Goertzel-style correlation), roughly its amplitude. */
	private static double amplitudeOf(short[] pcm, double hz, int frame) {
		double re = 0.0D, im = 0.0D;
		for(int i = 0; i < pcm.length; i++) {
			double phase = 2.0D * Math.PI * hz * (frame * FRAME + i) / RadioVoiceProcessor.SAMPLE_RATE;
			re += pcm[i] / 32768.0D * Math.sin(phase);
			im += pcm[i] / 32768.0D * Math.cos(phase);
		}
		return 2.0D * Math.hypot(re, im) / pcm.length;
	}

	// -------------------- ALIGNER --------------------

	@Test
	void aStationAloneGoesStraightThrough() {
		VoiceMix.Aligner aligner = new VoiceMix.Aligner();
		for(int f = 0; f < 5; f++) {
			List<VoiceMix.Signal> out = aligner.offer(ssb(A, tone(1000, 0.3, f), 1.0), f * 20 * MS);
			assertEquals(1, out.size(), "frame " + f);
			assertEquals(A, out.getFirst().key());
		}
	}

	@Test
	void aSecondStationWaitsForTheLeadAndIsMixedIn() {
		VoiceMix.Aligner aligner = new VoiceMix.Aligner();
		assertEquals(1, aligner.offer(ssb(A, tone(1000, 0.3, 0), 1.0), 0).size());
		assertTrue(aligner.offer(ssb(B, tone(600, 0.3, 0), 1.0), 5 * MS).isEmpty(), "B waits for A's next frame");
		List<VoiceMix.Signal> out = aligner.offer(ssb(A, tone(1000, 0.3, 1), 1.0), 20 * MS);
		assertEquals(List.of(A, B), out.stream().map(VoiceMix.Signal::key).toList());
	}

	@Test
	void theNextStationLeadsWhenTheLeadStops() {
		VoiceMix.Aligner aligner = new VoiceMix.Aligner();
		aligner.offer(ssb(A, tone(1000, 0.3, 0), 1.0), 0);
		assertTrue(aligner.offer(ssb(B, tone(600, 0.3, 0), 1.0), 10 * MS).isEmpty());
		// A has gone quiet for longer than the timeout: B leads, its waiting frame first, nothing lost.
		VoiceMix.Signal late = ssb(B, tone(600, 0.3, 5), 1.0);
		List<VoiceMix.Signal> out = aligner.offer(late, 100 * MS);
		assertEquals(1, out.size());
		assertNotSame(late, out.getFirst(), "B's waiting frame plays before its new one");
		assertSame(late, aligner.offer(ssb(B, tone(600, 0.3, 6), 1.0), 120 * MS).getFirst());
	}

	@Test
	void aWaitingStationNeverHoldsMoreThanAFewFrames() {
		VoiceMix.Aligner aligner = new VoiceMix.Aligner();
		aligner.offer(ssb(A, tone(1000, 0.3, 0), 1.0), 0);
		for(int f = 0; f < 10; f++)
			aligner.offer(ssb(B, tone(600, 0.3, f), 1.0), (1 + f) * MS);
		int played = 0;
		for(int f = 1; f < 10; f++)
			played += aligner.offer(ssb(A, tone(1000, 0.3, f), 1.0), f * 20 * MS).size() - 1;
		assertEquals(VoiceMix.Aligner.MAX_WAITING, played);
	}

	// -------------------- RENDERER --------------------

	@Test
	void ssbAddsBothVoicesAtTheirOwnLevels() {
		VoiceMix.Renderer renderer = new VoiceMix.Renderer();
		short[] out = null;
		for(int f = 0; f < 10; f++)
			out = renderer.render(List.of(ssb(A, tone(1000, 0.3, f), 1.0), ssb(B, tone(600, 0.3, f), 0.5)), CLEAN, 1.0, f * 20 * MS).pcm();
		assertEquals(0.3, amplitudeOf(out, 1000, 9), 0.02);
		assertEquals(0.15, amplitudeOf(out, 600, 9), 0.02);
	}

	@Test
	void oneStationRendersAsItAlwaysHas() {
		short[] mixed = new VoiceMix.Renderer().render(List.of(ssb(A, tone(1000, 0.3, 0), 0.5)), CLEAN, 1.0, 0).pcm();
		short[] direct = new RadioVoiceProcessor().process(tone(1000, 0.3, 0), CLEAN, 0.5, 0.0);
		assertArrayEquals(direct, mixed);
	}

	@Test
	void renderingLeavesTheInputAlone() {
		short[] pcm = tone(1000, 0.3, 0);
		short[] copy = pcm.clone();
		new VoiceMix.Renderer().render(List.of(ssb(A, pcm, 0.5), ssb(B, tone(600, 0.3, 0), 0.5)), CLEAN, 1.0, 0);
		assertArrayEquals(copy, pcm, "every speaker renders the same frame");
	}

	@Test
	void fmCapturesTheStrongerSignal() {
		VoiceMix.Renderer renderer = new VoiceMix.Renderer();
		VoiceMix.Mixed mixed = null;
		for(int f = 0; f < 10; f++) // B at a fifth of A: well under the capture ratio.
			mixed = renderer.render(List.of(fm(B, tone(600, 0.3, f), 0.1), fm(A, tone(1000, 0.3, f), 0.5)), CLEAN, 1.0, f * 20 * MS);
		assertEquals(A, mixed.loudest().key());
		double wanted = amplitudeOf(mixed.pcm(), 1000, 9), other = amplitudeOf(mixed.pcm(), 600, 9);
		assertTrue(wanted > 0.25, "the strong signal plays at full volume: " + wanted);
		assertTrue(other < wanted * 0.05, "the weak one is captured out: " + other + " against " + wanted);
	}

	@Test
	void fmCloseToTheCaptureRatioIsRoughAndBleedsThrough() {
		VoiceMix.Renderer renderer = new VoiceMix.Renderer();
		VoiceMix.Mixed mixed = null;
		for(int f = 0; f < 10; f++) // B just under half of A: captured, barely.
			mixed = renderer.render(List.of(fm(A, tone(1000, 0.3, f), 0.5), fm(B, tone(600, 0.3, f), 0.24)), CLEAN, 1.0, f * 20 * MS);
		assertEquals(A, mixed.loudest().key());
		assertTrue(amplitudeOf(mixed.pcm(), 600, 9) > 0.03, "B bleeds through: " + amplitudeOf(mixed.pcm(), 600, 9));
	}

	@Test
	void fmSignalsTooCloseToCaptureTakeTurns() {
		VoiceMix.Renderer renderer = new VoiceMix.Renderer();
		boolean heardA = false, heardB = false;
		for(int f = 0; f < 200; f++) {
			VoiceMix.Mixed mixed = renderer.render(List.of(fm(A, tone(1000, 0.3, f), 0.5), fm(B, tone(600, 0.3, f), 0.45)), CLEAN, 1.0, f * 20 * MS);
			heardA |= mixed.loudest().key().equals(A);
			heardB |= mixed.loudest().key().equals(B);
		}
		assertTrue(heardA && heardB, "neither captures: the receiver flips between them");
	}

	@Test
	void twoVoicesAtFullLevelDoNotHardClip() {
		double[] sum = { 1.3, -1.3, 0.5 };
		short[] out = VoiceMix.limit(sum);
		assertTrue(out[0] < Short.MAX_VALUE && out[0] > 0.9 * Short.MAX_VALUE);
		assertEquals(-out[0], out[1]);
		assertEquals(Math.round(0.5 * 32767), out[2], "below the knee it is left alone");
	}

	// -------------------- SMOOTHING --------------------

	@Test
	void theLevelGlidesToANewValueInsteadOfStepping() {
		RadioVoiceProcessor.Settings smooth = new RadioVoiceProcessor.Settings(300, 2700, 1.6, false, 0.2);
		RadioVoiceProcessor processor = new RadioVoiceProcessor();
		short[] ones = new short[FRAME];
		java.util.Arrays.fill(ones, (short)10000);
		processor.process(ones.clone(), smooth, 1.0, 0.0); // Settles at full level.
		short[] stepped = processor.process(ones.clone(), smooth, 0.0, 0.0); // The signal drops away.
		assertTrue(stepped[0] > 9900, "the first sample still at the old level: " + stepped[0]);
		assertTrue(stepped[FRAME - 1] > 0 && stepped[FRAME - 1] < 10000, "partway down after 20 ms: " + stepped[FRAME - 1]);
		short[] unsmoothed = new RadioVoiceProcessor().process(ones.clone(), CLEAN, 0.0, 0.0);
		assertEquals(0, unsmoothed[0], "0 seconds follows at once");
	}

	@Test
	void theFirstFrameStartsAtItsOwnLevel() {
		RadioVoiceProcessor.Settings smooth = new RadioVoiceProcessor.Settings(300, 2700, 1.6, false, 0.2);
		short[] ones = new short[FRAME];
		java.util.Arrays.fill(ones, (short)10000);
		short[] first = new RadioVoiceProcessor().process(ones, smooth, 0.5, 0.0);
		assertEquals(5000, first[0], "no fade in from silence when a signal opens the squelch");
	}

	@Test
	void fmHissGlidesInAsTheSignalWeakens() {
		RadioVoiceProcessor processor = new RadioVoiceProcessor();
		processor.fmFade(new short[FRAME], 0.5, 1.0, 0.15); // Full quieting: no hiss.
		double first = rms(processor.fmFade(new short[FRAME], 0.02, 1.0, 0.15), 0, FRAME); // Into the static zone.
		double later = 0.0D;
		for(int f = 0; f < 25; f++) // Half a second on.
			later = rms(processor.fmFade(new short[FRAME], 0.02, 1.0, 0.15), 0, FRAME);
		assertTrue(first < later * 0.25, "the static rises over a few frames: " + first + " then " + later);
		double stepped = rms(new RadioVoiceProcessor().fmFade(new short[FRAME], 0.02, 1.0, 0.0), 0, FRAME);
		assertTrue(stepped > later * 0.5, "without smoothing it is there at once: " + stepped);
	}

	private static double rms(short[] pcm, int from, int to) {
		double sum = 0.0D;
		for(int i = from; i < to; i++)
			sum += (pcm[i] / 32768.0D) * (pcm[i] / 32768.0D);
		return Math.sqrt(sum / (to - from));
	}
}

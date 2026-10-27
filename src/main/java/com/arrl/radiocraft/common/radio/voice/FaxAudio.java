package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.common.radio.data.TncPacket;

import java.util.List;

/**
 * What a radio fax sounds like on the air, as a modem of the 1980s did it: the caller's CNG beep (1100 Hz, half a
 * second), the answering CED tone (2100 Hz, a second), then the picture as a V.29-style carrier at 1700 Hz jumping
 * phase 2400 times a second, and a short beep for each packet acknowledged. The fax machine itself monitors the line
 * through a small speaker: {@link #monitorFrame} gives that, quietly.
 */
public final class FaxAudio {

	private FaxAudio() {}

	public static final double CNG_HZ = 1100.0D;
	public static final double CED_HZ = 2100.0D;
	public static final double ACK_HZ = 1650.0D;
	public static final double CARRIER_HZ = 1700.0D;
	/** Phase changes a second on the picture carrier. */
	private static final double SYMBOLS_PER_SECOND = 2400.0D;
	private static final double LEVEL = 0.35D;
	private static final double MONITOR_LEVEL = 0.1D;
	/** The steady carrier before the picture, so a receiver can lock on. */
	private static final double TRAINING_SECONDS = 0.1D;
	/**
	 * The shortest burst, 10 frames (0.2 s): a radio that has just sent is deaf for two ticks, and the answer can start
	 * going out in the tick the packet landed, so its tones must run into the fourth tick to be heard.
	 */
	public static final int MIN_FRAMES = 10;
	private static final double SAMPLE_RATE = RadioVoiceProcessor.SAMPLE_RATE;

	/** The tones a fax packet goes out with. */
	public static List<short[]> frames(TncPacket packet) {
		return switch(packet.kind()) {
			case FAX_CALL -> calling();
			case FAX_ANSWER -> answer();
			case FAX_PACKET -> data(packet.faxTicks(), packet.id());
			case FAX_ACK -> ack();
			default -> List.of();
		};
	}

	/** The caller: a CNG beep then a pause. */
	public static List<short[]> calling() {
		double[] samples = new double[(int)(0.8D * SAMPLE_RATE)];
		tone(samples, 0, (int)(0.5D * SAMPLE_RATE), CNG_HZ, LEVEL);
		return RepeaterAudio.frames(samples);
	}

	/** The answering machine: a second of CED. */
	public static List<short[]> answer() {
		double[] samples = new double[(int)(1.0D * SAMPLE_RATE)];
		tone(samples, 0, samples.length, CED_HZ, LEVEL);
		return RepeaterAudio.frames(samples);
	}

	/** A packet acknowledged: a short beep. */
	public static List<short[]> ack() {
		double[] samples = new double[MIN_FRAMES * RepeaterAudio.FRAME];
		tone(samples, 0, samples.length, ACK_HZ, LEVEL);
		return RepeaterAudio.frames(samples);
	}

	/** One packet of the picture: {@code ticks} of carrier, a short training burst then the phase-hopping data. */
	public static List<short[]> data(int ticks, long seed) {
		double[] samples = new double[Math.max(MIN_FRAMES * RepeaterAudio.FRAME, (int)(Math.max(1, ticks) * 0.05D * SAMPLE_RATE))];
		carrier(samples, 0, seed, LEVEL);
		return RepeaterAudio.frames(samples);
	}

	/**
	 * Frame {@code index} of the machine's own monitor speaker: the picture carrier, quietly, continuous from one
	 * frame to the next. While calling, the CNG beep pattern instead.
	 */
	public static short[] monitorFrame(long index, boolean calling) {
		double[] samples = new double[RepeaterAudio.FRAME];
		if(calling) {
			long sampleAt = index * RepeaterAudio.FRAME;
			int period = (int)(3.0D * SAMPLE_RATE), on = (int)(0.5D * SAMPLE_RATE);
			for(int i = 0; i < samples.length; i++)
				if((sampleAt + i) % period < on)
					samples[i] = Math.sin(2.0D * Math.PI * CNG_HZ * (sampleAt + i) / SAMPLE_RATE) * MONITOR_LEVEL;
		}
		else
			carrier(samples, index * RepeaterAudio.FRAME, 0x5A17L, MONITOR_LEVEL);
		short[] frame = new short[RepeaterAudio.FRAME];
		for(int i = 0; i < frame.length; i++)
			frame[i] = (short)(Math.max(-1.0D, Math.min(1.0D, samples[i])) * Short.MAX_VALUE);
		return frame;
	}

	/**
	 * The picture carrier from sample {@code offset} of a stream: a steady tone for the training time, then the
	 * phase jumping each symbol by a multiple of 45 degrees chosen from {@code seed} and the symbol number, so the
	 * stream is the same wherever it is cut.
	 */
	private static void carrier(double[] samples, long offset, long seed, double level) {
		double samplesPerSymbol = SAMPLE_RATE / SYMBOLS_PER_SECOND;
		long trainingSamples = (long)(TRAINING_SECONDS * SAMPLE_RATE);
		for(int i = 0; i < samples.length; i++) {
			long at = offset + i;
			double phase = 0.0D;
			if(at >= trainingSamples) {
				long symbol = (long)((at - trainingSamples) / samplesPerSymbol);
				phase = (hash(seed, symbol) & 7) * Math.PI / 4.0D;
			}
			double edge = at < trainingSamples ? Math.min(1.0D, at / (0.01D * SAMPLE_RATE)) : 1.0D; // No click at the start.
			samples[i] = Math.sin(2.0D * Math.PI * CARRIER_HZ * at / SAMPLE_RATE + phase) * level * edge;
		}
	}

	private static long hash(long seed, long value) {
		long x = (seed ^ (value * 0x9E3779B97F4A7C15L)) | 1L;
		x ^= x << 13;
		x ^= x >>> 7;
		x ^= x << 17;
		return x;
	}

	/** A tone of {@code hz} over {@code length} samples from {@code start}, with 10 ms soft edges. */
	private static void tone(double[] samples, int start, int length, double hz, double level) {
		int ramp = (int)(0.01D * SAMPLE_RATE);
		for(int i = 0; i < length && start + i < samples.length; i++) {
			double edge = Math.min(1.0D, Math.min(i, length - 1 - i) / (double)ramp);
			samples[start + i] = Math.sin(2.0D * Math.PI * hz * i / SAMPLE_RATE) * level * edge;
		}
	}
}

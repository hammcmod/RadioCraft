package com.arrl.radiocraft.common.radio.voice;

import java.util.List;

/**
 * The sound of a picture going over slow-scan TV, simplified: the calibration header (1900 Hz, a 1200 Hz break,
 * 1900 Hz) that every SSTV transmission starts with, then a line at a time, each a 1200 Hz sync pulse, a 1500 Hz
 * porch and the pixels as tones from 1500 Hz (black) to 2300 Hz (white). A map's 128 lines take about 26 seconds.
 */
public final class Sstv {

	private Sstv() {}

	public static final double BLACK_HZ = 1500.0D;
	public static final double WHITE_HZ = 2300.0D;
	public static final double SYNC_HZ = 1200.0D;
	public static final double LEADER_HZ = 1900.0D;
	private static final double LEADER_SECONDS = 0.3D;
	private static final double BREAK_SECONDS = 0.01D;
	private static final double SYNC_SECONDS = 0.009D;
	private static final double PORCH_SECONDS = 0.003D;
	/** Time per pixel. */
	public static final double PIXEL_SECONDS = 0.0015D;
	private static final double LEVEL = 0.35D;

	/**
	 * Frames for a picture given as brightness rows (0 black to 1 white, every row the same width).
	 */
	public static List<short[]> frames(double[][] rows) {
		double rate = RadioVoiceProcessor.SAMPLE_RATE;
		int width = rows.length == 0 ? 0 : rows[0].length;
		double seconds = header() + rows.length * (SYNC_SECONDS + PORCH_SECONDS + width * PIXEL_SECONDS);
		double[] samples = new double[(int)Math.ceil(seconds * rate)];
		Writer out = new Writer(samples);
		out.tone(LEADER_HZ, LEADER_SECONDS);
		out.tone(SYNC_HZ, BREAK_SECONDS);
		out.tone(LEADER_HZ, LEADER_SECONDS);
		for(double[] row : rows) {
			out.tone(SYNC_HZ, SYNC_SECONDS);
			out.tone(BLACK_HZ, PORCH_SECONDS);
			for(double brightness : row)
				out.tone(BLACK_HZ + (WHITE_HZ - BLACK_HZ) * Math.max(0.0D, Math.min(1.0D, brightness)), PIXEL_SECONDS);
		}
		return RepeaterAudio.frames(samples);
	}

	/** How many 20 ms frames a picture of {@code lines} lines {@code width} pixels wide takes. */
	public static int frameCount(int lines, int width) {
		double seconds = header() + lines * (SYNC_SECONDS + PORCH_SECONDS + width * PIXEL_SECONDS);
		int samples = (int)Math.ceil(seconds * RadioVoiceProcessor.SAMPLE_RATE);
		return (samples + RepeaterAudio.FRAME - 1) / RepeaterAudio.FRAME;
	}

	private static double header() {
		return LEADER_SECONDS * 2 + BREAK_SECONDS;
	}

	/** Writes tones one after another with the phase carried across, so frequency changes do not click. */
	private static final class Writer {
		private final double[] samples;
		private double phase = 0.0D;
		private double time = 0.0D; // In samples, kept fractional so short pixels do not drift.
		private int written = 0;

		Writer(double[] samples) {
			this.samples = samples;
		}

		void tone(double hz, double seconds) {
			time += seconds * RadioVoiceProcessor.SAMPLE_RATE;
			int end = Math.min(samples.length, (int)Math.round(time));
			double step = 2.0D * Math.PI * hz / RadioVoiceProcessor.SAMPLE_RATE;
			for(; written < end; written++) {
				samples[written] = Math.sin(phase) * LEVEL;
				phase += step;
			}
		}
	}
}

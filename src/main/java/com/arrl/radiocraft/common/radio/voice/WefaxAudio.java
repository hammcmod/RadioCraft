package com.arrl.radiocraft.common.radio.voice;

import net.minecraft.world.level.material.MapColor;

import java.util.List;

/**
 * What an HF weather fax sounds like (maintainer, 2026-10-31: "an SSTV kind of sound"): the picture sent as a tone that
 * slides between 1500 Hz for black and 2300 Hz for white, one scan line after another. A transmission opens with the
 * start tone (black and white alternating 300 times a second), then the phasing lines (white with a short black pulse,
 * which the receiver lines its drum up on), then the picture, and closes with the stop tone (450 times a second).
 * The receiving machine prints each line as it arrives: {@link #imageRows} says how far that is.
 */
public final class WefaxAudio {

	private WefaxAudio() {}

	public static final double BLACK_HZ = 1500.0D;
	public static final double WHITE_HZ = 2300.0D;
	public static final double START_RATE_HZ = 300.0D;
	public static final double STOP_RATE_HZ = 450.0D;
	// Shares of the transmission: start tone, phasing, picture, stop tone.
	public static final double START_SHARE = 0.08D, PHASING_SHARE = 0.07D, IMAGE_SHARE = 0.80D;
	/** The black pulse at the start of each phasing line, as a share of the line. */
	private static final double PHASING_PULSE = 0.05D;
	private static final double LEVEL = 0.35D;
	private static final double SAMPLE_RATE = RadioVoiceProcessor.SAMPLE_RATE;

	/**
	 * The whole transmission of a {@code size} x {@code size} picture lasting {@code ticks}, in 20 ms frames.
	 *
	 * @param brightness Each pixel's brightness, 0 black to 1 white, row by row.
	 */
	public static List<short[]> frames(double[] brightness, int size, int ticks) {
		int total = Math.max(1, ticks) * RepeaterAudio.FRAME * 50 / 20;
		double[] samples = new double[total];
		int startEnd = (int)(total * START_SHARE);
		int phasingEnd = startEnd + (int)(total * PHASING_SHARE);
		int imageEnd = phasingEnd + (int)(total * IMAGE_SHARE);
		double lineSamples = (imageEnd - phasingEnd) / (double)size;
		double phase = 0.0D;
		for(int i = 0; i < total; i++) {
			double white; // 0 black to 1 white: the tone between BLACK_HZ and WHITE_HZ.
			if(i < startEnd)
				white = square(i, START_RATE_HZ);
			else if(i < phasingEnd)
				white = ((i - startEnd) % lineSamples) / lineSamples < PHASING_PULSE ? 0.0D : 1.0D;
			else if(i < imageEnd) {
				int row = Math.min(size - 1, (int)((i - phasingEnd) / lineSamples));
				int column = Math.min(size - 1, (int)(((i - phasingEnd) - row * lineSamples) / lineSamples * size));
				white = brightness[row * size + column];
			}
			else
				white = square(i, STOP_RATE_HZ);
			phase += 2.0D * Math.PI * (BLACK_HZ + (WHITE_HZ - BLACK_HZ) * white) / SAMPLE_RATE; // Continuous phase: no clicks.
			double edge = Math.min(1.0D, Math.min(i, total - 1 - i) / (0.01D * SAMPLE_RATE));
			samples[i] = Math.sin(phase) * LEVEL * edge;
		}
		return RepeaterAudio.frames(samples);
	}

	/** {@link #frames(double[], int, int)} for a picture of map colours (a WEFAX sheet). */
	public static List<short[]> frames(byte[] pixels, int size, int ticks) {
		return frames(brightness(pixels), size, ticks);
	}

	/** Each map colour's brightness, 0 to 1. */
	public static double[] brightness(byte[] pixels) {
		double[] out = new double[pixels.length];
		for(int i = 0; i < pixels.length; i++) {
			int abgr = MapColor.getColorFromPackedId(pixels[i] & 0xFF);
			out[i] = (((abgr) & 0xFF) + ((abgr >> 8) & 0xFF) + ((abgr >> 16) & 0xFF)) / (3.0D * 255.0D);
		}
		return out;
	}

	/**
	 * How many of the picture's {@code size} lines have come in {@code elapsed} ticks into a transmission lasting
	 * {@code ticks}: none during the start tone and phasing, all of them once the stop tone begins.
	 */
	public static int imageRows(long elapsed, int ticks, int size) {
		double at = elapsed / (double)Math.max(1, ticks) - START_SHARE - PHASING_SHARE;
		return (int)Math.max(0, Math.min(size, Math.floor(at / IMAGE_SHARE * size)));
	}

	private static double square(int sample, double rate) {
		return ((long)(sample * 2.0D * rate / SAMPLE_RATE) & 1L) == 0L ? 0.0D : 1.0D;
	}
}

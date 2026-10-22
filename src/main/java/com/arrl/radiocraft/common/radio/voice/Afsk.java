package com.arrl.radiocraft.common.radio.voice;

import java.util.List;

/**
 * The sound of a packet: Bell 202 AFSK, 1200 baud with mark at 1200 Hz and space at 2200 Hz, as APRS sends it. Bits are
 * NRZI (a 0 changes tone, a 1 keeps it), led in by a run of HDLC flags. The bits are made up from the packet's length
 * and id rather than its real AX.25 encoding, which nobody can hear the difference of.
 */
public final class Afsk {

	private Afsk() {}

	public static final double BAUD = 1200.0D;
	public static final double MARK_HZ = 1200.0D;
	public static final double SPACE_HZ = 2200.0D;
	/** Flags before the packet while the far receiver's squelch opens (TXDELAY), about 200 ms. */
	public static final int PREAMBLE_FLAGS = 30;
	public static final int TAIL_FLAGS = 3;
	private static final double LEVEL = 0.35D;
	private static final int FLAG = 0x7E;

	/** Frames of the burst for a packet of {@code bytes} bytes, its bits seeded from {@code seed}. */
	public static List<short[]> burst(int bytes, long seed) {
		int totalBytes = PREAMBLE_FLAGS + Math.max(0, bytes) + TAIL_FLAGS;
		double samplesPerBit = RadioVoiceProcessor.SAMPLE_RATE / BAUD;
		double[] samples = new double[(int)Math.ceil(totalBytes * 8 * samplesPerBit)];
		long x = seed | 1L;
		boolean mark = true;
		double phase = 0.0D;
		int sample = 0;
		for(int b = 0; b < totalBytes; b++) {
			int value;
			if(b < PREAMBLE_FLAGS || b >= totalBytes - TAIL_FLAGS)
				value = FLAG;
			else {
				x ^= x << 13;
				x ^= x >>> 7;
				x ^= x << 17;
				value = (int)(x & 0xFF);
			}
			for(int bit = 0; bit < 8; bit++) { // Least significant bit first, as AX.25 sends it.
				if(((value >> bit) & 1) == 0)
					mark = !mark;
				double step = 2.0D * Math.PI * (mark ? MARK_HZ : SPACE_HZ) / RadioVoiceProcessor.SAMPLE_RATE;
				int end = (int)Math.round((b * 8 + bit + 1) * samplesPerBit);
				for(; sample < end && sample < samples.length; sample++) { // Phase carries on across bits: no clicks.
					samples[sample] = Math.sin(phase) * LEVEL;
					phase += step;
				}
			}
		}
		return RepeaterAudio.frames(samples);
	}

	/** How long the burst for a packet of {@code bytes} bytes lasts, in 20 ms frames. */
	public static int frameCount(int bytes) {
		int totalBits = (PREAMBLE_FLAGS + Math.max(0, bytes) + TAIL_FLAGS) * 8;
		int samples = (int)Math.ceil(totalBits * RadioVoiceProcessor.SAMPLE_RATE / BAUD);
		return (samples + RepeaterAudio.FRAME - 1) / RepeaterAudio.FRAME;
	}
}

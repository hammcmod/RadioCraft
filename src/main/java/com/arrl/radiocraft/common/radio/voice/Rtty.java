package com.arrl.radiocraft.common.radio.voice;

import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Radioteletype as amateurs send it: 45.45 baud ITA2 (Baudot) with mark at 2125 Hz and space 170 Hz above. Each
 * character is a start bit (space), five data bits and one and a half stop bits (mark), about 6 characters a second.
 * Like a teleprinter it only knows capitals, figures and a little punctuation. The data bits come from the character
 * code rather than the real ITA2 table, which sounds the same.
 */
public final class Rtty {

	private Rtty() {}

	public static final double BAUD = 45.45D;
	public static final double MARK_HZ = 2125.0D;
	public static final double SPACE_HZ = 2295.0D;
	/** Bits per character: start, five data, one and a half stop. */
	public static final double BITS_PER_CHAR = 7.5D;
	/** Steady mark before the first character after a pause, so the far receiver can lock on. */
	public static final double LEAD_IN_SECONDS = 0.3D;
	private static final double LEVEL = 0.2D; // Quiet under voice: a listener hears it, it does not drown the band.

	/** What a teleprinter can print, besides letters, figures and the space; a newline ends the line. */
	private static final String PUNCTUATION = "-?:$!&#'().,;/\"+=";

	/** True if a teleprinter has the character (after upper-casing). */
	public static boolean isPrintable(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '\n' || PUNCTUATION.indexOf(c) >= 0;
	}

	/** {@code text} upper-cased with anything a teleprinter cannot print dropped, at most {@code max} characters. */
	public static String sanitize(String text, int max) {
		StringBuilder out = new StringBuilder();
		for(char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
			if(c == '\r')
				c = '\n';
			if(isPrintable(c))
				out.append(c);
			if(out.length() >= max)
				break;
		}
		return out.toString();
	}

	/**
	 * What a receiver copying a weak signal prints: each character becomes a random one with chance {@code errorRate}
	 * (newlines survive, so lines still break where they should).
	 */
	public static String garble(String text, double errorRate, RandomGenerator random) {
		if(errorRate <= 0.0D)
			return text;
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 " + PUNCTUATION;
		StringBuilder out = new StringBuilder();
		for(char c : text.toCharArray())
			out.append(c != '\n' && random.nextDouble() < errorRate ? alphabet.charAt(random.nextInt(alphabet.length())) : c);
		return out.toString();
	}

	/**
	 * The tones for {@code text}, as 20 ms frames, padded out with mark (the idle tone) to the end of the last frame.
	 *
	 * @param leadIn True to start with {@link #LEAD_IN_SECONDS} of mark.
	 * @param phase  One element: the tone's phase where the last call left off, updated, so consecutive calls join
	 *               without a click.
	 */
	public static List<short[]> frames(String text, boolean leadIn, double[] phase) {
		double samplesPerBit = RadioVoiceProcessor.SAMPLE_RATE / BAUD;
		int lead = leadIn ? (int)(LEAD_IN_SECONDS * RadioVoiceProcessor.SAMPLE_RATE) : 0;
		int body = (int)Math.ceil(text.length() * BITS_PER_CHAR * samplesPerBit);
		int total = (lead + body + RepeaterAudio.FRAME - 1) / RepeaterAudio.FRAME * RepeaterAudio.FRAME;
		double[] samples = new double[Math.max(total, RepeaterAudio.FRAME)];
		for(int i = 0; i < samples.length; i++) {
			boolean mark = true;
			int inBody = i - lead;
			if(inBody >= 0 && inBody < body) {
				double bit = (inBody / samplesPerBit) % BITS_PER_CHAR;
				int c = text.charAt(Math.min(text.length() - 1, (int)(inBody / samplesPerBit / BITS_PER_CHAR)));
				if(bit < 1.0D)
					mark = false; // Start bit.
				else if(bit < 6.0D)
					mark = ((c >> ((int)bit - 1)) & 1) == 1;
			}
			samples[i] = Math.sin(phase[0]) * LEVEL;
			phase[0] += 2.0D * Math.PI * (mark ? MARK_HZ : SPACE_HZ) / RadioVoiceProcessor.SAMPLE_RATE;
		}
		phase[0] %= 2.0D * Math.PI;
		return RepeaterAudio.frames(samples);
	}

	/** Seconds {@code chars} characters take on the air. */
	public static double seconds(int chars) {
		return chars * BITS_PER_CHAR / BAUD;
	}
}

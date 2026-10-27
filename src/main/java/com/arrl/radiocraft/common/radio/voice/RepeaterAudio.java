package com.arrl.radiocraft.common.radio.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Audio a repeater makes up itself, as 20 ms frames of 48 kHz PCM like Simple Voice Chat's: its callsign in Morse
 * as a tone (MCW, which every FM radio can hear), the courtesy beep when a user lets go, silence to hold the
 * transmitter up through the hang time, and the bursts of noise of a repeater keying up into its own receiver.
 */
public final class RepeaterAudio {

	private RepeaterAudio() {}

	public static final int FRAME = 960;
	private static final double SAMPLE_RATE = RadioVoiceProcessor.SAMPLE_RATE;
	/** Morse speed of the ID: 20 words per minute, a 60 ms dot. */
	public static final int ID_WPM = 20;
	private static final double ID_TONE_HZ = 800.0D;
	private static final double ID_LEVEL = 0.35D;
	private static final double BEEP_HZ = 1000.0D;
	private static final double BEEP_LEVEL = 0.3D;
	private static final double RAMP_SECONDS = 0.005D; // Tone edges, so they do not click.

	static final Map<Character, String> MORSE = Map.ofEntries(
			Map.entry('A', ".-"), Map.entry('B', "-..."), Map.entry('C', "-.-."), Map.entry('D', "-.."), Map.entry('E', "."),
			Map.entry('F', "..-."), Map.entry('G', "--."), Map.entry('H', "...."), Map.entry('I', ".."), Map.entry('J', ".---"),
			Map.entry('K', "-.-"), Map.entry('L', ".-.."), Map.entry('M', "--"), Map.entry('N', "-."), Map.entry('O', "---"),
			Map.entry('P', ".--."), Map.entry('Q', "--.-"), Map.entry('R', ".-."), Map.entry('S', "..."), Map.entry('T', "-"),
			Map.entry('U', "..-"), Map.entry('V', "...-"), Map.entry('W', ".--"), Map.entry('X', "-..-"), Map.entry('Y', "-.--"),
			Map.entry('Z', "--.."), Map.entry('0', "-----"), Map.entry('1', ".----"), Map.entry('2', "..---"), Map.entry('3', "...--"),
			Map.entry('4', "....-"), Map.entry('5', "....."), Map.entry('6', "-...."), Map.entry('7', "--..."), Map.entry('8', "---.."),
			Map.entry('9', "----."), Map.entry('/', "-..-.")
	);

	/** Upper case, only letters, digits and '/', at most 10 characters: what the repeater can send. */
	public static String sanitizeCallsign(String callsign) {
		StringBuilder out = new StringBuilder();
		for(char c : callsign.toUpperCase(Locale.ROOT).toCharArray()) {
			if(MORSE.containsKey(c))
				out.append(c);
			if(out.length() >= 10)
				break;
		}
		return out.toString();
	}

	/**
	 * Key-down pattern of {@code text} in Morse, one entry per dot length: true while the tone is on. A dash is three
	 * dots, the gap inside a letter one, between letters three, and between words seven.
	 */
	public static List<Boolean> morseUnits(String text) {
		List<Boolean> units = new ArrayList<>();
		boolean firstLetter = true;
		for(char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
			if(c == ' ') {
				for(int i = 0; i < 4; i++) // With the 3 after the last letter, 7.
					units.add(false);
				continue;
			}
			String code = MORSE.get(c);
			if(code == null)
				continue;
			if(!firstLetter)
				for(int i = 0; i < 3; i++)
					units.add(false);
			firstLetter = false;
			for(int i = 0; i < code.length(); i++) {
				if(i > 0)
					units.add(false);
				int length = code.charAt(i) == '-' ? 3 : 1;
				for(int j = 0; j < length; j++)
					units.add(true);
			}
		}
		return units;
	}

	/** {@code text} in Morse as tone frames, with a short pause before it. Empty for empty text. */
	public static List<short[]> morse(String text) {
		return morse(text, ID_WPM, ID_TONE_HZ);
	}

	/** The courtesy beep: a short tone after a user lets go, so the next one knows they can talk. */
	public static List<short[]> courtesyBeep() {
		int gap = (int)(0.15D * SAMPLE_RATE);
		double[] samples = new double[gap + (int)(0.12D * SAMPLE_RATE)];
		tone(samples, gap, samples.length - gap, BEEP_HZ, BEEP_LEVEL);
		return frames(samples);
	}

	/** A steady tone of {@code hz} for {@code seconds} at {@code level} (0 to 1), with soft edges. */
	public static List<short[]> tone(double hz, double seconds, double level) {
		double[] samples = new double[(int)(seconds * SAMPLE_RATE)];
		tone(samples, 0, samples.length, hz, level);
		return frames(samples);
	}

	/** {@code text} in Morse as tone frames at {@code wpm} and {@code hz}, with a short pause before it. */
	public static List<short[]> morse(String text, int wpm, double hz) {
		List<Boolean> units = morseUnits(text);
		if(units.isEmpty())
			return List.of();
		int unitSamples = (int)Math.round(1.2D / wpm * SAMPLE_RATE);
		int lead = (int)(0.2D * SAMPLE_RATE);
		double[] samples = new double[lead + units.size() * unitSamples];
		int start = -1;
		for(int u = 0; u <= units.size(); u++) {
			boolean on = u < units.size() && units.get(u);
			if(on && start < 0)
				start = u;
			else if(!on && start >= 0) {
				tone(samples, lead + start * unitSamples, (u - start) * unitSamples, hz, ID_LEVEL);
				start = -1;
			}
		}
		return frames(samples);
	}

	/** One frame of silence: the transmitter is up but nobody is talking. */
	public static short[] silence() {
		return new short[FRAME];
	}

	/** One frame of white noise at {@code level} (0 to 1), from {@code seed} so tests can repeat it. */
	public static short[] noise(double level, long seed) {
		short[] frame = new short[FRAME];
		long x = seed | 1L;
		for(int i = 0; i < FRAME; i++) {
			x ^= x << 13;
			x ^= x >>> 7;
			x ^= x << 17;
			double white = (x >>> 11) * 0x1.0p-53 * 2.0D - 1.0D;
			frame[i] = (short)Math.round(white * level * 32767.0D);
		}
		return frame;
	}

	private static void tone(double[] samples, int from, int length, double hz, double level) {
		int ramp = (int)(RAMP_SECONDS * SAMPLE_RATE);
		for(int i = 0; i < length && from + i < samples.length; i++) {
			double envelope = Math.min(1.0D, Math.min(i, length - 1 - i) / (double)ramp);
			samples[from + i] = Math.sin(2.0D * Math.PI * hz * i / SAMPLE_RATE) * level * envelope;
		}
	}

	/** Cuts {@code samples} (-1 to 1) into 20 ms frames, padding the last one with silence. */
	static List<short[]> frames(double[] samples) {
		List<short[]> frames = new ArrayList<>();
		for(int f = 0; f * FRAME < samples.length; f++) {
			short[] frame = new short[FRAME];
			for(int i = 0; i < FRAME && f * FRAME + i < samples.length; i++)
				frame[i] = (short)Math.round(samples[f * FRAME + i] * 32767.0D);
			frames.add(frame);
		}
		return frames;
	}
}

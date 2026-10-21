package com.arrl.radiocraft.common.radio.voice;

import java.util.Locale;

/**
 * CTCSS ("PL") sub-audible tones. An FM transmitter can send one under the voice; a receiver with tone squelch only
 * opens for its own tone, and a repeater with an access tone only repeats signals carrying it. 0 means no tone.
 */
public final class Ctcss {

	private Ctcss() {}

	public static final float NONE = 0.0F;

	/** The standard EIA tones, in Hz. */
	public static final float[] TONES = {
			67.0F, 71.9F, 74.4F, 77.0F, 79.7F, 82.5F, 85.4F, 88.5F, 91.5F, 94.8F, 97.4F, 100.0F, 103.5F, 107.2F, 110.9F,
			114.8F, 118.8F, 123.0F, 127.3F, 131.8F, 136.5F, 141.3F, 146.2F, 151.4F, 156.7F, 162.2F, 167.9F, 173.8F,
			179.9F, 186.2F, 192.8F, 203.5F, 210.7F, 218.1F, 225.7F, 233.6F, 241.8F, 250.3F
	};

	/** Position of {@code tone} in the list: 0 for none, 1 to {@code TONES.length} for a standard tone. */
	public static int index(float tone) {
		if(tone <= 0.0F)
			return 0;
		for(int i = 0; i < TONES.length; i++)
			if(Math.abs(TONES[i] - tone) < 0.05F)
				return i + 1;
		return 0;
	}

	/** The tone at a list position (see {@link #index}), clamped to the list. */
	public static float fromIndex(int index) {
		if(index <= 0)
			return NONE;
		return TONES[Math.min(index, TONES.length) - 1];
	}

	/** The tone {@code steps} positions along the list from {@code tone}, stopping at "none" and at the top. */
	public static float step(float tone, int steps) {
		return fromIndex(Math.max(0, Math.min(TONES.length, index(tone) + steps)));
	}

	/** A standard tone, or none for anything else (so a packet can only carry real tones). */
	public static float sanitize(float tone) {
		return fromIndex(index(tone));
	}

	/** True if a receiver set to {@code squelchTone} opens for a signal carrying {@code signalTone}. */
	public static boolean opens(float squelchTone, float signalTone) {
		return squelchTone <= 0.0F || Math.abs(squelchTone - signalTone) < 0.05F;
	}

	/** "100.0" or "OFF". */
	public static String format(float tone) {
		return tone <= 0.0F ? "OFF" : String.format(Locale.ROOT, "%.1f", tone);
	}

	/** Standard 2m repeater offset, in Hz. */
	public static final int REPEATER_OFFSET_HZ = 600_000;

	/** The next offset in the cycle simplex, minus, plus. */
	public static int nextOffset(int offsetHz) {
		return offsetHz == 0 ? -REPEATER_OFFSET_HZ : offsetHz < 0 ? REPEATER_OFFSET_HZ : 0;
	}

	/** Only simplex or the standard offset either way. */
	public static int sanitizeOffset(int offsetHz) {
		return offsetHz == 0 ? 0 : offsetHz < 0 ? -REPEATER_OFFSET_HZ : REPEATER_OFFSET_HZ;
	}

	/** "SPLX", "-600" or "+600" (kHz). */
	public static String formatOffset(int offsetHz) {
		return offsetHz == 0 ? "SPLX" : (offsetHz < 0 ? "-" : "+") + Math.abs(offsetHz) / 1000;
	}
}

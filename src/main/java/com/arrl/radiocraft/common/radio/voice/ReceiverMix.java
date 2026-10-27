package com.arrl.radiocraft.common.radio.voice;

/**
 * How a receiver's AGC balances the wanted signal against the noise. A real receiver sets its gain from the total power
 * coming in (signal plus noise) and holds the output steady, so a strong signal turns the gain down and the background
 * noise drops away under it, while a weak one leaves the gain up and the noise sits on top of it. Amplitudes are 0 to 1:
 * {@code signal} is the received strength, {@code noise} the noise floor (solar, redstone, lightning, aurora).
 */
public final class ReceiverMix {

	private ReceiverMix() {}

	/** Output level of the wanted signal: about 1 when it is well above the noise, its share of the total when not. */
	public static double voiceLevel(double signal, double noise) {
		if(signal <= 0.0D)
			return 0.0D;
		return signal / Math.sqrt(signal * signal + noise * noise);
	}

	/**
	 * Factor the background static is multiplied by: 1 with no signal (the static plays at the noise floor, as always),
	 * falling toward 0 as the signal rises above the noise.
	 */
	public static double noiseDuck(double signal, double noise) {
		if(signal <= 0.0D)
			return 1.0D;
		if(noise <= 0.0D)
			return 0.0D;
		return noise / Math.sqrt(signal * signal + noise * noise);
	}

	/** Signal strength from which an FM receiver is fully quiet (full quieting): no hiss under the voice. */
	public static final double FM_QUIETING = 0.3D;
	/** Below this an FM signal starts to cut in and out (picket fencing). */
	public static final double FM_DROPOUT_START = 0.15D;
	/** Below this the voice is gone: the squelch is open on static alone. */
	public static final double FM_STATIC_ONLY = 0.04D;

	/**
	 * The hiss under an FM signal, 0 to 1: none on a signal at {@link #FM_QUIETING} or above, rising as it weakens.
	 * FM does not get quieter as the signal fades, only noisier; the voice stays at full volume.
	 */
	public static double fmHiss(double signal) {
		if(signal <= 0.0D)
			return 1.0D;
		if(signal >= FM_QUIETING)
			return 0.0D;
		double below = 1.0D - signal / FM_QUIETING;
		return below * below;
	}

	/**
	 * Share of the time a weak FM signal drops out to static, 0 to 1: none above {@link #FM_DROPOUT_START}, all of it
	 * below {@link #FM_STATIC_ONLY}. Past that the signal is out of reach and the squelch shuts: silence.
	 */
	public static double fmDropout(double signal) {
		return Math.max(0.0D, Math.min(1.0D, (FM_DROPOUT_START - signal) / (FM_DROPOUT_START - FM_STATIC_ONLY)));
	}

	/**
	 * Slow fading (QSB) on a skip signal: a factor between {@code 1 - depth} and 1 that swells and sinks over several
	 * seconds, from two slow waves so it never quite repeats. {@code seed} makes each path fade on its own.
	 */
	public static double fading(double seconds, long seed, double depth) {
		double phase = (seed & 0xFFFF) / 65536.0D * Math.PI * 2.0D;
		double wave = 0.6D * Math.sin(seconds * 0.71D + phase) + 0.4D * Math.sin(seconds * 0.29D + phase * 1.7D);
		return 1.0D - depth * (0.5D + 0.5D * wave);
	}
}

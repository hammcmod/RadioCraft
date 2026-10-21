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

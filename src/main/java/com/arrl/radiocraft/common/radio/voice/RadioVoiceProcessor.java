package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.RadiocraftServerConfig;

/**
 * Turns clean microphone audio into what comes out of a radio's speaker. One per receiver, since the filters and the AGC
 * carry state from one 20 ms frame to the next. Steps, in order:
 * <ol>
 *     <li>Off frequency only ({@link Detuning}): the transmitter's passband, then a frequency shift by the pitch error,
 *     so the part of the voice that slides out of the receiver's passband is lost. Splatter on top, when asked.</li>
 *     <li>Band-pass to the voice channel (300-2700 Hz for SSB): no deep bass, no airy highs.</li>
 *     <li>AGC: brings quiet and loud talkers to the same steady level, fast to duck a shout, slow to come back up.</li>
 *     <li>Soft clipping: a little speaker overdrive on the peaks.</li>
 *     <li>Aurora rasp, when asked: the buzzy flutter of a VHF signal scattered off an aurora.</li>
 *     <li>Output level from {@link ReceiverMix#voiceLevel}.</li>
 * </ol>
 * Audio is 48 kHz mono 16-bit, as Simple Voice Chat hands it over.
 */
public class RadioVoiceProcessor {

	public static final double SAMPLE_RATE = 48000.0D;

	/** RMS level the AGC aims for, as a fraction of full scale. */
	private static final double AGC_TARGET = 0.22D;
	/** Most the AGC will boost a quiet talker (about +12 dB), so breathing and room noise are not pulled up to speech level. */
	private static final double AGC_MAX_GAIN = 4.0D;
	/** Envelope below which the input counts as silence and the AGC stops raising its gain. */
	private static final double AGC_GATE = 0.01D;
	private static final double ATTACK_SECONDS = 0.005D;
	private static final double RELEASE_SECONDS = 0.4D;

	/**
	 * @param lowHz   High-pass corner, the bottom of the voice channel.
	 * @param highHz  Low-pass corner, the top of the voice channel.
	 * @param drive   Soft clipping drive: 1 is barely there, 3 is a crunchy speaker.
	 * @param enabled False passes the audio through untouched except for the output level.
	 */
	public record Settings(double lowHz, double highHz, double drive, boolean enabled) {
		public static final Settings SSB = new Settings(300.0D, 2700.0D, 1.6D, true);
	}

	/** The settings from the server config. */
	public static Settings currentSettings() {
		return new Settings(RadiocraftServerConfig.VOICE_BAND_LOW.get(), RadiocraftServerConfig.VOICE_BAND_HIGH.get(),
				RadiocraftServerConfig.VOICE_DRIVE.get(), RadiocraftServerConfig.VOICE_PROCESSING_ENABLED.get());
	}

	private final Biquad[] filters = new Biquad[4];
	private double filterLow = -1.0D;
	private double filterHigh = -1.0D;
	private double envelope = 0.0D;
	private double agcGain = 1.0D;
	private double raspLevel = 0.0D;
	private long raspNoise = System.nanoTime() | 1L; // Xorshift state: a per-sample random number without java.util.Random's cost.
	private final Biquad[] txFilters = new Biquad[4];
	private double txShift = Double.NaN;
	private double txBaseLow = -1.0D;
	private double txBaseHigh = -1.0D;
	private final AllPassChain hilbertRe = new AllPassChain(0.6923878D, 0.9360654322959D, 0.9882295226860D, 0.9987488452737D);
	private final AllPassChain hilbertIm = new AllPassChain(0.4021921162426D, 0.8561710882420D, 0.9722909545651D, 0.9952884791278D);
	private double hilbertDelay = 0.0D;
	private double shiftPhase = 0.0D;
	private double splatterEnvelope = 0.0D;
	private double qrmFlutter = 0.0D;

	/**
	 * Processes one frame in place and returns it.
	 *
	 * @param pcm      The frame, modified.
	 * @param level    Output level, 0 to 1 (see {@link ReceiverMix#voiceLevel}).
	 * @param rasp     Aurora rasp, 0 (none) to 1 (full).
	 */
	public short[] process(short[] pcm, Settings settings, double level, double rasp) {
		return process(pcm, settings, level, rasp, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * Processes one frame in place and returns it.
	 *
	 * @param pcm          The frame, modified.
	 * @param level        Output level, 0 to 1 (see {@link ReceiverMix#voiceLevel}).
	 * @param rasp         Aurora rasp, 0 (none) to 1 (full).
	 * @param pitchShiftHz Pitch error of a mistuned SSB signal (see {@link Detuning#ssb}), 0 on frequency.
	 * @param splatter     Splatter that follows this signal's own voice (the edge of the SSB window), 0 to 1.
	 * @param qrm          Splatter from a signal next door (FM), 0 to 1; it crackles whether or not this one is talking.
	 */
	public short[] process(short[] pcm, Settings settings, double level, double rasp, double pitchShiftHz, double splatter, double qrm) {
		if(!settings.enabled()) {
			for(int i = 0; i < pcm.length; i++)
				pcm[i] = clamp(pcm[i] * level);
			return pcm;
		}
		setUpFilters(settings.lowHz(), settings.highHz());
		double attack = coefficient(ATTACK_SECONDS);
		double release = coefficient(RELEASE_SECONDS);
		double drive = Math.max(0.1D, settings.drive());
		double driveNorm = softClip(drive);
		double raspSmooth = coefficient(1.0D / 120.0D); // Rasp noise low-passed to ~120 Hz: a buzz, not a hiss.
		boolean shifting = Math.abs(pitchShiftHz) >= 1.0D;
		if(shifting)
			setUpShift(settings.lowHz(), settings.highHz(), pitchShiftHz);
		double phaseStep = 2.0D * Math.PI * pitchShiftHz / SAMPLE_RATE;
		double splatterAttack = coefficient(0.002D);
		double splatterRelease = coefficient(0.05D);
		double flutterSmooth = coefficient(1.0D / 40.0D); // QRM bursts at a few tens of Hz: crackle, not steady hiss.

		for(int i = 0; i < pcm.length; i++) {
			double x = pcm[i] / 32768.0D;

			if(shifting) {
				for(Biquad filter : txFilters)
					x = filter.process(x);
				x = shift(x);
				shiftPhase += phaseStep;
				if(shiftPhase > Math.PI)
					shiftPhase -= 2.0D * Math.PI;
				else if(shiftPhase < -Math.PI)
					shiftPhase += 2.0D * Math.PI;
			}

			if(splatter > 0.0D) { // Noise riding on the signal's own envelope: garble that comes and goes with the words.
				double magnitude = Math.abs(x);
				splatterEnvelope = magnitude > splatterEnvelope
						? splatterAttack * splatterEnvelope + (1.0D - splatterAttack) * magnitude
						: splatterRelease * splatterEnvelope + (1.0D - splatterRelease) * magnitude;
				x += splatter * 2.0D * splatterEnvelope * (nextRandom() * 2.0D - 1.0D);
			}
			if(qrm > 0.0D) {
				qrmFlutter = flutterSmooth * qrmFlutter + (1.0D - flutterSmooth) * nextRandom();
				double burst = Math.max(0.0D, qrmFlutter * 2.0D - 0.8D); // Mostly quiet, with bursts.
				x += qrm * 0.5D * burst * (nextRandom() * 2.0D - 1.0D);
			}

			for(Biquad filter : filters)
				x = filter.process(x);

			double magnitude = Math.abs(x);
			envelope = magnitude > envelope ? attack * envelope + (1.0D - attack) * magnitude : release * envelope + (1.0D - release) * magnitude;
			double wanted = envelope > AGC_GATE ? Math.min(AGC_MAX_GAIN, AGC_TARGET / envelope) : agcGain; // Hold the gain through silence.
			agcGain = wanted < agcGain ? attack * agcGain + (1.0D - attack) * wanted : release * agcGain + (1.0D - release) * wanted;
			x *= agcGain;

			x = softClip(x * drive) / driveNorm;

			if(rasp > 0.0D) {
				double white = nextRandom();
				raspLevel = raspSmooth * raspLevel + (1.0D - raspSmooth) * white;
				x *= 1.0D - rasp + rasp * Math.min(2.0D, raspLevel * 2.0D);
			}

			pcm[i] = clamp(x * level * 32767.0D);
		}
		return pcm;
	}

	/** Uniform 0 to 1, averaging 0.5, from the xorshift state. */
	private double nextRandom() {
		raspNoise ^= raspNoise << 13;
		raspNoise ^= raspNoise >>> 7;
		raspNoise ^= raspNoise << 17;
		return (raspNoise >>> 11) * 0x1.0p-53;
	}

	/**
	 * The transmitter's passband for a shifted signal: only the part of the voice that lands back inside the receiver's
	 * {@code lowHz}-{@code highHz} after the shift. That cuts the top of the voice when the pitch goes up and the bottom
	 * when it goes down, and keeps anything from being shifted below 0 Hz and folding back.
	 */
	private void setUpShift(double lowHz, double highHz, double shiftHz) {
		if(Math.abs(shiftHz - txShift) < 1.0D && lowHz == txBaseLow && highHz == txBaseHigh)
			return;
		txShift = shiftHz;
		txBaseLow = lowHz;
		txBaseHigh = highHz;
		double low = Math.max(lowHz, lowHz - shiftHz);
		double high = Math.max(low + 50.0D, Math.min(highHz, highHz - shiftHz));
		txFilters[0] = Biquad.highPass(low, 0.5412D);
		txFilters[1] = Biquad.highPass(low, 1.3066D);
		txFilters[2] = Biquad.lowPass(high, 0.5412D);
		txFilters[3] = Biquad.lowPass(high, 1.3066D);
	}

	/**
	 * Moves every frequency in the signal up by the shift (down if negative), the way a mistuned SSB receiver does: a
	 * pair of all-pass chains 90 degrees apart (Olli Niemitalo's design) makes the analytic signal, which is then turned
	 * by the shift's phase.
	 */
	private double shift(double x) {
		double re = hilbertDelay;
		hilbertDelay = hilbertRe.process(x); // The real path runs one sample late, as the design has it.
		double im = hilbertIm.process(x);
		return re * Math.cos(shiftPhase) + im * Math.sin(shiftPhase);
	}

	private void setUpFilters(double lowHz, double highHz) {
		if(lowHz == filterLow && highHz == filterHigh)
			return;
		filterLow = lowHz;
		filterHigh = highHz;
		// Two 2nd order sections each side make 4th order Butterworth slopes (Q 0.541 and 1.307).
		filters[0] = Biquad.highPass(lowHz, 0.5412D);
		filters[1] = Biquad.highPass(lowHz, 1.3066D);
		filters[2] = Biquad.lowPass(highHz, 0.5412D);
		filters[3] = Biquad.lowPass(highHz, 1.3066D);
	}

	/** A tanh-shaped saturation curve (Padé approximation), flat at ±1 past ±3; much cheaper than Math.tanh per sample. */
	static double softClip(double x) {
		if(x >= 3.0D)
			return 1.0D;
		if(x <= -3.0D)
			return -1.0D;
		double x2 = x * x;
		return x * (27.0D + x2) / (27.0D + 9.0D * x2);
	}

	/** One-pole smoothing coefficient for a time constant in seconds. */
	private static double coefficient(double seconds) {
		return Math.exp(-1.0D / (seconds * SAMPLE_RATE));
	}

	private static short clamp(double sample) {
		return (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
	}

	/** All-pass sections {@code y[n] = c^2 (x[n] + y[n-2]) - x[n-2]} in series: one half of the 90 degree pair. */
	static final class AllPassChain {
		private final double[] a;
		private final double[] x1, x2, y1, y2;

		AllPassChain(double... coefficients) {
			a = new double[coefficients.length];
			for(int i = 0; i < a.length; i++)
				a[i] = coefficients[i] * coefficients[i];
			x1 = new double[a.length];
			x2 = new double[a.length];
			y1 = new double[a.length];
			y2 = new double[a.length];
		}

		double process(double x) {
			for(int i = 0; i < a.length; i++) {
				double y = a[i] * (x + y2[i]) - x2[i];
				x2[i] = x1[i];
				x1[i] = x;
				y2[i] = y1[i];
				y1[i] = y;
				x = y;
			}
			return x;
		}
	}

	/** A 2nd order IIR section (RBJ audio EQ cookbook), direct form I. */
	static final class Biquad {
		private final double b0, b1, b2, a1, a2;
		private double x1, x2, y1, y2;

		private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
			this.b0 = b0 / a0;
			this.b1 = b1 / a0;
			this.b2 = b2 / a0;
			this.a1 = a1 / a0;
			this.a2 = a2 / a0;
		}

		static Biquad lowPass(double hz, double q) {
			double w = 2.0D * Math.PI * hz / SAMPLE_RATE;
			double alpha = Math.sin(w) / (2.0D * q);
			double cos = Math.cos(w);
			return new Biquad((1.0D - cos) / 2.0D, 1.0D - cos, (1.0D - cos) / 2.0D, 1.0D + alpha, -2.0D * cos, 1.0D - alpha);
		}

		static Biquad highPass(double hz, double q) {
			double w = 2.0D * Math.PI * hz / SAMPLE_RATE;
			double alpha = Math.sin(w) / (2.0D * q);
			double cos = Math.cos(w);
			return new Biquad((1.0D + cos) / 2.0D, -(1.0D + cos), (1.0D + cos) / 2.0D, 1.0D + alpha, -2.0D * cos, 1.0D - alpha);
		}

		double process(double x) {
			double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
			x2 = x1;
			x1 = x;
			y2 = y1;
			y1 = y;
			return y;
		}
	}
}

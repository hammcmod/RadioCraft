package com.arrl.radiocraft.common.radio.voice;

/**
 * What a receiver makes of a voice signal that is not quite on its frequency. Offsets are in Hz, receiver minus
 * transmitter; a radio steps 1 kHz at a time, so in practice they are whole kHz (give or take float rounding).
 * <ul>
 *     <li><b>SSB</b>: anything up to 10 kHz off is heard, weaker the further off it is, with its pitch shifted the way a
 *     mistuned SSB signal sounds. The shift also slides part of the voice out of the receiver's passband, so the top or
 *     the bottom of the voice is cut off. Towards the end of the window splatter creeps in.</li>
 *     <li><b>FM</b>: up to 2 kHz off sounds as usual, a little weaker. From 3 to 10 kHz off (and on VHF, one channel step
 *     off) the signal does not open the squelch at all, but it puts splatter on whatever the receiver is hearing on its
 *     own frequency.</li>
 * </ul>
 */
public final class Detuning {

	private Detuning() {}

	/** Offsets under this count as on frequency (float rounding at 144 MHz is 16 Hz). */
	static final double ON_FREQUENCY_HZ = 500.0D;
	/** Furthest an SSB signal can be off and still be heard. */
	public static final double SSB_WINDOW_HZ = 10_000.0D;
	/** Furthest an FM signal can be off and still be heard (weaker). */
	public static final double FM_CLEAR_HZ = 2_000.0D;
	/** Furthest an FM signal can be off and still cause splatter (without a channel step to cover). */
	public static final double FM_SPLATTER_HZ = 10_000.0D;

	/** Strength left at the edge of the SSB window. */
	private static final double SSB_EDGE_STRENGTH = 0.4D;
	/** Pitch shift per Hz off: 10 kHz off moves the voice 1.5 kHz, leaving 900 Hz of it in a 300-2700 Hz passband. */
	private static final double SSB_SHIFT_PER_HZ = 0.15D;
	/** Share of the SSB window (from the centre) that is clean; past it splatter rises to {@link #SSB_EDGE_SPLATTER}. */
	private static final double SSB_SPLATTER_START = 0.6D;
	private static final double SSB_EDGE_SPLATTER = 0.6D;
	/** Strength lost per kHz off on FM, inside {@link #FM_CLEAR_HZ}. */
	private static final double FM_LOSS_PER_KHZ = 0.15D;
	/** FM splatter 3 kHz off, falling to {@link #FM_FAR_SPLATTER} at 10 kHz; one channel step off it is {@link #FM_STEP_SPLATTER}. */
	private static final double FM_NEAR_SPLATTER = 0.5D;
	private static final double FM_FAR_SPLATTER = 0.2D;
	private static final double FM_STEP_SPLATTER = 0.15D;

	/**
	 * @param audible       Played as a signal of its own: opens the squelch, counts as received.
	 * @param strength      Multiplier on the received strength.
	 * @param pitchShiftHz  How far the voice moves in pitch (positive is higher).
	 * @param splatter      Splatter mixed into this signal's own audio, 0 to 1.
	 * @param qrm           For a signal that is not audible: splatter it puts on the signal the receiver is hearing, 0 to 1
	 *                      (before scaling by the two signals' strengths).
	 */
	public record Effect(boolean audible, double strength, double pitchShiftHz, double splatter, double qrm) {
		public boolean isNone() {
			return !audible && qrm <= 0.0D;
		}
	}

	public static final Effect ON_FREQUENCY = new Effect(true, 1.0D, 0.0D, 0.0D, 0.0D);
	public static final Effect NONE = new Effect(false, 0.0D, 0.0D, 0.0D, 0.0D);

	/**
	 * An SSB receiver on {@code rxHz} hearing a signal on {@code txHz}.
	 *
	 * @param lowerSideband True on bands that use LSB (below 10 MHz): tuning above the signal raises its pitch there and
	 *                      lowers it on USB.
	 */
	public static Effect ssb(double rxHz, double txHz, boolean lowerSideband) {
		double offset = rxHz - txHz;
		double off = Math.abs(offset);
		if(off < ON_FREQUENCY_HZ)
			return ON_FREQUENCY;
		if(off > SSB_WINDOW_HZ + ON_FREQUENCY_HZ)
			return NONE;
		double t = Math.min(1.0D, off / SSB_WINDOW_HZ);
		double strength = 1.0D - (1.0D - SSB_EDGE_STRENGTH) * t;
		double shift = (lowerSideband ? offset : -offset) * SSB_SHIFT_PER_HZ;
		double splatter = t <= SSB_SPLATTER_START ? 0.0D : (t - SSB_SPLATTER_START) / (1.0D - SSB_SPLATTER_START) * SSB_EDGE_SPLATTER;
		return new Effect(true, strength, shift, splatter, 0.0D);
	}

	/**
	 * An FM receiver on {@code rxHz} hearing a signal on {@code txHz}.
	 *
	 * @param channelStepHz The receiver's channel step (VHF), 0 if it has none: a signal one step off still splatters.
	 */
	public static Effect fm(double rxHz, double txHz, double channelStepHz) {
		double off = Math.abs(rxHz - txHz);
		if(off < ON_FREQUENCY_HZ)
			return ON_FREQUENCY;
		if(off <= FM_CLEAR_HZ + ON_FREQUENCY_HZ)
			return new Effect(true, 1.0D - FM_LOSS_PER_KHZ * off / 1000.0D, 0.0D, 0.0D, 0.0D);
		if(off <= FM_SPLATTER_HZ + ON_FREQUENCY_HZ) {
			double t = Math.min(1.0D, (off - 3000.0D) / (FM_SPLATTER_HZ - 3000.0D));
			return new Effect(false, 0.0D, 0.0D, 0.0D, FM_NEAR_SPLATTER - (FM_NEAR_SPLATTER - FM_FAR_SPLATTER) * Math.max(0.0D, t));
		}
		if(channelStepHz > 0.0D && off <= channelStepHz + ON_FREQUENCY_HZ)
			return new Effect(false, 0.0D, 0.0D, 0.0D, FM_STEP_SPLATTER);
		return NONE;
	}

	/**
	 * How much splatter a signal of strength {@code interferer} with {@link Effect#qrm} {@code qrm} puts on a wanted
	 * signal of strength {@code wanted}: the stronger the interferer is next to the wanted signal, the more, up to 1.
	 */
	public static double splatterOn(double qrm, double interferer, double wanted) {
		if(qrm <= 0.0D || interferer <= 0.0D)
			return 0.0D;
		return Math.min(1.0D, qrm * interferer / Math.max(0.05D, wanted));
	}
}

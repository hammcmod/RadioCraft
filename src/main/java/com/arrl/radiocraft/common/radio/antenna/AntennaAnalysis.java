package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.Band;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * What the antenna analyzer says about an antenna: the band it is resonant on, or the band it is closest to and whether
 * it is too short or too long for it, worked out from nothing but its SWR across frequency.
 */
public final class AntennaAnalysis {

	/** At or under this SWR somewhere in a band the antenna counts as resonant there. */
	public static final double RESONANT_SWR = 1.5D;
	/** Past this everywhere the antenna is resonant on no band at all. */
	public static final double USELESS_SWR = 10.0D;
	private static final int BAND_STEPS = 16;
	private static final int SWEEP_STEPS = 240;
	/** How far either side of the best band the sweep looks for the frequency the antenna is resonant at. */
	private static final double SWEEP_SPAN = 0.45D;

	public enum Verdict { RESONANT, TOO_SHORT, TOO_LONG, POOR_MATCH, NONE }

	/** One band's best SWR and where in the band it is. */
	public record BandSwr(Band band, double swr, double frequencyHertz) {}

	/**
	 * @param best the band with the lowest SWR, null when the antenna works on none.
	 * @param resonanceHertz where the SWR is lowest near {@code best}; outside the band for too short and too long.
	 * @param bands every band's lowest SWR, in the order given.
	 */
	public record Result(Verdict verdict, Band best, double bestSwr, double resonanceHertz, List<BandSwr> bands) {}

	private AntennaAnalysis() {}

	public static Result analyze(DoubleUnaryOperator swr, Collection<Band> bands) {
		List<BandSwr> perBand = new ArrayList<>();
		BandSwr best = null;
		for(Band band : bands) {
			BandSwr lowest = null;
			for(int i = 0; i <= BAND_STEPS; i++) {
				double f = band.minFrequency() + (band.maxFrequency() - band.minFrequency()) * i / BAND_STEPS;
				double s = swr.applyAsDouble(f);
				if(Double.isFinite(s) && (lowest == null || s < lowest.swr()))
					lowest = new BandSwr(band, s, f);
			}
			if(lowest == null)
				lowest = new BandSwr(band, Double.POSITIVE_INFINITY, (band.minFrequency() + band.maxFrequency()) / 2.0D);
			perBand.add(lowest);
			if(best == null || lowest.swr() < best.swr())
				best = lowest;
		}
		if(best == null || !(best.swr() < USELESS_SWR))
			return new Result(Verdict.NONE, null, best == null ? Double.POSITIVE_INFINITY : best.swr(), 0.0D, perBand);
		if(best.swr() <= RESONANT_SWR)
			return new Result(Verdict.RESONANT, best.band(), best.swr(), best.frequencyHertz(), perBand);

		// Sweep around the band for where the antenna really is resonant: below the band it is too long, above too short.
		double from = best.band().minFrequency() * (1.0D - SWEEP_SPAN);
		double to = best.band().maxFrequency() * (1.0D + SWEEP_SPAN);
		double dipHertz = best.frequencyHertz();
		double dipSwr = best.swr();
		for(int i = 0; i <= SWEEP_STEPS; i++) {
			double f = from + (to - from) * i / SWEEP_STEPS;
			double s = swr.applyAsDouble(f);
			if(Double.isFinite(s) && s < dipSwr - 1e-6) {
				dipSwr = s;
				dipHertz = f;
			}
		}
		Verdict verdict = dipHertz < best.band().minFrequency() ? Verdict.TOO_LONG
				: dipHertz > best.band().maxFrequency() ? Verdict.TOO_SHORT : Verdict.POOR_MATCH;
		return new Result(verdict, best.band(), best.swr(), dipHertz, perBand);
	}
}

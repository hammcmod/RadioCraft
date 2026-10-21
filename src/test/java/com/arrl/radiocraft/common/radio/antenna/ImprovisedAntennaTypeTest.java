package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.antenna.types.ImprovisedAntennaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImprovisedAntennaTypeTest {

	/** 20m: 21.15 m wavelength at 14.175 MHz, so a resonant arm is about 5.3 blocks. */
	private static final double WAVELENGTH_20M = 299792458.0D / 14_175_000.0D;

	private static double swr(double[] arms, double penalty, int ratio) {
		return ImprovisedAntennaType.swr(arms, penalty, ratio, WAVELENGTH_20M);
	}

	@Test
	void nearlyADipoleIsNearlyAsGoodAsOne() {
		double clean = swr(new double[] { 5.29, 5.29 }, 0.0D, 1);
		assertEquals(1.0D, clean, 0.01D);
		double dipping = swr(new double[] { 5.10, 5.00 }, 0.2D, 1); // The playtest build: one arm drops a block.
		assertTrue(dipping < 1.8D, "a dipole with one arm dipping a block should still be a decent match, was " + dipping);
	}

	@Test
	void worseShapesGiveWorseSwr() {
		double good = swr(new double[] { 5.3, 5.3 }, 0.0D, 1);
		double bent = swr(new double[] { 5.3, 5.3 }, 1.5D, 1);
		double offLength = swr(new double[] { 9.0, 2.0 }, 0.0D, 1);
		double wrongBalun = swr(new double[] { 5.3, 5.3 }, 0.0D, 9);
		assertTrue(bent > good);
		assertTrue(offLength > good);
		assertTrue(wrongBalun > good);
	}

	@Test
	void armsResonateAtOddQuarterWaves() {
		double quarter = WAVELENGTH_20M / 4.0D;
		assertEquals(1.0D, swr(new double[] { 3.0D * quarter }, 0.0D, 9), 1e-9, "a 3/4 wave wire on a 9:1 is resonant");
		assertTrue(swr(new double[] { 2.0D * quarter }, 0.0D, 9) > 1.0D, "a half wave fed at the end is not");
	}

	@Test
	void anythingStillWorks() {
		double tangle = swr(new double[] { 40.0, 0.5 }, 8.0D, 4);
		assertTrue(tangle <= ImprovisedAntennaType.MAX_SWR);
		assertTrue(tangle > 5.0D, "a tangle should be a poor match, was " + tangle);
	}
}

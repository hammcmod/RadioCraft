package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetuningTest {

	private static final double F = 14_200_000.0D;

	@Test
	void onFrequencyIsUntouched() {
		assertSame(Detuning.ON_FREQUENCY, Detuning.ssb(F, F, false));
		assertSame(Detuning.ON_FREQUENCY, Detuning.fm(144_001_008.0D, 144_001_000.0D, 20_000.0D)); // Float rounding at 2m.
	}

	@Test
	void ssbIsHeardAcrossTenKilohertzWeakerAndShifted() {
		Detuning.Effect one = Detuning.ssb(F + 1000, F, false);
		Detuning.Effect ten = Detuning.ssb(F + 10_000, F, false);
		assertTrue(one.audible() && ten.audible());
		assertTrue(one.strength() < 1.0D && ten.strength() < one.strength());
		assertEquals(-150.0D, one.pitchShiftHz(), 1e-6, "USB tuned above the signal sounds lower");
		assertEquals(150.0D, Detuning.ssb(F + 1000, F, true).pitchShiftHz(), 1e-6, "LSB the other way round");
		assertEquals(0.0D, one.splatter());
		assertTrue(ten.splatter() > 0.0D, "splatter at the end of the window");
		assertTrue(Detuning.ssb(F + 11_000, F, false).isNone());
	}

	@Test
	void fmIsClearToTwoKilohertzThenOnlySplatters() {
		Detuning.Effect two = Detuning.fm(F + 2000, F, 0);
		assertTrue(two.audible());
		assertTrue(two.strength() < 1.0D);
		assertEquals(0.0D, two.pitchShiftHz());

		Detuning.Effect three = Detuning.fm(F - 3000, F, 0);
		assertFalse(three.audible(), "3 kHz off does not open the squelch");
		assertTrue(three.qrm() > 0.0D);
		assertTrue(Detuning.fm(F + 10_000, F, 0).qrm() > 0.0D);
		assertTrue(Detuning.fm(F + 11_000, F, 0).isNone());
	}

	@Test
	void fmOneVhfStepOffStillSplatters() {
		Detuning.Effect step = Detuning.fm(146_520_000.0D, 146_540_000.0D, 20_000.0D);
		assertFalse(step.audible());
		assertTrue(step.qrm() > 0.0D);
		assertTrue(Detuning.fm(146_520_000.0D, 146_560_000.0D, 20_000.0D).isNone());
	}

	@Test
	void splatterGrowsWithTheInterferer() {
		assertEquals(0.0D, Detuning.splatterOn(0.5D, 0.0D, 1.0D));
		assertTrue(Detuning.splatterOn(0.5D, 0.8D, 0.4D) > Detuning.splatterOn(0.5D, 0.2D, 0.4D));
		assertEquals(1.0D, Detuning.splatterOn(0.5D, 1.0D, 0.1D));
	}
}

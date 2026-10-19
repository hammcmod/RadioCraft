package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins down the current propagation model in {@link BandUtils#getBaseStrength}: linear falloff inside line of sight,
 * a triangular "skip" window that differs between day and night, and nothing in between.
 */
class BandUtilsTest {

    private static final double EPS = 1e-9;
    // Same values as the 20m default in Band#getDefaults.
    private static final Band TWENTY_M = new Band("20m", 300, 2000, 6000, 800, 1000, 14_000_000, 14_350_000);

    @Test
    void losStrengthFallsLinearlyToZero() {
        assertEquals(1.0, BandUtils.getBaseStrength(TWENTY_M, 0, 1.0, 1.0, true), EPS);
        assertEquals(0.5, BandUtils.getBaseStrength(TWENTY_M, 150, 1.0, 1.0, true), EPS);
        assertEquals(0.25, BandUtils.getBaseStrength(TWENTY_M, 150, 0.5, 1.0, true), EPS);
    }

    @Test
    void deadZoneBetweenLosAndSkip() {
        assertEquals(0.0, BandUtils.getBaseStrength(TWENTY_M, 1000, 1.0, 1.0, true), EPS);
    }

    @Test
    void daySkipPeaksInTheMiddleOfTheWindow() {
        assertEquals(1.0, BandUtils.getBaseStrength(TWENTY_M, 4000, 1.0, 1.0, true), EPS);
        assertEquals(0.5, BandUtils.getBaseStrength(TWENTY_M, 5000, 1.0, 1.0, true), EPS);
        assertEquals(0.0, BandUtils.getBaseStrength(TWENTY_M, 7000, 1.0, 1.0, true), EPS);
    }

    @Test
    void nightUsesTheNightSkipWindow() {
        assertEquals(1.0, BandUtils.getBaseStrength(TWENTY_M, 900, 1.0, 1.0, false), EPS);
        assertEquals(0.0, BandUtils.getBaseStrength(TWENTY_M, 4000, 1.0, 1.0, false), EPS);
    }

    @Test
    void nullBandHasNoStrength() {
        assertEquals(0.0, BandUtils.getBaseStrength(null, 10, 1.0, 1.0, true), EPS);
    }

    @Test
    void wavelengthFromFrequency() {
        assertEquals(21.41, BandUtils.getWavelengthMetersFromFrequencyHertz(14_000_000), 0.01);
        assertEquals(2.08, BandUtils.getWavelengthMetersFromFrequencyHertz(144_000_000), 0.01);
    }
}

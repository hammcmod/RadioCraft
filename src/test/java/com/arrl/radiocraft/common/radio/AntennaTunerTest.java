package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The antenna tuner maths: within range the radio sees 1:1 and most of the mismatch loss is recovered, beyond range
 * the tuner does nothing.
 */
class AntennaTunerTest {

    private static final double EPS = 1e-9;
    private static final double MAX = 6.0;   // config default antenna_tuner_max_swr
    private static final double LOSS = 0.25; // config default antenna_tuner_loss

    @Test
    void matchedAntennaStaysMatched() {
        assertEquals(1.0, SWRHelper.tunedSWR(1.0, MAX), EPS);
        assertEquals(1.0, SWRHelper.tunedEfficiency(1.0, MAX, LOSS), EPS);
    }

    @Test
    void mismatchWithinRangeIsBroughtToUnity() {
        assertEquals(1.0, SWRHelper.tunedSWR(3.0, MAX), EPS);
        assertEquals(1.0, SWRHelper.tunedSWR(6.0, MAX), EPS); // Inclusive at the limit.
    }

    @Test
    void mismatchBeyondRangePassesThrough() {
        assertEquals(6.5, SWRHelper.tunedSWR(6.5, MAX), EPS);
        assertEquals(10.0, SWRHelper.tunedSWR(10.0, MAX), EPS);
        assertEquals(SWRHelper.getEfficiencyMultiplier(10.0), SWRHelper.tunedEfficiency(10.0, MAX, LOSS), EPS);
    }

    @Test
    void noAntennaIsPassedThrough() {
        assertEquals(0.0, SWRHelper.tunedSWR(0.0, MAX), EPS);
    }

    @Test
    void tunerRecoversMostOfTheMismatchLoss() {
        // SWR 3 reflects a quarter of the power; the tuner loses a quarter of that quarter.
        assertEquals(0.75, SWRHelper.getEfficiencyMultiplier(3.0), EPS);
        assertEquals(1.0 - 0.25 * LOSS, SWRHelper.tunedEfficiency(3.0, MAX, LOSS), EPS);
        // A perfect tuner (loss 0) recovers everything, a useless one (loss 1) only quiets the radio.
        assertEquals(1.0, SWRHelper.tunedEfficiency(3.0, MAX, 0.0), EPS);
        assertEquals(0.75, SWRHelper.tunedEfficiency(3.0, MAX, 1.0), EPS);
    }

    @Test
    void furtherOffResonanceStillCostsMore() {
        double two = SWRHelper.tunedEfficiency(2.0, MAX, LOSS);
        double four = SWRHelper.tunedEfficiency(4.0, MAX, LOSS);
        double six = SWRHelper.tunedEfficiency(6.0, MAX, LOSS);
        assertTrue(two > four && four > six, two + " > " + four + " > " + six);
        assertTrue(six > SWRHelper.getEfficiencyMultiplier(6.0), "Tuned is better than untuned at the limit");
    }
}

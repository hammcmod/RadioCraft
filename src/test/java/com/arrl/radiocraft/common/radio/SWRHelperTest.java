package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chance a transmitter blows when keyed into a mismatch: nothing below the threshold, then following the reflected
 * power up to the configured chance at SWR 10.
 */
class SWRHelperTest {

    private static final double EPS = 1e-9;

    @Test
    void nothingBelowTheThresholdOrWithoutAnAntenna() {
        assertEquals(0.0, SWRHelper.blowChancePerSecond(0.0, 3.0, 0.05), EPS);
        assertEquals(0.0, SWRHelper.blowChancePerSecond(1.0, 3.0, 0.05), EPS);
        assertEquals(0.0, SWRHelper.blowChancePerSecond(2.99, 3.0, 0.05), EPS);
    }

    @Test
    void worstMatchGivesTheConfiguredChance() {
        assertEquals(0.05, SWRHelper.blowChancePerSecond(SWRHelper.OVERDRAW_SWR, 3.0, 0.05), EPS);
    }

    @Test
    void chanceFollowsReflectedPower() {
        // SWR 3 reflects 1/4 of the power, SWR 10 reflects (9/11)^2.
        double atThree = SWRHelper.blowChancePerSecond(3.0, 3.0, 0.05);
        assertEquals(0.05 * 0.25 / ((9.0 / 11.0) * (9.0 / 11.0)), atThree, EPS);
        double atSix = SWRHelper.blowChancePerSecond(6.0, 3.0, 0.05);
        assertTrue(atThree < atSix && atSix < 0.05, "SWR 6 should sit between SWR 3 and SWR 10: " + atSix);
    }

    @Test
    void zeroChanceNeverBlowsAndChanceIsCapped() {
        assertEquals(0.0, SWRHelper.blowChancePerSecond(10.0, 3.0, 0.0), EPS);
        assertEquals(1.0, SWRHelper.blowChancePerSecond(100.0, 3.0, 1.0), EPS);
    }
}

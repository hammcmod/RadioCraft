package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The manual tuner's dials and lamp, and the automatic tuner's retune rule and clicks. */
class TunerMathTest {

    private static final double EPS = 1e-9;

    @Test
    void inRangeTunesToOneAndBeyondTakesOffWhatItCan() {
        assertEquals(1.0, TunerMath.bestSWR(3.0, 7.0), EPS);
        assertEquals(1.0, TunerMath.bestSWR(7.0, 7.0), EPS);
        assertEquals(3.0, TunerMath.bestSWR(9.0, 7.0), EPS);
        assertEquals(0.0, TunerMath.bestSWR(0.0, 7.0), EPS);
    }

    @Test
    void targetsStayOffTheStopsAndDependOnTheSeed() {
        for(long seed = 0; seed < 200; seed++) {
            for(int target : TunerMath.targets(seed, seed % 2 == 0 ? 0.0 : 1.0)) {
                assertTrue(target >= TunerMath.TARGET_MIN && target <= TunerMath.TARGET_MAX, "target " + target);
            }
        }
        assertFalse(java.util.Arrays.equals(TunerMath.targets(TunerMath.seed(1, 2, 3), 0.5), TunerMath.targets(TunerMath.seed(1, 2, 4), 0.5)));
        assertArrayEquals(TunerMath.targets(42L, 0.5), TunerMath.targets(42L, 0.5));
    }

    @Test
    void dialsOnTargetGiveTheBestMatchAndEveryDialHelps() {
        int[] targets = {20, 30, 40};
        assertEquals(1.0, TunerMath.manualSWR(4.0, 7.0, targets, targets), EPS);
        assertEquals(3.0, TunerMath.manualSWR(9.0, 7.0, targets, targets), EPS);
        double allOff = TunerMath.manualSWR(4.0, 7.0, new int[]{0, 0, 0}, targets);
        double oneOn = TunerMath.manualSWR(4.0, 7.0, new int[]{20, 0, 0}, targets);
        double twoOn = TunerMath.manualSWR(4.0, 7.0, new int[]{20, 30, 0}, targets);
        assertTrue(allOff > oneOn && oneOn > twoOn && twoOn > 1.0, allOff + " " + oneOn + " " + twoOn);
        assertTrue(allOff <= 4.0, "never worse than the antenna itself");
        double offByOne = TunerMath.manualSWR(4.0, 7.0, new int[]{21, 30, 40}, targets);
        assertTrue(offByOne > TunerMath.PERFECT_SWR, "one step off is not a perfect tune: " + offByOne);
    }

    @Test
    void perfectTuneMatchesTheOldTunerEfficiency() {
        assertEquals(SWRHelper.tunedEfficiency(3.0, 7.0, 0.25), TunerMath.efficiency(3.0, 1.0, 0.25), EPS);
        assertTrue(TunerMath.efficiency(3.0, 2.0, 0.25) < TunerMath.efficiency(3.0, 1.0, 0.25));
    }

    @Test
    void lampGoesRedThenYellowGreenThenSolidGreen() {
        assertEquals(TunerMath.Lamp.GREEN, TunerMath.lamp(4.0, 1.0, 7.3));
        assertEquals(TunerMath.Lamp.OFF, TunerMath.lamp(4.0, 0.0, 0.0));
        assertEquals(TunerMath.Lamp.RED, TunerMath.lamp(6.0, 5.0, 0.0)); // Start of a cycle: lit.
        assertEquals(TunerMath.Lamp.YELLOW_GREEN, TunerMath.lamp(6.0, 2.0, 0.0));
        assertTrue(TunerMath.blinkPeriodTicks(6.0, 6.0) > TunerMath.blinkPeriodTicks(6.0, 4.0), "red blinks faster closer to 3");
        assertTrue(TunerMath.blinkPeriodTicks(6.0, 2.5) > TunerMath.blinkPeriodTicks(6.0, 1.2), "yellow-green blinks faster closer to 1");
        assertEquals(TunerMath.SLOWEST_BLINK, TunerMath.blinkPeriodTicks(6.0, 6.0), EPS);
    }

    @Test
    void autoTunerRetunesOnAMegahertzOrANewBand() {
        assertTrue(TunerMath.needsRetune(0.0F, null, 14_100_000.0F, "20m"));
        assertFalse(TunerMath.needsRetune(14_000_000.0F, "20m", 14_350_000.0F, "20m"));
        assertTrue(TunerMath.needsRetune(28_000_000.0F, "10m", 29_000_000.0F, "10m"));
        assertTrue(TunerMath.needsRetune(14_000_000.0F, "20m", 14_000_000.0F, "40m"));
    }

    @Test
    void clicksSpeedUpWhenTuningButNotWhenFailing() {
        int firstGap = -1, lastGap = -1, previous = -1;
        for(int t = 0; t < TunerMath.TUNE_TICKS; t++) {
            if(TunerMath.clicksAt(t, false)) {
                if(previous >= 0) {
                    if(firstGap < 0)
                        firstGap = t - previous;
                    lastGap = t - previous;
                }
                previous = t;
            }
        }
        assertTrue(firstGap > lastGap && lastGap == 1, "first gap " + firstGap + ", last " + lastGap);
        for(int t = 0; t < TunerMath.FAIL_SEARCH_TICKS - 1; t++)
            assertFalse(TunerMath.clicksAt(t, true) && TunerMath.clicksAt(t + 1, true), "failing clicks never rattle");
        assertTrue(TunerMath.clicksAt(0, true));
    }
}

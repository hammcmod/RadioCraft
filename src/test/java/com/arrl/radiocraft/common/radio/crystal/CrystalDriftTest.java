package com.arrl.radiocraft.common.radio.crystal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrystalDriftTest {

    @Test
    void staysWithinTheLimit() {
        for(long t = 0; t < 20_000; t += 7)
            assertTrue(Math.abs(CrystalDrift.offsetHz(t, 12345L, 1000.0D)) <= 1000.0D);
    }

    @Test
    void noLimitMeansNoDrift() {
        assertEquals(0.0D, CrystalDrift.offsetHz(5_000L, 99L, 0.0D));
    }

    @Test
    void wandersBothWaysOverAFewMinutes() {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for(long t = 0; t < 4_000; t += 20) { // About 3 minutes.
            double hz = CrystalDrift.offsetHz(t, 42L, 1000.0D);
            min = Math.min(min, hz);
            max = Math.max(max, hz);
        }
        assertTrue(max > 500.0D, "never drifted far up: " + max);
        assertTrue(min < -500.0D, "never drifted far down: " + min);
    }

    @Test
    void creepsRatherThanJumps() {
        for(long t = 0; t < 10_000; t++) // From one tick to the next it moves by a few Hz at most.
            assertTrue(Math.abs(CrystalDrift.offsetHz(t + 1, 7L, 1000.0D) - CrystalDrift.offsetHz(t, 7L, 1000.0D)) < 5.0D);
    }

    @Test
    void radiosDoNotDriftInStep() {
        assertNotEquals(CrystalDrift.offsetHz(1_000L, 1L, 1000.0D), CrystalDrift.offsetHz(1_000L, 0x5A5A_1234L, 1000.0D));
    }
}

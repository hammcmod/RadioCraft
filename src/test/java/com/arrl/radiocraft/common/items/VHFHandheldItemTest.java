package com.arrl.radiocraft.common.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VHFHandheldItemTest {

    /** Idle on a 30000 FE battery: 30000 / 288000 FE a tick, so a whole battery lasts the 12 days, not 30000 ticks. */
    @Test
    void fractionalDrainAddsUpOverTime() {
        double perTick = 30000.0D / 288000.0D;
        long total = 0;
        for(long t = 0; t < 288000; t++)
            total += VHFHandheldItem.spreadDrain(perTick, t);
        assertEquals(30000, total);
    }

    @Test
    void neverMoreThanOneFeAboveTheRate() {
        for(long t = 0; t < 10000; t++) {
            int drain = VHFHandheldItem.spreadDrain(0.625D, t);
            assertTrue(drain == 0 || drain == 1, "tick " + t + " took " + drain);
        }
        assertEquals(3, VHFHandheldItem.spreadDrain(3.0D, 12345L));
    }
}

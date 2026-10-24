package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiverMixTest {

    @Test
    void fmIsQuietExceptUnderAWeakSignal() {
        assertEquals(0.0D, ReceiverMix.fmHiss(0.0D, 0.15D), 1e-9, "squelch shut: silent");
        assertEquals(0.0D, ReceiverMix.fmHiss(0.8D, 0.15D), 1e-9, "strong signal: full quieting");
        assertTrue(ReceiverMix.fmHiss(0.05D, 0.15D) > ReceiverMix.fmHiss(0.2D, 0.15D), "weaker is noisier");
        assertTrue(ReceiverMix.fmHiss(0.05D, 0.15D) <= 0.15D);
    }

    @Test
    void ssbStaticDucksUnderAStrongSignal() {
        assertEquals(1.0D, ReceiverMix.noiseDuck(0.0D, 0.3D), 1e-9);
        assertTrue(ReceiverMix.noiseDuck(0.9D, 0.3D) < 0.4D);
    }
}

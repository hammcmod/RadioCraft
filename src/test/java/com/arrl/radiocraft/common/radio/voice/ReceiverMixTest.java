package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiverMixTest {

    @Test
    void fmGetsNoisierNotQuieter() {
        assertEquals(0.0D, ReceiverMix.fmHiss(0.8D), 1e-9, "strong signal: full quieting");
        assertTrue(ReceiverMix.fmHiss(0.05D) > ReceiverMix.fmHiss(0.2D), "weaker is noisier");
        assertEquals(0.0D, ReceiverMix.fmDropout(0.2D), 1e-9, "a fair signal holds");
        assertTrue(ReceiverMix.fmDropout(0.08D) > 0.0D && ReceiverMix.fmDropout(0.08D) < 1.0D, "a weak one cuts in and out");
        assertEquals(1.0D, ReceiverMix.fmDropout(0.02D), 1e-9, "at the edge: only static");
    }

    @Test
    void fmFadeKeepsTheVoiceLevel() {
        RadioVoiceProcessor processor = new RadioVoiceProcessor();
        short[] voice = new short[960];
        java.util.Arrays.fill(voice, (short)8000);
        assertEquals(8000, processor.fmFade(voice.clone(), 0.9D, 1.0D)[480], "a strong signal passes untouched");
        short[] edge = processor.fmFade(new short[960], 0.01D, 1.0D);
        long energy = 0;
        for(short sample : edge)
            energy += Math.abs(sample);
        assertTrue(energy > 0, "past the dropouts there is static");
    }

    @Test
    void ssbStaticDucksUnderAStrongSignal() {
        assertEquals(1.0D, ReceiverMix.noiseDuck(0.0D, 0.3D), 1e-9);
        assertTrue(ReceiverMix.noiseDuck(0.9D, 0.3D) < 0.4D);
    }
}

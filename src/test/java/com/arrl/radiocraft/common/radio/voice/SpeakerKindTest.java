package com.arrl.radiocraft.common.radio.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeakerKindTest {

    private static final RadioVoiceProcessor.Settings SSB = RadioVoiceProcessor.Settings.SSB; // 300-2700 Hz.

    private static double width(RadioVoiceProcessor.Settings settings) {
        return settings.highHz() - settings.lowHz();
    }

    @Test
    void speakersNarrowThePassbandByTheirRange() {
        assertEquals(0.8D * width(SSB), width(SpeakerKind.BUILT_IN.apply(SSB)), 1e-9, "a radio's own speaker loses 20%");
        assertEquals(0.9D * width(SSB), width(SpeakerKind.STANDARD.apply(SSB)), 1e-9, "a standard speaker gets 10% of it back");
        assertEquals(width(SSB), width(SpeakerKind.PREMIUM.apply(SSB)), 1e-9, "a premium speaker plays it all");
        assertEquals(0.8D * width(SSB), width(SpeakerKind.WALL.apply(SSB)), 1e-9);
        RadioVoiceProcessor.Settings builtIn = SpeakerKind.BUILT_IN.apply(SSB);
        assertEquals(1500.0D, (builtIn.lowHz() + builtIn.highHz()) / 2.0D, 1e-9, "trimmed evenly from both ends");
    }

    @Test
    void wiredSpeakersDistort() {
        assertTrue(SpeakerKind.WALL.apply(SSB).drive() > SSB.drive());
        assertTrue(SpeakerKind.LOUDSPEAKER.apply(SSB).drive() > SpeakerKind.WALL.apply(SSB).drive());
        assertTrue(SpeakerKind.LOUDSPEAKER.distance > SpeakerKind.WALL.distance, "the loudspeaker carries further");
        assertTrue(SpeakerKind.WALL.isWired() && SpeakerKind.LOUDSPEAKER.isWired() && !SpeakerKind.STANDARD.isWired());
    }

    @Test
    void theBetterSpeakerWins() {
        assertEquals(SpeakerKind.PREMIUM, SpeakerKind.better(SpeakerKind.STANDARD, SpeakerKind.PREMIUM));
        assertEquals(SpeakerKind.STANDARD, SpeakerKind.better(SpeakerKind.BUILT_IN, SpeakerKind.STANDARD));
    }
}

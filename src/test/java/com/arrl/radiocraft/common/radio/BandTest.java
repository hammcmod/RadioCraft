package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default band table should match the real amateur allocations (US limits, in Hz).
 */
class BandTest {

    private static final Map<String, float[]> ALLOCATIONS = Map.of(
            "2m", new float[] { 144_000_000, 148_000_000 },
            "10m", new float[] { 28_000_000, 29_700_000 },
            "20m", new float[] { 14_000_000, 14_350_000 },
            "40m", new float[] { 7_000_000, 7_300_000 },
            "80m", new float[] { 3_500_000, 4_000_000 });

    @Test
    void defaultFrequencyRangesMatchAmateurAllocations() {
        for (Band band : Band.getDefaults()) {
            float[] expected = ALLOCATIONS.get(band.name());
            assertNotNull(expected, "Unexpected band " + band.name());
            assertEquals(expected[0], band.minFrequency(), 0.0F, band.name() + " min");
            assertEquals(expected[1], band.maxFrequency(), 0.0F, band.name() + " max");
        }
        assertEquals(ALLOCATIONS.size(), Band.getDefaults().size());
    }

    @Test
    void defaultBandsDoNotOverlap() {
        var bands = Band.getDefaults();
        for (Band a : bands)
            for (Band b : bands)
                if (!a.name().equals(b.name()))
                    assertTrue(a.maxFrequency() <= b.minFrequency() || b.maxFrequency() <= a.minFrequency(),
                            a.name() + " overlaps " + b.name());
    }
}

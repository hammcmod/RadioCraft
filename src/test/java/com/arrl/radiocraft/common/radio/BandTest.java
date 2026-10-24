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

    /** Every HF band skips by default, 10m included: one long hop beyond the others' reach, day and night. */
    @Test
    void everyHfBandSkipsAndTenMetresReachesFurthest() {
        Band ten = null;
        for (Band band : Band.getDefaults()) {
            if (band.isVhf())
                continue;
            assertTrue(band.maxSkipDay() > band.minSkipDay() && band.maxSkipNight() > band.minSkipNight(), band.name() + " has no skip zone");
            if (band.name().equals("10m"))
                ten = band;
        }
        assertNotNull(ten);
        for (Band band : Band.getDefaults())
            if (!band.isVhf() && band != ten)
                assertTrue(ten.maxSkipDay() >= band.maxSkipDay(), "10m should reach at least as far as " + band.name());
        assertTrue(BandUtils.getBaseStrength(ten, 5000, 1.0, 1.0, true) > 0.5, "10m by day at 5000 blocks");
        assertEquals(0.0, BandUtils.getBaseStrength(ten, 1500, 1.0, 1.0, true), 1e-9, "1500 blocks is in 10m's skip zone (silent)");
    }
}

package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.Band;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntennaAnalysisTest {

    private static final List<Band> BANDS = Band.getDefaults().stream()
            .sorted((a, b) -> Float.compare(a.minFrequency(), b.minFrequency())).toList();

    /** An antenna resonant at {@code hertz}: SWR 1 there, climbing 1 for every 2% away. */
    private static DoubleUnaryOperator resonantAt(double hertz) {
        return f -> 1.0D + Math.abs(f - hertz) / hertz * 50.0D;
    }

    @Test
    void resonantInsideABand() {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(resonantAt(14_175_000.0D), BANDS);
        assertEquals(AntennaAnalysis.Verdict.RESONANT, result.verdict());
        assertEquals("20m", result.best().name());
    }

    @Test
    void resonantBelowABandIsTooLong() {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(resonantAt(13_400_000.0D), BANDS);
        assertEquals(AntennaAnalysis.Verdict.TOO_LONG, result.verdict());
        assertEquals("20m", result.best().name());
    }

    @Test
    void resonantAboveABandIsTooShort() {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(resonantAt(7_800_000.0D), BANDS);
        assertEquals(AntennaAnalysis.Verdict.TOO_SHORT, result.verdict());
        assertEquals("40m", result.best().name());
    }

    @Test
    void flatHighSwrInABandNeedsATuner() {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(f -> f > 7e6 && f < 7.3e6 ? 2.5D : 20.0D, BANDS);
        assertEquals(AntennaAnalysis.Verdict.POOR_MATCH, result.verdict());
    }

    @Test
    void nothingUsable() {
        AntennaAnalysis.Result result = AntennaAnalysis.analyze(f -> 50.0D, BANDS);
        assertEquals(AntennaAnalysis.Verdict.NONE, result.verdict());
        assertEquals(BANDS.size(), result.bands().size());
    }
}

package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.data.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EndFedAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.InvertedVAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.RandomWireAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.InvertedVAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.RandomWireAntennaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SWR is 1.0 when each element is a quarter wavelength (rounded to whole blocks) and rises 0.5 per block of error.
 */
class AntennaSwrTest {

    private static final double EPS = 1e-9;
    private static final float TWENTY_M = 14_000_000; // quarter wave ~5.35 -> 5 blocks
    private static final float FORTY_M = 7_300_000;   // quarter wave ~10.27 -> 10 blocks

    @Test
    void perfectDipoleHasUnitySwr() {
        DipoleAntennaType type = new DipoleAntennaType();
        assertEquals(1.0, type.getSWR(new DipoleAntennaData(5, 5), TWENTY_M), EPS);
        assertEquals(1.0, type.getSWR(new DipoleAntennaData(10, 10), FORTY_M), EPS);
    }

    @Test
    void dipoleSwrRisesHalfPerBlockOfError() {
        DipoleAntennaType type = new DipoleAntennaType();
        assertEquals(2.0, type.getSWR(new DipoleAntennaData(4, 4), TWENTY_M), EPS);
        assertEquals(3.0, type.getSWR(new DipoleAntennaData(3, 7), TWENTY_M), EPS);
    }

    @Test
    void endFedUsesSingleElementLength() {
        EndFedAntennaType type = new EndFedAntennaType();
        assertEquals(1.0, type.getSWR(new EndFedAntennaData(5), TWENTY_M), EPS);
        assertEquals(1.5, type.getSWR(new EndFedAntennaData(6), TWENTY_M), EPS);
    }

    @Test
    void invertedVWantsArmsFivePercentShorter() {
        InvertedVAntennaType type = new InvertedVAntennaType();
        // 20m: quarter wave 5.35 * 0.95 = 5.08 -> 5 blocks, same as the dipole.
        // 80m at 3.8 MHz: quarter wave 19.72 (a dipole wants 20) * 0.95 = 18.74 -> 19, one block less per leg.
        assertEquals(5, InvertedVAntennaType.desiredArmLength(TWENTY_M));
        assertEquals(19, InvertedVAntennaType.desiredArmLength(3_800_000));
        assertEquals(1.0, type.getSWR(new InvertedVAntennaData(5, 5, 3, 3), TWENTY_M), EPS);
        assertEquals(1.5, type.getSWR(new InvertedVAntennaData(5, 6, 3, 3), TWENTY_M), EPS);
        // Arm length is measured along the wire, so a 4 across / 3 down arm is 5 long and matched.
        assertEquals(1.0, type.getSWR(new InvertedVAntennaData(Math.sqrt(16 + 9), 5, 3, 3), TWENTY_M), EPS);
    }

    @Test
    void invertedVPenaltyIsWorstOn40m() {
        assertEquals(0.75, InvertedVAntennaType.transmitPenalty(band("40m", 7_000_000, 7_300_000)), EPS);
        assertEquals(0.9, InvertedVAntennaType.transmitPenalty(band("20m", 14_000_000, 14_350_000)), EPS);
        assertEquals(1.0, InvertedVAntennaType.transmitPenalty(null), EPS);
    }

    @Test
    void randomWireIsNeverMatchedAndNeedsHalfAWave() {
        float f = 14_000_000; // half wave 10.71 blocks
        assertEquals(SWRHelper.OVERDRAW_SWR, RandomWireAntennaType.swrForLength(10.0, f), EPS); // Too short.
        assertEquals(RandomWireAntennaType.BASE_SWR, RandomWireAntennaType.swrForLength(299792458.0 / f / 2.0, f), EPS); // Exactly a half wave.
        assertEquals(RandomWireAntennaType.BASE_SWR + 0.5 * (12.0 - 299792458.0 / f / 2.0), RandomWireAntennaType.swrForLength(12.0, f), EPS);
        // 21 blocks is nearest two half waves (21.41): off by 0.41.
        assertEquals(RandomWireAntennaType.BASE_SWR + 0.5 * (299792458.0 / f - 21.0), RandomWireAntennaType.swrForLength(21.0, f), EPS);
        RandomWireAntennaType type = new RandomWireAntennaType();
        assertEquals(RandomWireAntennaType.swrForLength(30.0, f), type.getSWR(new RandomWireAntennaData(30.0, 3), f), EPS);
    }

    private static Band band(String name, float min, float max) {
        return new Band(name, 300, 0, 0, 0, 0, min, max);
    }

    @Test
    void efficiencyMultiplierFromSwr() {
        assertEquals(1.0, SWRHelper.getEfficiencyMultiplier(1.0), EPS);
        assertEquals(8.0 / 9.0, SWRHelper.getEfficiencyMultiplier(2.0), EPS);
        assertEquals(0.75, SWRHelper.getEfficiencyMultiplier(3.0), EPS);
    }
}

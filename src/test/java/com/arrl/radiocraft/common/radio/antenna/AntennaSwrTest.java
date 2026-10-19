package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.data.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EndFedAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType;
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
    void efficiencyMultiplierFromSwr() {
        assertEquals(1.0, SWRHelper.getEfficiencyMultiplier(1.0), EPS);
        assertEquals(8.0 / 9.0, SWRHelper.getEfficiencyMultiplier(2.0), EPS);
        assertEquals(0.75, SWRHelper.getEfficiencyMultiplier(3.0), EPS);
    }
}

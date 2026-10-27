package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.antenna.types.vhf.CommercialVHFAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.JPoleAntennaType;
import com.arrl.radiocraft.common.radio.rfi.AntennaStrikes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Ringo and the folded dipole: more gain than a J-pole, and grounded against lightning. */
class CommercialVHFAntennaTypeTest {

    private static final double EPS = 1e-9;

    private static CommercialVHFAntennaType type(double gain) {
        return new CommercialVHFAntennaType("test", () -> null, gain);
    }

    @Test
    void gainIsOverAJPole() {
        JPoleAntennaType jPole = new JPoleAntennaType();
        CommercialVHFAntennaType ringo = type(CommercialVHFAntennaType.RINGO_GAIN);
        CommercialVHFAntennaType dipole = type(CommercialVHFAntennaType.FOLDED_DIPOLE_GAIN);
        assertEquals(jPole.getTransmitGainLinear() * 1.10D, ringo.getTransmitGainLinear(), EPS);
        assertEquals(jPole.getReceiveGainLinear() * 1.10D, ringo.getReceiveGainLinear(), EPS);
        assertEquals(jPole.getTransmitGainLinear() * 1.25D, dipole.getTransmitGainLinear(), EPS);
        assertEquals(jPole.getReceiveGainLinear() * 1.25D, dipole.getReceiveGainLinear(), EPS);
    }

    @Test
    void groundedAntennasTakeAQuarterOfTheStrikes() {
        assertEquals(1.0D, new JPoleAntennaType().lightningDamageFactor(), EPS);
        assertEquals(0.25D, type(CommercialVHFAntennaType.RINGO_GAIN).lightningDamageFactor(), EPS);
        assertEquals(0.25D, type(CommercialVHFAntennaType.FOLDED_DIPOLE_GAIN).lightningDamageFactor(), EPS);
        int damaged = 0;
        for(int i = 0; i < 1000; i++)
            if(AntennaStrikes.damages(0.25D, (i + 0.5D) / 1000.0D))
                damaged++;
        assertEquals(250, damaged, "a quarter of evenly spread rolls do damage");
        assertTrue(AntennaStrikes.damages(1.0D, 0.999D), "an ordinary antenna is always damaged");
        assertFalse(AntennaStrikes.damages(0.25D, 0.25D));
    }
}

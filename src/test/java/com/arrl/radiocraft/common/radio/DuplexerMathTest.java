package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The duplexer's cavities: free when tuned to the signal, lossy and mismatched the further off they are. */
class DuplexerMathTest {

    private static final double EPS = 1e-9;
    private static final int TX = 146_940_000, RX = 146_340_000;

    private static int[] tuned() {
        return new int[]{TX, TX, TX, RX, RX, RX};
    }

    @Test
    void tunedCavitiesPassEverythingAndShowNoMismatch() {
        int[] screws = tuned();
        assertEquals(1.0, DuplexerMath.pass(screws, DuplexerMath.TX_FIRST, TX), EPS);
        assertEquals(1.0, DuplexerMath.pass(screws, DuplexerMath.RX_FIRST, RX), EPS);
        assertEquals(1.0, DuplexerMath.swr(1.0), EPS);
        assertEquals(1.0, DuplexerMath.pass(new int[6], DuplexerMath.TX_FIRST, TX), EPS); // Never tuned.
    }

    @Test
    void aCavityOneHalfWidthOffPassesHalf() {
        int[] screws = tuned();
        screws[1] = TX + (int)DuplexerMath.CAVITY_HALF_WIDTH_HZ;
        assertEquals(0.5, DuplexerMath.pass(screws, DuplexerMath.TX_FIRST, TX), EPS);
        assertEquals(1.0, DuplexerMath.pass(screws, DuplexerMath.RX_FIRST, RX), EPS); // The other side is untouched.
        double swr = DuplexerMath.swr(0.5);
        assertTrue(swr > 5.0 && swr < 6.0, "half reflected is about SWR 5.8: " + swr);
    }

    @Test
    void furtherOffIsWorse() {
        int[] near = tuned(), far = tuned();
        near[4] = RX + DuplexerMath.SCREW_STEP_HZ;
        far[4] = RX + 4 * DuplexerMath.SCREW_STEP_HZ;
        assertTrue(DuplexerMath.pass(near, DuplexerMath.RX_FIRST, RX) > DuplexerMath.pass(far, DuplexerMath.RX_FIRST, RX));
    }

    @Test
    void mismatchesCombineAndAreCapped() {
        assertEquals(3.0, DuplexerMath.combine(1.5, 2.0), EPS);
        assertEquals(SWRHelper.OVERDRAW_SWR, DuplexerMath.combine(5.0, 5.0), EPS);
        assertEquals(0.0, DuplexerMath.combine(0.0, 3.0), EPS);
        assertEquals(SWRHelper.OVERDRAW_SWR, DuplexerMath.swr(0.0), EPS);
    }
}

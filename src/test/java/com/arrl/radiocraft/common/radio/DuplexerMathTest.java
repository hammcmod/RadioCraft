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

    @Test
    void tunedDuplexerDoesNotDesense() {
        assertTrue(DuplexerMath.desense(tuned(), RX, TX) > 0.98, "tuned (about 1% at 600 kHz split): " + DuplexerMath.desense(tuned(), RX, TX));
        assertEquals(1.0, DuplexerMath.desense(new int[6], RX, TX), EPS); // Never tuned.
    }

    @Test
    void cavityOnTheOtherFrequencyDeafensTheReceiver() {
        int[] txOnInput = tuned();
        txOnInput[0] = RX; // Transmitter noise on the input frequency gets out.
        int[] rxOnOutput = tuned();
        rxOnOutput[5] = TX; // The transmitter's carrier gets into the receiver.
        assertTrue(DuplexerMath.desense(txOnInput, RX, TX) < 0.5, "TX cavity on the input: " + DuplexerMath.desense(txOnInput, RX, TX));
        assertTrue(DuplexerMath.desense(rxOnOutput, RX, TX) < 0.5, "RX cavity on the output: " + DuplexerMath.desense(rxOnOutput, RX, TX));
        int[] slightlyOff = tuned();
        slightlyOff[0] = TX - 50_000; // Off its own frequency, but still far from the input.
        assertTrue(DuplexerMath.desense(slightlyOff, RX, TX) > 0.95, "a little off: " + DuplexerMath.desense(slightlyOff, RX, TX));
    }
}

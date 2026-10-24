package com.arrl.radiocraft.common.radio;

/**
 * A duplexer's six cavities: three on the transmit side, three on the receive side, each tuned by a screw to a
 * frequency. A signal passes a cavity tuned to it untouched and loses more the further the cavity is off, so a
 * well tuned duplexer costs nothing and a badly tuned one loses signal both ways and shows the transmitter a
 * mismatch. Pure functions for unit tests.
 */
public class DuplexerMath {

    /** Screws 0 to 2 tune the transmit cavities, 3 to 5 the receive cavities. */
    public static final int SCREWS = 6;
    public static final int TX_FIRST = 0;
    public static final int RX_FIRST = 3;
    /** Hz a cavity can be off before it passes only half the power. */
    public static final double CAVITY_HALF_WIDTH_HZ = 25_000.0D;
    /** Hz one notch of a screw moves its cavity. */
    public static final int SCREW_STEP_HZ = 5_000;

    /**
     * Share of the power at {@code signalHz} that gets through the three cavities starting at {@code first}. A screw
     * set to 0 has never been tuned and passes everything (a duplexer that has not met its repeater yet).
     */
    public static double pass(int[] screwsHz, int first, double signalHz) {
        double pass = 1.0D;
        for(int i = first; i < first + 3; i++) {
            if(screwsHz[i] <= 0)
                continue;
            double off = (signalHz - screwsHz[i]) / CAVITY_HALF_WIDTH_HZ;
            pass *= 1.0D / (1.0D + off * off);
        }
        return pass;
    }

    /** The SWR a transmitter sees from cavities passing {@code pass} of its power (the rest reflected), at most {@link SWRHelper#OVERDRAW_SWR}. */
    public static double swr(double pass) {
        double rho = Math.sqrt(Math.max(0.0D, Math.min(1.0D, 1.0D - pass)));
        if(rho >= 1.0D)
            return SWRHelper.OVERDRAW_SWR;
        return Math.min(SWRHelper.OVERDRAW_SWR, (1.0D + rho) / (1.0D - rho));
    }

    /**
     * Two mismatches in a row: the antenna's SWR through the duplexer's. 0 (no antenna) stays 0; at most
     * {@link SWRHelper#OVERDRAW_SWR}.
     */
    public static double combine(double antennaSwr, double duplexerSwr) {
        if(antennaSwr <= 0.0D)
            return antennaSwr;
        return Math.min(SWRHelper.OVERDRAW_SWR, antennaSwr * duplexerSwr);
    }

}

package com.arrl.radiocraft.common.radio.microwave;

import com.arrl.radiocraft.common.radio.terrain.TerrainMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicrowaveMathTest {

    private static MicrowaveMath.End dish(double x, double z, double azimuth) {
        return new MicrowaveMath.End(x, 64, z, azimuth, 0.0D, 1500.0D);
    }

    @Test
    void rangeIsTheGeometricMeanLessTheBlackout() {
        assertEquals(1500.0D, MicrowaveMath.linkRange(1500, 1500, 0), 1e-9);
        assertEquals(9000.0D, MicrowaveMath.linkRange(9000, 9000, 0), 1e-9);
        assertEquals(Math.sqrt(1500.0D * 9000.0D), MicrowaveMath.linkRange(1500, 9000, 0), 1e-9);
        assertEquals(1480.0D, MicrowaveMath.linkRange(1500, 1500, 20), 1e-9);
    }

    @Test
    void dishesMustRoughlyFaceEachOther() {
        MicrowaveMath.End west = dish(0, 0, 90), east = dish(1000, 0, 270);
        assertEquals(MicrowaveMath.Result.OK, MicrowaveMath.check(null, west, east, 30, 0));
        assertEquals(MicrowaveMath.Result.OK, MicrowaveMath.check(null, dish(0, 0, 115), east, 30, 0), "25 degrees off still links");
        assertEquals(MicrowaveMath.Result.NOT_FACING, MicrowaveMath.check(null, dish(0, 0, 190), east, 30, 0), "100 degrees off hears nothing");
        assertEquals(MicrowaveMath.Result.NOT_FACING, MicrowaveMath.check(null, dish(0, 0, 270), east, 30, 0), "pointing away hears nothing");
        assertEquals(MicrowaveMath.Result.OUT_OF_RANGE, MicrowaveMath.check(null, west, dish(2000, 0, 270), 30, 0));
    }

    @Test
    void pointingLossFollowsTheMainLobeDownToTheSidelobes() {
        assertEquals(0.0D, MicrowaveMath.pointingLoss(0, 30), 1e-9);
        assertEquals(3.0D, MicrowaveMath.pointingLoss(15, 30), 1e-9, "3 dB at half the beam width");
        assertEquals(12.0D, MicrowaveMath.pointingLoss(30, 30), 1e-9, "12 dB at the full width");
        assertEquals(MicrowaveMath.SIDELOBE_LOSS, MicrowaveMath.pointingLoss(80, 30), 1e-9, "the sidelobes, no lower");
    }

    @Test
    void roughAimShowsSomethingToPeak() {
        MicrowaveMath.Signal rough = MicrowaveMath.signal(null, dish(0, 0, 170), dish(1000, 0, 190), 30, 0); // 80 degrees off at each end.
        assertEquals(MicrowaveMath.Result.WEAK, rough.result());
        assertTrue(rough.rssi() > MicrowaveMath.NO_SIGNAL_DBM, "a roughly aimed pair reads something: " + rough.rssi());
        assertEquals(MicrowaveMath.FLOOR_DBM, rough.rssi(), 1e-9);
        double oneEnd = MicrowaveMath.signal(null, dish(0, 0, 90), dish(1000, 0, 190), 30, 0).rssi();
        assertTrue(oneEnd > rough.rssi(), "aligning one end raises it: " + oneEnd);
    }

    @Test
    void shortPathsAreForgiving() {
        MicrowaveMath.End here = dish(0, 0, 90), there = dish(13, 0, 270 + 80); // Thirteen blocks, the far dish 80 degrees off.
        MicrowaveMath.Signal signal = MicrowaveMath.signal(null, here, there, 30, 0);
        assertEquals(MicrowaveMath.Result.OK, signal.result(), "rssi " + signal.rssi());
        MicrowaveMath.Signal both = MicrowaveMath.signal(null, dish(0, 0, 90 + 60), there, 30, 0);
        assertTrue(both.rssi() > MicrowaveMath.FLOOR_DBM, "both ends rough still reads: " + both.rssi());
    }

    @Test
    void elevationCounts() {
        MicrowaveMath.End low = new MicrowaveMath.End(0, 64, 0, 90, 0, 1500), high = new MicrowaveMath.End(100, 164, 0, 270, 0, 1500);
        assertTrue(MicrowaveMath.offAxis(low, 100, 164, 0) > 40.0D, "a dish 100 up at 100 along is 45 degrees above the beam");
        assertEquals(MicrowaveMath.Result.WEAK, MicrowaveMath.check(null, low, high, 30, 0), "45 degrees off at both ends is too weak to decode");
        double[] aim = MicrowaveMath.aimAt(low, 100, 164, 0);
        MicrowaveMath.End tilted = new MicrowaveMath.End(0, 64, 0, aim[0], aim[1], 1500);
        MicrowaveMath.End tiltedDown = new MicrowaveMath.End(100, 164, 0, 270, -aim[1], 1500);
        assertEquals(MicrowaveMath.Result.OK, MicrowaveMath.check(null, tilted, tiltedDown, 30, 0));
    }

    @Test
    void terrainMustStayBelowTheLine() {
        TerrainMath.HeightSource flat = (x, z) -> 60;
        TerrainMath.HeightSource hill = (x, z) -> x > 400 && x < 600 ? 80 : 60;
        TerrainMath.HeightSource unknown = (x, z) -> TerrainMath.UNKNOWN;
        MicrowaveMath.End west = dish(0, 0, 90), east = dish(1000, 0, 270);
        assertTrue(MicrowaveMath.clearPath(flat, west, east));
        assertFalse(MicrowaveMath.clearPath(hill, west, east));
        assertTrue(MicrowaveMath.clearPath(unknown, west, east), "unexplored ground counts as clear");
        assertEquals(MicrowaveMath.Result.BLOCKED, MicrowaveMath.check(hill, west, east, 30, 0));
    }

    @Test
    void aligningPeaksTheSignal() {
        MicrowaveMath.End east = dish(1000, 0, 270);
        double deadOn = MicrowaveMath.signal(null, dish(0, 0, 90), east, 45, 0).rssi();
        double tenOff = MicrowaveMath.signal(null, dish(0, 0, 100), east, 45, 0).rssi();
        double thirtyOff = MicrowaveMath.signal(null, dish(0, 0, 120), east, 45, 0).rssi();
        assertTrue(deadOn > tenOff && tenOff > thirtyOff, "the signal falls off as the dish turns away: " + deadOn + " " + tenOff + " " + thirtyOff);
        assertTrue(deadOn > MicrowaveMath.GOOD_DBM, "dead on at two thirds of range is a good link: " + deadOn);
        assertEquals(MicrowaveMath.Result.OK, MicrowaveMath.signal(null, dish(0, 0, 120), east, 45, 0).result(), "30 degrees off still holds, weakly");
    }

    @Test
    void farAndOffAxisIsTooWeak() {
        MicrowaveMath.End west = dish(0, 0, 140), east = dish(1450, 0, 220); // 50 degrees off at each end, near the edge of range.
        MicrowaveMath.Signal signal = MicrowaveMath.signal(null, west, east, 45, 0);
        assertEquals(MicrowaveMath.Result.WEAK, signal.result(), "rssi " + signal.rssi());
        assertTrue(MicrowaveMath.distanceLevel(0, 1500) > MicrowaveMath.distanceLevel(1500, 1500));
        assertEquals(MicrowaveMath.EDGE_DBM, MicrowaveMath.distanceLevel(1500, 1500), 1e-9);
    }

    @Test
    void frequencies() {
        assertTrue(MicrowaveMath.interferes(6175, 6200, 30));
        assertFalse(MicrowaveMath.interferes(6175, 6215, 30));
        assertEquals(MicrowaveMath.MAX_MHZ, MicrowaveMath.tune(7120, 3));
        assertEquals(6185, MicrowaveMath.tune(6175, 1));
        assertEquals("00FF", MicrowaveMath.hex(255));
    }
}

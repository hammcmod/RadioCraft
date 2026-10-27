package com.arrl.radiocraft.common.radio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DirectionFindingTest {

    @Test
    void bearingsAreCompassDegrees() {
        assertEquals(0.0D, DirectionFinding.bearing(0, 0, 0, -100), 1e-9); // North is -Z.
        assertEquals(90.0D, DirectionFinding.bearing(0, 0, 100, 0), 1e-9);
        assertEquals(180.0D, DirectionFinding.bearing(0, 0, 0, 100), 1e-9);
        assertEquals(270.0D, DirectionFinding.bearing(0, 0, -100, 0), 1e-9);
        assertEquals(45.0D, DirectionFinding.bearing(0, 0, 100, -100), 1e-9);
    }

    @Test
    void differenceWrapsRoundNorth() {
        assertEquals(20.0D, DirectionFinding.difference(350.0D, 10.0D), 1e-9);
        assertEquals(180.0D, DirectionFinding.difference(0.0D, 180.0D), 1e-9);
        assertEquals(0.0D, DirectionFinding.normalize(360.0D), 1e-9);
        assertEquals(350.0D, DirectionFinding.normalize(-10.0D), 1e-9);
    }

    @Test
    void moreModulesGiveBetterBearings() {
        assertTrue(Double.isInfinite(DirectionFinding.sigma(2, false, false)), "fewer than three give no bearing");
        double previous = Double.POSITIVE_INFINITY;
        for(int locked = DirectionFinding.MIN_MODULES; locked <= DirectionFinding.MAX_MODULES; locked++) {
            double sigma = DirectionFinding.sigma(locked, false, false);
            assertTrue(sigma < previous, locked + " modules should beat " + (locked - 1));
            previous = sigma;
        }
        assertEquals(DirectionFinding.sigma(8, false, false), DirectionFinding.sigma(12, false, false), 1e-9, "eight is the most that count");
        assertTrue(DirectionFinding.sigma(5, true, false) > DirectionFinding.sigma(5, false, false), "skip smears the bearing");
        assertTrue(DirectionFinding.sigma(5, false, true) > DirectionFinding.sigma(5, false, false), "a weak signal is harder");
    }

    @Test
    void twoGoodBearingsCrossAtTheTransmitter() {
        double tx = 300.0D, tz = -200.0D;
        DirectionFinding.Line a = new DirectionFinding.Line(0, 0, DirectionFinding.bearing(0, 0, tx, tz), 2.0D);
        DirectionFinding.Line b = new DirectionFinding.Line(400, 100, DirectionFinding.bearing(400, 100, tx, tz), 2.0D);
        DirectionFinding.Fix fix = DirectionFinding.triangulate(List.of(a, b));
        assertTrue(fix.found(), fix.problem().toString());
        assertEquals(tx, fix.x(), 1e-6);
        assertEquals(tz, fix.z(), 1e-6);
        assertTrue(fix.radius() > 0.0D && fix.radius() < 100.0D, "box " + fix.radius());
    }

    @Test
    void threeNoisyBearingsLandNearTheTransmitter() {
        double tx = -1200.0D, tz = 800.0D;
        List<DirectionFinding.Line> lines = List.of(
                new DirectionFinding.Line(0, 0, DirectionFinding.bearing(0, 0, tx, tz) + 2.0D, 3.0D),
                new DirectionFinding.Line(-600, -300, DirectionFinding.bearing(-600, -300, tx, tz) - 1.5D, 3.0D),
                new DirectionFinding.Line(-200, 1200, DirectionFinding.bearing(-200, 1200, tx, tz) + 1.0D, 3.0D));
        DirectionFinding.Fix fix = DirectionFinding.triangulate(lines);
        assertTrue(fix.found(), fix.problem().toString());
        double miss = Math.hypot(fix.x() - tx, fix.z() - tz);
        assertTrue(miss < fix.radius(), "missed by " + miss + " with a box of " + fix.radius());
    }

    @Test
    void badGeometryGivesNoFix() {
        assertEquals(DirectionFinding.Problem.TOO_FEW, DirectionFinding.triangulate(List.of(new DirectionFinding.Line(0, 0, 10, 2))).problem());
        assertEquals(DirectionFinding.Problem.TOO_CLOSE, DirectionFinding.triangulate(List.of(
                new DirectionFinding.Line(0, 0, 10, 2), new DirectionFinding.Line(10, 0, 80, 2))).problem());
        assertEquals(DirectionFinding.Problem.TOO_PARALLEL, DirectionFinding.triangulate(List.of(
                new DirectionFinding.Line(0, 0, 90, 2), new DirectionFinding.Line(0, 200, 95, 2))).problem());
        assertEquals(DirectionFinding.Problem.DIVERGE, DirectionFinding.triangulate(List.of( // Pointing away from each other.
                new DirectionFinding.Line(0, 0, 270, 2), new DirectionFinding.Line(200, 0, 45, 2))).problem());
    }
}

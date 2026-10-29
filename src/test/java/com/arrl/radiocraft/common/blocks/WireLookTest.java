package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blocks.WireLook.End;
import com.arrl.radiocraft.common.blocks.WireLook.Look;
import com.arrl.radiocraft.common.blocks.WireLook.Part;
import com.arrl.radiocraft.common.blocks.WireLook.Placed;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static net.minecraft.core.Direction.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The cable pieces {@link WireLook#parts} picks for the layouts the maintainer checked by eye on 2026-10-21. */
class WireLookTest {

    private static Set<Direction> links(Direction... dirs) {
        return dirs.length == 0 ? EnumSet.noneOf(Direction.class) : EnumSet.of(dirs[0], dirs);
    }

    private static Placed p(Part part, Direction dir) {
        return new Placed(part, dir);
    }

    @Test
    void straightRunOnTheFloorIsMiddleAndTwoStubs() {
        assertEquals(List.of(p(Part.MIDDLE, null), p(Part.HORIZONTAL, NORTH), p(Part.HORIZONTAL, SOUTH)),
                WireLook.parts(Look.NONE, links(NORTH, SOUTH)));
    }

    @Test
    void loneWireIsANub() {
        assertEquals(List.of(p(Part.MIDDLE, null)), WireLook.parts(Look.NONE, links()));
    }

    @Test
    void columnUpTheMiddleWithNoWall() {
        assertEquals(List.of(p(Part.MIDDLE, null), p(Part.HORIZONTAL, EAST), p(Part.VERTICAL, null)),
                WireLook.parts(Look.NONE, links(EAST, UP)));
    }

    @Test
    void columnPieceBesideAPostIsOneFullEdgeRunAndNothingElse() {
        // Arrives on the north edge from below and carries straight on up it: no middle, no stubs.
        assertEquals(List.of(p(Part.EDGE_FULL, NORTH)), WireLook.parts(new Look(NORTH, NORTH, End.NONE, false), links(UP, DOWN)));
    }

    @Test
    void footOfAColumnTurnsFromItsFloorRunToTheWall() {
        assertEquals(List.of(p(Part.MIDDLE, null), p(Part.HORIZONTAL, NORTH), p(Part.HORIZONTAL, SOUTH), p(Part.EDGE, NORTH)),
                WireLook.parts(new Look(NORTH, null, End.NONE, false), links(SOUTH, UP)));
    }

    @Test
    void runArrivingOnAnEdgeAndLeavingThatWayIsJustACorner() {
        assertEquals(List.of(p(Part.CORNER, WEST)), WireLook.parts(new Look(null, WEST, End.NONE, false), links(WEST, DOWN)));
    }

    @Test
    void climbIntoADeviceDrawsTheOverhang() {
        assertEquals(List.of(p(Part.MIDDLE, null), p(Part.HORIZONTAL, EAST), p(Part.HORIZONTAL, WEST), p(Part.EDGE, EAST), p(Part.CLIMB_OVERHANG, EAST)),
                WireLook.parts(new Look(EAST, null, End.CLIMB_DEVICE, false), links(WEST)));
    }

    @Test
    void trenchFloorClimbsBothWalls() {
        assertEquals(List.of(p(Part.MIDDLE, null), p(Part.HORIZONTAL, EAST), p(Part.HORIZONTAL, WEST), p(Part.EDGE, EAST), p(Part.EDGE, WEST)),
                WireLook.parts(new Look(EAST, null, End.CLIMB_WIRE, true), links()));
    }

    @Test
    void runUpAnEdgeIntoADeviceAboveTurnsIn() {
        assertEquals(List.of(p(Part.EDGE_FULL, SOUTH), p(Part.UP_OVERHANG, SOUTH)),
                WireLook.parts(new Look(SOUTH, SOUTH, End.UP_DEVICE, false), links(UP, DOWN)));
    }

    @Test
    void rotationsFollowTheBlockstateConvention() {
        assertEquals(0, WireLook.rotation(NORTH));
        assertEquals(90, WireLook.rotation(EAST));
        assertEquals(180, WireLook.rotation(SOUTH));
        assertEquals(270, WireLook.rotation(WEST));
        assertEquals(0, WireLook.rotation(null));
    }

    private static void assertClose(double expected, double actual) {
        assertEquals(expected, actual, 1.0E-9D);
    }

    /** A strand two pixels right of the middle: its stubs keep their face ends and meet the moved middle. */
    @Test
    void bundleOffsetMovesTheMiddleAndStretchesStubsToTheFaces() {
        assertEquals(WireLook.Affine.NONE, WireLook.affine(p(Part.HORIZONTAL, NORTH), 0));
        WireLook.Affine middle = WireLook.affine(p(Part.MIDDLE, null), 2);
        assertClose(9.0D / 16.0D, middle.x(7.0D / 16.0D)); // The middle cube 7..9 moves to 9..11, both ways.
        assertClose(9.0D / 16.0D, middle.z(7.0D / 16.0D));
        WireLook.Affine north = WireLook.affine(p(Part.HORIZONTAL, NORTH), 2);
        assertClose(9.0D / 16.0D, north.x(7.0D / 16.0D)); // Sideways with the middle...
        assertClose(0.0D, north.z(0.0D)); // ...still on the north face...
        assertClose(9.0D / 16.0D, north.z(7.0D / 16.0D)); // ...and its inner end at the moved middle.
        WireLook.Affine east = WireLook.affine(p(Part.HORIZONTAL, EAST), 2);
        assertClose(1.0D, east.x(1.0D));
        assertClose(11.0D / 16.0D, east.x(9.0D / 16.0D));
        assertClose(9.0D / 16.0D, east.z(7.0D / 16.0D));
        WireLook.Affine south = WireLook.affine(p(Part.HORIZONTAL, SOUTH), -2);
        assertClose(1.0D, south.z(1.0D));
        assertClose(7.0D / 16.0D, south.z(9.0D / 16.0D));
        assertClose(5.0D / 16.0D, south.x(7.0D / 16.0D));
        WireLook.Affine west = WireLook.affine(p(Part.HORIZONTAL, WEST), -2);
        assertClose(0.0D, west.x(0.0D));
        assertClose(5.0D / 16.0D, west.x(7.0D / 16.0D));
    }

    /** Pieces on an edge slide along their wall and stay against it. */
    @Test
    void bundleOffsetSlidesEdgePiecesAlongTheWall() {
        WireLook.Affine northWall = WireLook.affine(p(Part.EDGE, NORTH), 4);
        assertClose(11.0D / 16.0D, northWall.x(7.0D / 16.0D));
        assertClose(-1.0D / 16.0D, northWall.z(-1.0D / 16.0D)); // Not pushed into or off the wall.
        WireLook.Affine eastWall = WireLook.affine(p(Part.CORNER, EAST), 4);
        assertClose(15.0D / 16.0D, eastWall.x(15.0D / 16.0D));
        assertClose(11.0D / 16.0D, eastWall.z(7.0D / 16.0D));
    }

    /** A strand 2 px right of the middle leaving north for a lone wire (at 0): its stub meets the face at 1 px, slanting from 2 px at the middle. */
    @Test
    void stubSlantsToMeetTheNextBlock() {
        WireLook.Faces faces = new WireLook.Faces(2, 2, 1, 2, 2, 2); // Halfway to the lone wire to the north; the rest at our offset.
        WireLook.Affine north = WireLook.affine(p(Part.HORIZONTAL, NORTH), 2, faces, false);
        assertClose(9.0D / 16.0D, north.x(8.0D / 16.0D, 0, 0)); // At the face: 1 px over.
        assertClose(10.0D / 16.0D, north.x(8.0D / 16.0D, 0, 7.0D / 16.0D)); // At the middle: our 2 px.
        assertClose(0.0D, north.z(8.0D / 16.0D, 0, 0.0D)); // Still on the face.
        assertClose(9.0D / 16.0D, north.z(8.0D / 16.0D, 0, 7.0D / 16.0D)); // Inner end at the moved middle.
        WireLook.Affine east = WireLook.affine(p(Part.HORIZONTAL, EAST), 2, new WireLook.Faces(2, 2, 2, 2, 2, 0), false);
        assertClose(8.0D / 16.0D, east.z(1.0D, 0, 8.0D / 16.0D)); // Meets a device to the east dead centre.
        assertClose(10.0D / 16.0D, east.z(9.0D / 16.0D, 0, 8.0D / 16.0D)); // Our 2 px at the inner end.
    }

    /** A run up a wall slides along it from our offset at the foot to the meeting point with the block above. */
    @Test
    void wallRunSlidesToMeetTheBlockAbove() {
        WireLook.Affine edge = WireLook.affine(p(Part.EDGE, NORTH), 4, new WireLook.Faces(4, 1, 4, 4, 4, 4), false);
        assertClose(12.0D / 16.0D, edge.x(8.0D / 16.0D, 0.0D, 0.0D)); // Foot: our 4 px.
        assertClose(9.0D / 16.0D, edge.x(8.0D / 16.0D, 1.0D, 0.0D)); // Top: 1 px, halfway to the lone wire above.
        WireLook.Affine hanging = WireLook.affine(p(Part.EDGE, NORTH), 4, new WireLook.Faces(1, 4, 4, 4, 4, 4), true);
        assertClose(12.0D / 16.0D, hanging.x(8.0D / 16.0D, 1.0D, 0.0D)); // Hanging: the foot is at the ceiling...
        assertClose(9.0D / 16.0D, hanging.x(8.0D / 16.0D, 0.0D, 0.0D)); // ...and the run goes down to meet the block below.
    }

    /** Strands sit side by side with a pixel between, centred; a lone cable at 0; too many fat ones give up their gaps. */
    @Test
    void bundleLayoutSpacesMixedWidths() {
        assertEquals(0.0D, WireLook.layout(new double[] { 1 })[0], 1e-9);
        double[] two = WireLook.layout(new double[] { 1, 2 }); // 1 + 1 + 2 = 4 wide: wire centred at -1.5, coax at 1.
        assertEquals(-1.5D, two[0], 1e-9);
        assertEquals(1.0D, two[1], 1e-9);
        double[] seven = WireLook.layout(new double[] { 1, 1, 1, 1, 1, 1, 1 }); // 13 wide: -6 .. 6.
        assertEquals(-6.0D, seven[0], 1e-9);
        assertEquals(6.0D, seven[6], 1e-9);
        double[] fat = WireLook.layout(new double[] { 2, 2, 2, 2, 2, 2, 2 }); // 14 wide with no gaps: -6 .. 6.
        assertEquals(-6.0D, fat[0], 1e-9);
        assertEquals(6.0D, fat[6], 1e-9);
        assertEquals(2.0D, fat[1] - fat[0], 1e-9);
    }

    // ---- Cable against a tower beam (2026-10-28): drawn out past the block face to the beam's steel ----

    @Test
    void runUpABeamCarriesItsReachOnEveryPieceOnThatEdge() {
        // Comes in from the south on the floor and goes up a normal beam (steel 5 px back) on the north edge.
        assertEquals(List.of(p(Part.MIDDLE, null), new Placed(Part.HORIZONTAL, NORTH, 5, 5), p(Part.HORIZONTAL, SOUTH),
                        new Placed(Part.EDGE, NORTH, 5, 5)),
                WireLook.parts(new Look(NORTH, null, End.NONE, false, 5, 0, 0), links(SOUTH, UP)));
    }

    @Test
    void runPastTheTopOfABeamEasesBackToTheEdge() {
        // The wire below ran up a thin beam (6 px); here the wall is gone and the run carries on up.
        assertEquals(List.of(new Placed(Part.EDGE_FULL, NORTH, 6, 0)),
                WireLook.parts(new Look(NORTH, NORTH, End.NONE, false, 0, 6, 0), links(UP, DOWN)));
    }

    @Test
    void runUpABeamSlidesOutPastTheFace() {
        WireLook.Affine north = WireLook.affine(new Placed(Part.EDGE, NORTH, 5, 5), 0);
        assertEquals(0.5D - 5.0D / 16.0D, north.z(0.0D, 0.5D, 0.5D), 1e-9);
        assertEquals(0.5D, north.x(0.5D, 0.5D, 0.5D), 1e-9);
        WireLook.Affine east = WireLook.affine(new Placed(Part.EDGE, EAST, 4, 4), 0);
        assertEquals(1.0D + 4.0D / 16.0D, east.x(1.0D, 0.5D, 0.5D), 1e-9);
    }

    @Test
    void runEasingBackSlantsFromFootToHead() {
        WireLook.Affine floor = WireLook.affine(new Placed(Part.EDGE_FULL, SOUTH, 6, 0), 0);
        assertEquals(1.0D + 6.0D / 16.0D, floor.z(0.5D, 0.0D, 1.0D), 1e-9); // Out at the foot,
        assertEquals(1.0D, floor.z(0.5D, 1.0D, 1.0D), 1e-9);                 // back on the face at the head.
        WireLook.Affine hanging = WireLook.affine(new Placed(Part.EDGE_FULL, SOUTH, 6, 0), 0, WireLook.Faces.all(0), true);
        assertEquals(1.0D + 6.0D / 16.0D, hanging.z(0.5D, 0.0D, 1.0D), 1e-9); // Foot and head are world bottom and top,
        assertEquals(1.0D, hanging.z(0.5D, 1.0D, 1.0D), 1e-9);                 // either way up.
    }

    @Test
    void stubToABeamRunStretchesOutWithItsMiddleEndInPlace() {
        WireLook.Affine stub = WireLook.affine(new Placed(Part.HORIZONTAL, WEST, 5, 5), 0);
        assertEquals(-5.0D / 16.0D, stub.x(0.0D), 1e-9);        // The face end out at the run,
        assertEquals(7.0D / 16.0D, stub.x(7.0D / 16.0D), 1e-9); // the middle end where it was.
    }

    @Test
    void climbOverABeamKeepsItsFarEndOverTheMiddle() {
        WireLook.Affine over = WireLook.affine(new Placed(Part.CLIMB_OVERHANG, NORTH, 4, 4), 0);
        assertEquals(-7.0D / 16.0D, over.z(-7.0D / 16.0D), 1e-9);           // Far end over the beam's middle,
        assertEquals((1.0D - 4.0D) / 16.0D, over.z(1.0D / 16.0D), 1e-9);    // inner cap out with the run.
    }

    @Test
    void hitboxPieceIsTheWallPiece() {
        assertSame(Part.EDGE, new Placed(Part.EDGE, NORTH, 5, 5).plain().part());
        assertEquals(p(Part.EDGE, NORTH), new Placed(Part.EDGE, NORTH, 5, 5).plain());
    }

    @Test
    void stubToAHornFeedRunsAcrossTheBaseAndSlantsToTheCorner() {
        Look look = new Look(null, null, End.NONE, false, 0, 0, 0, NORTH, 8, 0, 0);
        assertEquals(List.of(p(Part.MIDDLE, null), new Placed(Part.HORIZONTAL, NORTH, 16, 16, 8)), WireLook.parts(look, links(NORTH)));
        WireLook.Affine feed = WireLook.affine(new Placed(Part.HORIZONTAL, NORTH, 16, 16, 8), 0);
        assertEquals(-1.0D, feed.z(0.5D, 0.0D, 0.0D), 1e-9);                    // The face end across the whole base block,
        assertEquals(1.0D, feed.x(0.5D, 0.0D, 0.0D), 1e-9);                     // over at the corner where the four meet;
        assertEquals(7.0D / 16.0D, feed.z(0.5D, 0.0D, 7.0D / 16.0D), 1e-9);     // the middle end where it was.
        assertEquals(0.5D, feed.x(0.5D, 0.0D, 7.0D / 16.0D), 1e-9);
        WireLook.Affine east = WireLook.affine(new Placed(Part.HORIZONTAL, EAST, 16, 16, -8), 0);
        assertEquals(2.0D, east.x(1.0D, 0.0D, 0.5D), 1e-9);
        assertEquals(0.0D, east.z(1.0D, 0.0D, 0.5D), 1e-9);
        assertEquals(0.5D, east.z(9.0D / 16.0D, 0.0D, 0.5D), 1e-9);
    }
}

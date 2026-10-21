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
}

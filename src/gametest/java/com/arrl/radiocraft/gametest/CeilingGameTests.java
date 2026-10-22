package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blocks.WireLook;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Radios and wires hung from the underside of blocks (maintainer, 2026-10-21): a hanging radio's shape meets the
 * ceiling, coax along a ceiling links a hanging radio to its antenna, and a ceiling wire climbs down the face of a
 * beam to the wire under it, the floor climb upside down.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CeilingGameTests {

    private static final String BATCH = "ceiling";

    /** A wire of {@code wire}'s kind hanging at {@code pos}, worked out the way placing it against the ceiling does. */
    private static void hangWire(GameTestHelper helper, BlockPos pos) {
        WireBlock wire = RadiocraftBlocks.COAX_WIRE.get();
        helper.setBlock(pos, wire.getBlockState(helper.getLevel(), helper.absolutePos(pos), true));
    }

    private static void ceiling(GameTestHelper helper, int y, int fromX, int toX, int z) {
        for(int x = fromX; x <= toX; x++)
            helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    /** A radio under a ceiling, coax along the ceiling, a dummy load at the end: the radio sees its antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void hangingRadioOnCeilingCoax(GameTestHelper helper) {
        BlockPos radio = new BlockPos(3, 5, 6);
        ceiling(helper, 6, 2, 8, 6);
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get().defaultBlockState().setValue(RadioBlock.CEILING, true));
        hangWire(helper, radio.east());
        hangWire(helper, radio.east(2));
        helper.setBlock(radio.east(3), RadiocraftBlocks.DUMMY_LOAD.get());
        helper.startSequence()
                .thenExecute(() -> {
                    BlockState state = helper.getBlockState(radio);
                    double top = state.getShape(helper.getLevel(), helper.absolutePos(radio), CollisionContext.empty()).max(Direction.Axis.Y);
                    helper.assertTrue(top == 1.0D, "The hanging radio's shape reaches " + top + ", not the ceiling");
                    helper.assertTrue(WireBlock.isCeiling(helper.getBlockState(radio.east())), "The coax is not hanging");
                    helper.assertTrue(helper.getBlockState(radio.east()).getValue(WireBlock.WEST)
                            && helper.getBlockState(radio.east()).getValue(WireBlock.EAST), "The ceiling coax does not link both ways");
                })
                .thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().size() == 1,
                        "The hanging radio does not see the dummy load through the ceiling coax"))
                .thenSucceed();
    }

    /**
     * The maintainer's build (2026-10-21): coax along the underside of an overhang, out past its end, then up the
     * outside face of the overhang. The corner wire hangs, the wires up the face stand; they share the run on the
     * face's edge, so neither draws a stub out to the middle of its block.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void ceilingRunTurnsUpAWallWithoutStubs(GameTestHelper helper) {
        WireBlock coax = RadiocraftBlocks.COAX_WIRE.get();
        ceiling(helper, 6, 5, 8, 6); // The overhang: x 5 to 8 at y 6.
        BlockPos[] under = { new BlockPos(7, 5, 6), new BlockPos(6, 5, 6), new BlockPos(5, 5, 6) };
        BlockPos corner = new BlockPos(4, 5, 6); // Just past the overhang's end, under nothing.
        BlockPos[] up = { new BlockPos(4, 6, 6), new BlockPos(4, 7, 6) }; // Up the overhang's west face and above it.
        for(int pass = 0; pass < 3; pass++) { // Each wire's look depends on its neighbours: settle them all.
            for(BlockPos pos : under)
                helper.setBlock(pos, coax.getBlockState(helper.getLevel(), helper.absolutePos(pos), true));
            helper.setBlock(corner, coax.getBlockState(helper.getLevel(), helper.absolutePos(corner), true));
            for(BlockPos pos : up)
                helper.setBlock(pos, coax.getBlockState(helper.getLevel(), helper.absolutePos(pos), false));
        }
        helper.startSequence()
                .thenExecute(() -> {
                    for(BlockPos pos : new BlockPos[] { corner, up[0] }) {
                        BlockState state = helper.getBlockState(pos);
                        var parts = WireLook.parts(coax.look(helper.getLevel(), helper.absolutePos(pos), state), WireBlock.lookConnections(state));
                        helper.assertFalse(parts.stream().anyMatch(p -> p.part() == WireLook.Part.MIDDLE),
                                "The wire at " + pos.toShortString() + " draws a stub to its middle: " + parts);
                    }
                    helper.assertTrue(helper.getBlockState(up[0]).getValue(WireBlock.HUG) == WireBlock.Edge.EAST,
                            "The run up the face is not on the overhang's edge: " + helper.getBlockState(up[0]).getValue(WireBlock.HUG));
                })
                .thenSucceed();
    }

    /**
     * Coax on a ceiling beside a beam hanging one block lower, a wire under the beam: the ceiling wire runs down the
     * beam's face (hugging it) and links diagonally to the wire under it, like a floor wire climbing a step.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void ceilingWireClimbsDownABeam(GameTestHelper helper) {
        BlockPos top = new BlockPos(6, 5, 6); // Under the ceiling, north of the beam.
        BlockPos beam = top.south();
        BlockPos under = beam.below(); // Hanging from the beam's underside.
        ceiling(helper, 6, 5, 7, 6);
        ceiling(helper, 6, 5, 7, 7);
        helper.setBlock(beam, Blocks.STONE);
        hangWire(helper, under);
        hangWire(helper, top);
        helper.startSequence()
                .thenExecute(() -> {
                    BlockState state = helper.getBlockState(top);
                    helper.assertTrue(state.getValue(WireBlock.HUG) == WireBlock.Edge.SOUTH, "The ceiling wire hugs " + state.getValue(WireBlock.HUG) + ", not the beam");
                    helper.assertTrue(state.getValue(WireBlock.CLIMB) != WireBlock.Climb.NONE, "The ceiling wire does not climb down the beam");
                    WireBlock wire = (WireBlock)state.getBlock();
                    helper.assertTrue(wire.getLinks(helper.getLevel(), helper.absolutePos(top)).containsKey(helper.absolutePos(under)),
                            "The climb does not link to the wire under the beam");
                    helper.assertTrue(wire.getLinks(helper.getLevel(), helper.absolutePos(under)).containsKey(helper.absolutePos(top)),
                            "The wire under the beam does not link back up to the climber");
                })
                .thenSucceed();
    }
}

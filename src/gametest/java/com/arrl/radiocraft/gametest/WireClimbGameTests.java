package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import com.arrl.radiocraft.common.blocks.WireLook;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Wires climbing a block like redstone dust: a coax at the foot of a step runs up its face and links diagonally to
 * the coax (or device) on top. The station is a 20m radio on the ground, a one-block step, and the balun on top of a
 * second step, so the only way from radio to antenna is up the climb.
 *
 * <pre>
 *   z:  2      3     4      5        6        7
 *     radio  coax  FOOT  [STEP]   [stone]  [stone]        y = 2 (ground)
 *                         TOP     coax     balun           y = 3
 * </pre>
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WireClimbGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 2);
    private static final BlockPos GROUND_COAX = new BlockPos(8, 2, 3);
    private static final BlockPos FOOT = new BlockPos(8, 2, 4); // Climbs south, up the step.
    private static final BlockPos STEP = new BlockPos(8, 2, 5);
    private static final BlockPos TOP = new BlockPos(8, 3, 5); // On the step, stub north to the climber.
    private static final BlockPos UPPER_COAX = new BlockPos(8, 3, 6);
    private static final BlockPos BALUN = new BlockPos(8, 3, 7);

    /** The worked-out look of the wire at {@code pos} (arrival and end are no longer stored in the block state). */
    private static WireLook.Look look(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return ((WireBlock) state.getBlock()).look(helper.getLevel(), helper.absolutePos(pos), state);
    }

    private static void placeStationEnds(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(STEP, Blocks.STONE);
        helper.setBlock(new BlockPos(8, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 2, 7), Blocks.STONE);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
    }

    private static void coax(GameTestHelper helper, BlockPos pos) {
        GameTestUtil.placeWire(helper, pos, RadiocraftBlocks.COAX_WIRE.get());
    }

    private static int antennasSeen(GameTestHelper helper) {
        return GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas().size();
    }

    private static void assertRadioSeesMatchedDipole(GameTestHelper helper) {
        List<AntennaNetworkObject> antennas = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
        helper.assertTrue(antennas.size() == 1, "Radio sees " + antennas.size() + " antennas");
        helper.assertTrue(antennas.getFirst().getAntenna() != null, "Antenna not recognized yet");
        float frequency = GameTestUtil.blockEntity(helper, RADIO, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class).getFrequency();
        double swr = antennas.getFirst().getSWR(frequency);
        helper.assertTrue(swr == 1.0D, "SWR was " + swr);
    }

    private static boolean climbsSouth(GameTestHelper helper, BlockPos pos) {
        return look(helper, pos).end().isClimb() && helper.getBlockState(pos).getValue(WireBlock.HUG) == WireBlock.Edge.SOUTH;
    }

    private static void assertClimbState(GameTestHelper helper) {
        helper.assertTrue(climbsSouth(helper, FOOT), "Foot coax should climb south up the step");
        helper.assertTrue(look(helper, FOOT).end() == WireLook.End.CLIMB_WIRE, "A climb to the coax on the step ends at a wire");
        helper.assertTrue(!helper.getBlockState(FOOT).getValue(WireBlock.SOUTH), "Foot coax has no plain south connection (the step is solid)");
        helper.assertTrue(look(helper, TOP).arrive() == Direction.NORTH, "Top coax should take the climber in on its north edge");
        helper.assertTrue(!helper.getBlockState(TOP).getValue(WireBlock.NORTH), "Top coax has no neighbour to the north");
        helper.assertTrue(!helper.getBlockState(TOP).getValue(WireBlock.DOWN), "Top coax must not claim a down connection into the step");
    }

    /**
     * The other way up a step: a column of wire beside the wall. The foot hugs the wall without climbing, the wire
     * above it arrives on the same wall, and the radio still sees the antenna through the column.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void wireColumnBesideTheStepHugsItAndStillLinks(GameTestHelper helper) {
        placeStationEnds(helper);
        BlockPos aboveFoot = FOOT.above();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    coax(helper, TOP);
                    coax(helper, UPPER_COAX);
                    coax(helper, GROUND_COAX);
                    coax(helper, FOOT);
                    coax(helper, aboveFoot); // A wire straight above the foot: it carries the run, so no climb.
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(!look(helper, FOOT).end().isClimb(), "A wire above the foot means no climb");
                    helper.assertTrue(helper.getBlockState(FOOT).getValue(WireBlock.UP), "Foot links up to the wire above it");
                    helper.assertTrue(helper.getBlockState(FOOT).getValue(WireBlock.HUG) == WireBlock.Edge.SOUTH, "Foot should hug the step");
                    helper.assertTrue(look(helper, aboveFoot).arrive() == Direction.SOUTH, "The wire above arrives on the step's face");
                    helper.assertTrue(helper.getBlockState(aboveFoot).getValue(WireBlock.HUG) == WireBlock.Edge.NONE, "The wire above does not go on up");
                    helper.assertTrue(helper.getBlockState(aboveFoot).getValue(WireBlock.SOUTH), "The wire above links south onto the step's wire");
                    helper.assertTrue(helper.getBlockState(TOP).getValue(WireBlock.NORTH), "The wire on the step links north to the column");
                    assertRadioSeesMatchedDipole(helper);
                })
                .thenExecute(() -> helper.setBlock(aboveFoot, Blocks.AIR)) // Take the column piece away: the foot climbs instead.
                .thenWaitUntil(() -> {
                    assertClimbState(helper);
                    assertRadioSeesMatchedDipole(helper);
                })
                .thenSucceed();
    }

    /** Coax laid from the radio up over the step to the balun, foot before top: the climb appears when the top wire lands. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void coaxClimbsAStepToReachTheBalun(GameTestHelper helper) {
        placeStationEnds(helper);
        helper.startSequence()
                .thenIdle(2) // Let block entities load and create their network objects.
                .thenExecute(() -> {
                    coax(helper, GROUND_COAX);
                    coax(helper, FOOT);
                    helper.assertTrue(!look(helper, FOOT).end().isClimb(), "Nothing on the step yet, so no climb");
                    coax(helper, TOP);
                    coax(helper, UPPER_COAX);
                })
                .thenWaitUntil(() -> {
                    assertClimbState(helper);
                    assertRadioSeesMatchedDipole(helper);
                })
                .thenSucceed();
    }

    /** The same run laid top first: the foot wire computes its climb at placement and the top wire grows its stub. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void placingTheFootLastLinksTheClimb(GameTestHelper helper) {
        placeStationEnds(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    coax(helper, UPPER_COAX);
                    coax(helper, TOP);
                    helper.assertTrue(!helper.getBlockState(TOP).getValue(WireBlock.NORTH), "No climber yet, so no stub north");
                    coax(helper, GROUND_COAX);
                    coax(helper, FOOT);
                })
                .thenWaitUntil(() -> {
                    assertClimbState(helper);
                    assertRadioSeesMatchedDipole(helper);
                })
                .thenSucceed();
    }

    /** Breaking the step the wire climbs ends the climb and splits the network: the radio loses the antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void breakingTheStepCutsTheClimb(GameTestHelper helper) {
        placeStationEnds(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    coax(helper, GROUND_COAX);
                    coax(helper, FOOT);
                    coax(helper, TOP);
                    coax(helper, UPPER_COAX);
                })
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenExecute(() -> helper.setBlock(STEP, Blocks.AIR))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!look(helper, FOOT).end().isClimb(), "Climb should end with the step gone");
                    helper.assertTrue(look(helper, TOP).arrive() == null, "Top arrival should go with the climb");
                    helper.assertTrue(antennasSeen(helper) == 0, "Radio should lose the antenna, sees " + antennasSeen(helper));
                })
                .thenExecute(() -> helper.setBlock(STEP, Blocks.STONE)) // Put the step back: the climb and the link return.
                .thenWaitUntil(() -> {
                    assertClimbState(helper);
                    assertRadioSeesMatchedDipole(helper);
                })
                .thenSucceed();
    }

    /**
     * In a trench with walls both sides, the run up picks the wall on the side the wire above leaves towards (west
     * here, although east comes first in the search order), so it meets that wire head on.
     */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void runGoesUpTheWallOnTheSideItLeaves(GameTestHelper helper) {
        BlockPos foot = new BlockPos(8, 2, 8);
        helper.setBlock(foot.west(), Blocks.STONE);
        helper.setBlock(foot.east(), Blocks.STONE);
        coax(helper, foot.west().above()); // The run's destination: on top of the west wall.
        coax(helper, foot.above());
        coax(helper, foot);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(foot).getValue(WireBlock.HUG) == WireBlock.Edge.WEST,
                    "Foot should hug the west wall, the side the run leaves on, but hugs " + helper.getBlockState(foot).getValue(WireBlock.HUG));
            helper.assertTrue(look(helper, foot.above()).arrive() == Direction.WEST, "The wire above arrives on the west edge");
            helper.succeed();
        });
    }

    /**
     * A one-wide trench: the coax on its floor climbs both walls, so the radio on one side reaches the balun on the
     * other without a bridge over the trench.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void coaxClimbsBothWallsOfAOneWideTrench(GameTestHelper helper) {
        BlockPos floor = new BlockPos(8, 2, 8);
        BlockPos radio = new BlockPos(6, 3, 8), balun = new BlockPos(10, 3, 8);
        helper.setBlock(floor.west(), Blocks.STONE);
        helper.setBlock(floor.east(), Blocks.STONE);
        helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(balun, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, balun, balun.north(5));
        GameTestUtil.wire(helper, balun, balun.south(5));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    coax(helper, floor.west().above());
                    coax(helper, floor.east().above());
                    coax(helper, floor);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(look(helper, floor).back(), "Floor coax should climb both walls");
                    helper.assertTrue(look(helper, floor.west().above()).arrive() != null, "West top coax takes the climb in");
                    helper.assertTrue(look(helper, floor.east().above()).arrive() != null, "East top coax takes the climb in");
                    List<AntennaNetworkObject> antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
                    helper.assertTrue(antennas.size() == 1, "Radio should reach the balun across the trench, sees " + antennas.size());
                })
                .thenSucceed();
    }

    /** A climb can end straight at a device: the balun sits on the step itself. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void coaxClimbsStraightIntoABalun(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(STEP, Blocks.STONE);
        helper.setBlock(TOP, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, TOP, TOP.west(5));
        GameTestUtil.wire(helper, TOP, TOP.east(5));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    coax(helper, GROUND_COAX);
                    coax(helper, FOOT);
                    helper.assertTrue(climbsSouth(helper, FOOT), "Foot coax should climb to the balun on the step");
                    helper.assertTrue(look(helper, FOOT).end() == WireLook.End.CLIMB_DEVICE, "A climb into a balun ends at a device");
                })
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenSucceed();
    }
}

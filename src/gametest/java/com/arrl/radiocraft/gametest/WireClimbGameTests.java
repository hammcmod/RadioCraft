package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
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

    private static void assertClimbState(GameTestHelper helper) {
        helper.assertTrue(helper.getBlockState(FOOT).getValue(WireBlock.CLIMB_SOUTH), "Foot coax should climb south up the step");
        helper.assertTrue(!helper.getBlockState(FOOT).getValue(WireBlock.SOUTH), "Foot coax has no plain south connection (the step is solid)");
        helper.assertTrue(helper.getBlockState(TOP).getValue(WireBlock.NORTH), "Top coax should show a stub north towards the climber");
        helper.assertTrue(!helper.getBlockState(TOP).getValue(WireBlock.DOWN), "Top coax must not claim a down connection into the step");
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
                    helper.assertTrue(!helper.getBlockState(FOOT).getValue(WireBlock.CLIMB_SOUTH), "Nothing on the step yet, so no climb");
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
                    helper.assertTrue(!helper.getBlockState(FOOT).getValue(WireBlock.CLIMB_SOUTH), "Climb should end with the step gone");
                    helper.assertTrue(!helper.getBlockState(TOP).getValue(WireBlock.NORTH), "Top stub should go with the climb");
                    helper.assertTrue(antennasSeen(helper) == 0, "Radio should lose the antenna, sees " + antennasSeen(helper));
                })
                .thenExecute(() -> helper.setBlock(STEP, Blocks.STONE)) // Put the step back: the climb and the link return.
                .thenWaitUntil(() -> {
                    assertClimbState(helper);
                    assertRadioSeesMatchedDipole(helper);
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
                    helper.assertTrue(helper.getBlockState(FOOT).getValue(WireBlock.CLIMB_SOUTH), "Foot coax should climb to the balun on the step");
                })
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenSucceed();
    }
}

package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.PassthroughBlockEntity;
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
 * The passthrough: a wall block made with the passthrough item that wire and coax run through. A stone wall two
 * blocks thick stands between the radio and the rest of the station; one block of it is a passthrough.
 *
 * <pre>
 *   z:  2      3      4(wall)  5(wall)   6      7
 *     radio  coax   [pass]   [stone]    ...            coax goes through the passthrough; the stone next to it is a wall.
 * </pre>
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PassthroughGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 2);
    private static final BlockPos COAX_IN = new BlockPos(8, 2, 3);
    private static final BlockPos WALL = new BlockPos(8, 2, 4);
    private static final BlockPos COAX_OUT = new BlockPos(8, 2, 5);
    private static final BlockPos BALUN = new BlockPos(8, 2, 6);

    /** Turns the block at {@code pos} into a passthrough the way the item does. */
    private static void passthrough(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, RadiocraftBlocks.PASSTHROUGH.get().getBlockState(helper.getLevel(), helper.absolutePos(pos)));
        PassthroughBlockEntity be = GameTestUtil.blockEntity(helper, pos, PassthroughBlockEntity.class);
        be.setMimic(Blocks.STONE.defaultBlockState());
    }

    private static void assertRadioSeesMatchedDipole(GameTestHelper helper) {
        List<AntennaNetworkObject> antennas = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
        helper.assertTrue(antennas.size() == 1, "Radio sees " + antennas.size() + " antennas");
        helper.assertTrue(antennas.getFirst().getAntenna() != null, "Antenna not recognized yet");
    }

    /** Coax into a passthrough in a wall and out the other side: the radio reaches the balun. Then break the wall piece. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void coaxRunsThroughAPassthroughWall(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(WALL, Blocks.STONE);
        helper.setBlock(WALL.east(), Blocks.STONE);
        helper.setBlock(WALL.west(), Blocks.STONE);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    GameTestUtil.placeWire(helper, COAX_IN, RadiocraftBlocks.COAX_WIRE.get());
                    GameTestUtil.placeWire(helper, COAX_OUT, RadiocraftBlocks.COAX_WIRE.get());
                    helper.assertTrue(GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas().isEmpty(), "A stone wall should block the coax");
                    passthrough(helper, WALL);
                    helper.assertTrue(helper.getBlockState(COAX_IN).getValue(WireBlock.SOUTH), "Coax should link into the passthrough");
                    helper.assertTrue(helper.getBlockState(WALL).getValue(WireBlock.NORTH) && helper.getBlockState(WALL).getValue(WireBlock.SOUTH), "Passthrough should link both ways");
                })
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenExecute(() -> helper.setBlock(WALL, Blocks.STONE))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas().isEmpty(), "Plain stone again should cut the run");
                    helper.succeed();
                });
    }

    /** The same passthrough carries power: a battery on the far side ends up on the radio's power network. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void powerWireRunsThroughThePassthroughToo(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(WALL, Blocks.STONE);
        helper.setBlock(BALUN, RadiocraftBlocks.LARGE_BATTERY.get()); // Not a balun here: the battery sits where the balun did.
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    passthrough(helper, WALL);
                    GameTestUtil.placeWire(helper, COAX_IN, RadiocraftBlocks.WIRE.get());
                    GameTestUtil.placeWire(helper, COAX_OUT, RadiocraftBlocks.WIRE.get());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    RadioNetworkObject radio = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class);
                    BatteryNetworkObject battery = GameTestUtil.networkObject(helper, BALUN, BatteryNetworkObject.class);
                    boolean shared = false;
                    for(BENetwork network : radio.getNetworks().values())
                        shared |= network instanceof PowerBENetwork && network.getNetworkObjects().contains(battery);
                    helper.assertTrue(shared, "Battery and radio should share a power network through the passthrough");
                    helper.assertTrue(radio.hasBattery(), "Radio should see the battery through the wall");
                    helper.succeed();
                });
    }
}

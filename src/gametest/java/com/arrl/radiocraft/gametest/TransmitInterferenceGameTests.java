package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.rfi.TransmitInterference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Redstone interference on transmit: keying a station induces a level in dust near its antenna and flips switches.
 * Station A of the two-station layout (radio z=1, balun z=3, dipole arms x 3..13 at z=3). The hit chance is forced to 1 so the picks are certain; nothing here needs a frequency because PTT alone
 * counts as transmitting and no audio is sent.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransmitInterferenceGameTests {

    private static void everyComponentGetsHit() {
        RadiocraftServerConfig.REDSTONE_INTERFERENCE_TRANSMIT_CHANCE.set(1.0D);
    }

    /** Dust on a stone block. */
    private static void dust(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.REDSTONE_WIRE);
    }

    private static int power(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(RedStoneWireBlock.POWER);
    }

    private static void keyUp(GameTestHelper helper, BlockPos radio) {
        radio(helper, radio).setPTTDown(true);
        helper.assertTrue(radio(helper, radio).canTransmitVoice(), "Radio cannot transmit");
    }

    /** Full-power rig, matched dipole: dust 2 blocks from the arm reads level 4 while keyed, dust 9 blocks away does not. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void transmitterInducesLevelInNearbyDust(GameTestHelper helper) {
        everyComponentGetsHit();
        BlockPos near = BALUN_A.east(2).south(2);
        BlockPos far = BALUN_A.south(9);
        buildStation(helper, RADIO_A, BALUN_A);
        dust(helper, near);
        dust(helper, far);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(power(helper, near) == 0, "Dust powered before transmitting");
            keyUp(helper, RADIO_A);
        });
        helper.runAtTickTime(34, () -> {
            TransmitInterference.Targets targets = radio(helper, RADIO_A).getTransmitTargets();
            helper.assertTrue(targets != null && targets.dust().containsKey(helper.absolutePos(near)), "Near dust was not picked: " + targets);
            helper.assertTrue(targets.dust().get(helper.absolutePos(near)) == TransmitInterference.BASE_LEVEL, "Matched station should induce the base level");
            helper.assertTrue(power(helper, near) == TransmitInterference.BASE_LEVEL, "Near dust reads " + power(helper, near) + " while keyed (is the dust mixin applied?)");
            helper.assertTrue(power(helper, far) == 0, "Far dust reads " + power(helper, far));
            radio(helper, RADIO_A).setPTTDown(false);
        });
        helper.runAtTickTime(38, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getTransmitTargets() == null, "Targets kept after key-up");
            helper.assertTrue(power(helper, near) == 0, "Near dust still reads " + power(helper, near) + " after key-up");
            helper.succeed();
        });
    }

    /** Keying flips a lever in reach and makes a torch flicker. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void transmitterFlipsLeverAndFlickersTorch(GameTestHelper helper) {
        everyComponentGetsHit();
        BlockPos lever = BALUN_A.west(2).south(2);
        BlockPos torch = BALUN_A.east(2).south(2);
        buildStation(helper, RADIO_A, BALUN_A);
        helper.setBlock(lever.below(), Blocks.STONE);
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
        helper.setBlock(torch.below(), Blocks.STONE);
        helper.setBlock(torch, Blocks.REDSTONE_TORCH);
        boolean[] torchWentOut = new boolean[1];
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            helper.assertBlockProperty(lever, BlockStateProperties.POWERED, false);
            helper.assertBlockProperty(torch, BlockStateProperties.LIT, true);
            keyUp(helper, RADIO_A);
        });
        for(int t = 30; t <= 33; t++)
            helper.runAtTickTime(t, () -> torchWentOut[0] |= !helper.getBlockState(torch).getValue(BlockStateProperties.LIT));
        helper.runAtTickTime(36, () -> {
            helper.assertBlockProperty(lever, BlockStateProperties.POWERED, true);
            helper.assertTrue(torchWentOut[0], "Torch never went out");
            helper.assertBlockProperty(torch, BlockStateProperties.LIT, true); // Its own tick put it right again.
            radio(helper, RADIO_A).setPTTDown(false);
            helper.succeed();
        });
    }

    /** With a bad SWR the RF reaches the shack: dust 9 blocks from the radio (and well clear of the antenna) reads more than the base level. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void badSwrReachesTheShackAndRaisesTheLevel(GameTestHelper helper) {
        everyComponentGetsHit();
        BlockPos shack = RADIO_A.south(9); // 9 from the radio, 7 from the balun, 8 from the coax.
        buildStationWithArms(helper, RADIO_A, BALUN_A, 2); // 2+2 arms on 20m: badly matched.
        dust(helper, shack);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            double swr = GameTestUtil.networkObject(helper, RADIO_A, RadioNetworkObject.class).getAntennas().getFirst().getSWR(radio(helper, RADIO_A).getFrequency());
            helper.assertTrue(TransmitInterference.isBadSwr(swr), "Test station SWR " + swr + " does not count as bad");
            keyUp(helper, RADIO_A);
        });
        helper.runAtTickTime(34, () -> {
            int level = power(helper, shack);
            helper.assertTrue(level > TransmitInterference.BASE_LEVEL, "Shack dust reads " + level + " with a bad SWR");
            radio(helper, RADIO_A).setPTTDown(false);
            helper.succeed();
        });
    }

    /** A QRP rig only reaches 2 blocks from the antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void qrpRigReachesLess(GameTestHelper helper) {
        everyComponentGetsHit();
        BlockPos close = BALUN_A.east(3).south(1);
        BlockPos fourAway = BALUN_A.south(4);
        buildStationWithArms(helper, RADIO_A, BALUN_A, 5);
        helper.setBlock(RADIO_A, RadiocraftBlocks.QRP_RADIO_20M.get());
        dust(helper, close);
        dust(helper, fourAway);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(radio(helper, RADIO_A).isQrp(), "QRP radio is not QRP");
            keyUp(helper, RADIO_A);
        });
        helper.runAtTickTime(34, () -> {
            helper.assertTrue(power(helper, close) == TransmitInterference.BASE_LEVEL, "Dust one block from the arm reads " + power(helper, close));
            helper.assertTrue(power(helper, fourAway) == 0, "Dust four blocks from the balun reads " + power(helper, fourAway) + " for a QRP rig");
            radio(helper, RADIO_A).setPTTDown(false);
            helper.succeed();
        });
    }

    /** Like {@link TransmissionGameTests#buildStation} but with a chosen arm length. */
    static void buildStationWithArms(GameTestHelper helper, BlockPos radio, BlockPos balun, int arm) {
        helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(balun, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, balun, balun.west(arm));
        GameTestUtil.wire(helper, balun, balun.east(arm));
        int step = Integer.signum(balun.getZ() - radio.getZ());
        for(BlockPos p = radio.relative(Direction.SOUTH, step); !p.equals(balun); p = p.relative(Direction.SOUTH, step))
            GameTestUtil.placeWire(helper, p, RadiocraftBlocks.COAX_WIRE.get());
    }
}

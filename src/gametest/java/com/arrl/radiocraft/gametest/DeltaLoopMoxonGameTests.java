package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftAntennaTypes;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.DeltaLoopAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.MoxonAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DeltaLoopAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.MoxonAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Locale;
import java.util.UUID;

/** The delta loop and the Moxon (doc/DeltaLoopAndMoxonPlan.md), built from wire, poles and baluns as a player would. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DeltaLoopMoxonGameTests {

    private static final float F_20M = 14_200_000.0F;

    private static StaticAntenna<?> antenna(GameTestHelper helper, BlockPos balun) {
        return GameTestUtil.networkObject(helper, balun, AntennaNetworkObject.class).getAntenna();
    }

    /** An upright 20m delta loop fed at a bottom corner: 7 blocks along the bottom, apex 6 up, on a 4:1 balun at {@code y}. */
    private static BlockPos buildDelta(GameTestHelper helper, int y) {
        BlockPos balun = new BlockPos(3, y, 8);
        helper.setBlock(balun, RadiocraftBlocks.BALUN_FOUR_TO_ONE.get());
        BlockPos corner = balun.east(7), apex = balun.east(3).above(6);
        GameTestUtil.wire(helper, balun, corner);
        GameTestUtil.wire(helper, corner, apex);
        GameTestUtil.wire(helper, apex, balun);
        return balun;
    }

    private static double deltaSwr(StaticAntenna<?> antenna) {
        return RadiocraftAntennaTypes.DELTA_LOOP.getSWR((DeltaLoopAntennaData)antenna.data, F_20M);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void deltaLoopHighUpIsMatched(GameTestHelper helper) {
        BlockPos balun = buildDelta(helper, 9);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(antenna(helper, balun) != null, "Delta loop not recognized yet"))
                .thenExecute(() -> {
                    StaticAntenna<?> antenna = antenna(helper, balun);
                    helper.assertTrue(antenna.type == RadiocraftAntennaTypes.DELTA_LOOP, "Recognized as " + antenna.type.getId());
                    helper.assertFalse(DeltaLoopAntennaType.isTooLow((DeltaLoopAntennaData)antenna.data), "Counted as too low at "
                            + ((DeltaLoopAntennaData)antenna.data).getHeight() + " blocks up");
                    helper.assertTrue(deltaSwr(antenna) < 1.5D, "SWR on 20m " + deltaSwr(antenna));
                })
                .thenSucceed();
    }

    /** On the ground it still works (maintainer, 2026-10-24): slightly worse SWR, much less skip. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void deltaLoopOnTheGroundStillWorksBadly(GameTestHelper helper) {
        BlockPos balun = buildDelta(helper, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(antenna(helper, balun) != null, "Delta loop not recognized yet"))
                .thenExecute(() -> {
                    StaticAntenna<?> antenna = antenna(helper, balun);
                    helper.assertTrue(antenna.type == RadiocraftAntennaTypes.DELTA_LOOP, "Recognized as " + antenna.type.getId());
                    DeltaLoopAntennaData data = (DeltaLoopAntennaData)antenna.data;
                    helper.assertTrue(DeltaLoopAntennaType.isTooLow(data), "A loop on the ground is not too low? " + data.getHeight());
                    helper.assertTrue(deltaSwr(antenna) >= 1.0D + DeltaLoopAntennaType.LOW_SWR_PENALTY, "No low SWR penalty: " + deltaSwr(antenna));
                    helper.assertTrue(DeltaLoopAntennaType.lowSkipFactor(data.getHeight()) <= 0.4D, "Skip barely cut on the ground");
                })
                .thenSucceed();
    }

    /**
     * A 20m Moxon facing north: 1:1 balun, driven element 7 across with 1 block tails folded back, a 1 block gap,
     * reflector tails of 1 and the reflector across the back, 3 deep. {@code reflector} false leaves the reflector off.
     */
    private static BlockPos buildMoxon(GameTestHelper helper, boolean reflector) {
        BlockPos balun = new BlockPos(5, 2, 4);
        helper.setBlock(balun, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        BlockPos front1 = balun.west(3), front2 = balun.east(4);
        GameTestUtil.wire(helper, balun, front1);
        GameTestUtil.wire(helper, balun, front2);
        GameTestUtil.wire(helper, front1, front1.south());
        GameTestUtil.wire(helper, front2, front2.south());
        if(reflector) {
            helper.setBlock(front1.south(2), RadiocraftBlocks.ANTENNA_POLE.get());
            GameTestUtil.wire(helper, front1.south(2), front1.south(3));
            GameTestUtil.wire(helper, front1.south(3), front2.south(3));
            GameTestUtil.wire(helper, front2.south(3), front2.south(2));
        }
        return balun;
    }

    private static <T extends AntennaData> double transmit(StaticAntenna<T> antenna, IAntennaPacket packet, BlockPos to) {
        return antenna.type.getTransmitEfficiency(packet, antenna.data, to, false);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void moxonBeamsForward(GameTestHelper helper) {
        BlockPos balun = buildMoxon(helper, true);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(antenna(helper, balun) != null, "Moxon not recognized yet"))
                .thenExecute(() -> {
                    StaticAntenna<?> antenna = antenna(helper, balun);
                    helper.assertTrue(antenna.type == RadiocraftAntennaTypes.MOXON, "Recognized as " + antenna.type.getId());
                    MoxonAntennaData data = (MoxonAntennaData)antenna.data;
                    helper.assertTrue(data.getWidth() == 7 && data.getDepth() == 3, "Measured " + data.getWidth() + " x " + data.getDepth());
                    helper.assertTrue(data.getFacing() == Direction.NORTH, "Faces " + data.getFacing());
                    double swr = RadiocraftAntennaTypes.MOXON.getSWR(data, F_20M);
                    helper.assertTrue(swr == 1.0D, "SWR on 20m " + swr);

                    Band band = Band.getBand("20m");
                    AntennaVoicePacket packet = new AntennaVoicePacket(() -> helper.getLevel(), new short[960], band, F_20M, 1.0D, antenna, UUID.randomUUID());
                    BlockPos at = helper.absolutePos(balun);
                    double front = transmit(antenna, packet, at.north(100)), rear = transmit(antenna, packet, at.south(100));
                    helper.assertTrue(front > 0.0D && Math.abs(front / rear - MoxonAntennaType.FORWARD / MoxonAntennaType.BEHIND) < 0.01D,
                            String.format(Locale.ROOT, "Front %.4f, back %.4f: expected a %.0f to 1 beam", front, rear, MoxonAntennaType.FORWARD / MoxonAntennaType.BEHIND));
                })
                .thenSucceed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void moxonWithoutItsReflectorIsNotAMoxon(GameTestHelper helper) {
        BlockPos balun = buildMoxon(helper, false);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(antenna(helper, balun) != null, "Antenna not recognized yet"))
                .thenExecute(() -> helper.assertTrue(antenna(helper, balun).type != RadiocraftAntennaTypes.MOXON, "A Moxon without a reflector"))
                .thenSucceed();
    }
}

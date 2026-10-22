package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * The dummy load (maintainer's model, 2026-10-21): touching a radio or on its coax, it is the radio's antenna, a
 * perfect match on every band, on the antenna network of the radio's band, and it only reaches about 10 blocks.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DummyLoadGameTests {

    private static final String BATCH = "dummy_load";
    private static final UUID OPERATOR = UUID.nameUUIDFromBytes("gametest-dummy-load".getBytes());

    private static RadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, RadioBlockEntity.class);
    }

    private static List<AntennaNetworkObject> antennas(GameTestHelper helper, BlockPos radioPos) {
        return GameTestUtil.networkObject(helper, radioPos, RadioNetworkObject.class).getAntennas();
    }

    /** True once the radio sees exactly one antenna and it is on the {@code networkId} antenna network. */
    private static boolean on(GameTestHelper helper, BlockPos radioPos, net.minecraft.resources.ResourceLocation networkId) {
        List<AntennaNetworkObject> found = antennas(helper, radioPos);
        return found.size() == 1 && networkId.equals(found.getFirst().getNetworkId());
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        if(!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
            radio(helper, pos).toggle();
    }

    /** A station: the radio with a dummy load touching its north side. */
    private static void station(GameTestHelper helper, BlockPos pos, Block radio) {
        helper.setBlock(pos, radio);
        helper.setBlock(pos.north(), RadiocraftBlocks.DUMMY_LOAD.get());
    }

    private static short[] tone() {
        short[] audio = new short[960];
        for(int i = 0; i < audio.length; i++)
            audio[i] = (short)(Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
        return audio;
    }

    /** A speaks one frame; true if B heard it this tick. */
    private static boolean heard(GameTestHelper helper, BlockPos a, BlockPos b) {
        RadioBlockEntity speaker = radio(helper, a);
        speaker.setPTTDown(true);
        speaker.acceptVoicePacket(() -> helper.getLevel(), tone(), OPERATOR);
        speaker.setPTTDown(false);
        return radio(helper, b).getLastSignalGameTime() == helper.getLevel().getGameTime();
    }

    /** Touching the radio is enough, its SWR is 1.0 on every band, and it follows a VHF radio onto the VHF network. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void perfectMatchOnEveryBand(GameTestHelper helper) {
        BlockPos hf = new BlockPos(3, 2, 6);
        BlockPos vhf = new BlockPos(11, 2, 6);
        station(helper, hf, RadiocraftBlocks.ALL_BAND_RADIO.get());
        station(helper, vhf, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(antennas(helper, hf).size() == 1, "The all-band radio does not see the dummy load touching it");
                    helper.assertTrue(antennas(helper, vhf).size() == 1, "The base station does not see the dummy load touching it");
                })
                .thenExecute(() -> {
                    AntennaNetworkObject load = antennas(helper, hf).getFirst();
                    for(float hz : new float[] { 3_600_000.0F, 7_100_000.0F, 14_200_000.0F, 28_400_000.0F, 146_000_000.0F })
                        helper.assertTrue(load.getSWR(hz) == 1.0D, "SWR " + load.getSWR(hz) + " at " + hz + " Hz");
                })
                .thenWaitUntil(() -> helper.assertTrue(on(helper, vhf, AntennaNetworkManager.VHF_ID), "The base station's dummy load is not on the VHF network"))
                .thenExecute(() -> helper.assertTrue(on(helper, hf, AntennaNetworkManager.HF_ID), "The all-band radio's dummy load left the HF network"))
                .thenSucceed();
    }

    /** Two stations on dummy loads hear each other a few blocks apart, on VHF and HF, but not 14 blocks apart. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void reachesOnlyAFewBlocks(GameTestHelper helper) {
        BlockPos vhfA = new BlockPos(2, 2, 3), vhfB = new BlockPos(6, 2, 3), vhfFar = new BlockPos(15, 2, 3);
        BlockPos hfA = new BlockPos(2, 2, 10), hfB = new BlockPos(5, 2, 10);
        for(BlockPos pos : List.of(vhfA, vhfB, vhfFar))
            station(helper, pos, RadiocraftBlocks.VHF_BASE_STATION.get());
        for(BlockPos pos : List.of(hfA, hfB))
            station(helper, pos, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.startSequence()
                .thenExecute(() -> {
                    for(BlockPos pos : List.of(vhfA, vhfB, vhfFar)) {
                        radio(helper, pos).setFrequency(146_820_000.0F);
                        powerOn(helper, pos);
                    }
                    for(BlockPos pos : List.of(hfA, hfB)) {
                        radio(helper, pos).setFrequency(14_230_000.0F);
                        powerOn(helper, pos);
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(on(helper, vhfA, AntennaNetworkManager.VHF_ID) && on(helper, vhfB, AntennaNetworkManager.VHF_ID)
                        && on(helper, vhfFar, AntennaNetworkManager.VHF_ID) && on(helper, hfA, AntennaNetworkManager.HF_ID)
                        && on(helper, hfB, AntennaNetworkManager.HF_ID), "Dummy loads not all on their networks yet"))
                .thenExecute(() -> {
                    helper.assertTrue(heard(helper, vhfA, vhfB), "VHF: B did not hear A 4 blocks away");
                    helper.assertFalse(radio(helper, vhfFar).getLastSignalGameTime() == helper.getLevel().getGameTime(), "VHF: heard 13 blocks away");
                    helper.assertTrue(heard(helper, hfA, hfB), "HF: B did not hear A 3 blocks away");
                })
                .thenSucceed();
    }
}

package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * HF block radio behavior: power, tuning, persistence, and finding an antenna over coax.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RadioGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 3);
    private static final BlockPos BALUN = new BlockPos(8, 2, 8);

    @GameTest(template = GameTestUtil.EMPTY)
    public static void radioPowerToggles(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
        RadioNetworkObject net = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class);

        helper.assertFalse(net.isPowered, "Radio should start off");
        radio.toggle();
        helper.assertTrue(net.isPowered, "Radio should be on after toggle");
        radio.toggle();
        helper.assertFalse(net.isPowered, "Radio should be off after second toggle");
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void frequencyStepsAndClampsToBand(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
        float min = radio.getBand().minFrequency();

        helper.assertTrue(radio.getFrequency() == min, "Radio should start at band bottom, was " + radio.getFrequency());
        radio.updateFrequency(3);
        helper.assertTrue(radio.getFrequency() == min + 3_000, "Expected 3 steps of 1 kHz, was " + radio.getFrequency());
        radio.updateFrequency(-10);
        helper.assertTrue(radio.getFrequency() == min, "Should clamp at band bottom, was " + radio.getFrequency());
        radio.updateFrequency(1_000_000);
        helper.assertTrue(radio.getFrequency() <= radio.getBand().maxFrequency(), "Should clamp at band top, was " + radio.getFrequency());
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void ssbAndCwAreMutuallyExclusive(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);

        radio.setSSBEnabled(true);
        radio.setCWEnabled(true);
        helper.assertTrue(radio.getCWEnabled() && !radio.getSSBEnabled(), "Enabling CW should disable SSB");
        radio.setSSBEnabled(true);
        helper.assertTrue(radio.getSSBEnabled() && !radio.getCWEnabled(), "Enabling SSB should disable CW");
        helper.succeed();
    }

    /** Lays coax from the radio to the balun in order, the way a player walking the run would. */
    private static void placeCoaxRun(GameTestHelper helper) {
        for (int z = RADIO.getZ() + 1; z < BALUN.getZ(); z++)
            GameTestUtil.placeWire(helper, new BlockPos(8, 2, z), RadiocraftBlocks.COAX_WIRE.get());
    }

    /** Lays the same run but leaves a middle piece for last, so the last piece joins two existing runs. */
    private static void placeCoaxRunClosingInMiddle(GameTestHelper helper) {
        int gap = RADIO.getZ() + 2;
        for (int z = RADIO.getZ() + 1; z < BALUN.getZ(); z++)
            if (z != gap)
                GameTestUtil.placeWire(helper, new BlockPos(8, 2, z), RadiocraftBlocks.COAX_WIRE.get());
        GameTestUtil.placeWire(helper, new BlockPos(8, 2, gap), RadiocraftBlocks.COAX_WIRE.get());
    }

    private static void placeDipole(GameTestHelper helper) {
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
    }

    private static void assertRadioSeesMatchedDipole(GameTestHelper helper) {
        RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
        List<AntennaNetworkObject> antennas = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
        helper.assertTrue(antennas.size() == 1, "Radio sees " + antennas.size() + " antennas");
        helper.assertTrue(antennas.getFirst().getAntenna() != null, "Antenna not recognized yet");
        double swr = antennas.getFirst().getSWR(radio.getFrequency());
        helper.assertTrue(swr == 1.0D, "SWR at " + radio.getFrequency() + " Hz was " + swr);
    }

    /**
     * Radio and 20m dipole placed first, then coax with the closing piece in the middle. The radio should see exactly
     * one antenna, matched at SWR 1.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioFindsDipoleThroughCoax(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        placeDipole(helper);
        helper.startSequence()
                .thenIdle(2) // Let block entities load and create their network objects.
                .thenExecute(() -> placeCoaxRunClosingInMiddle(helper))
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenSucceed();
    }

    /** Coax laid end to end from the radio to the balun: the last piece touches the balun and merges the run. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioFindsDipoleWhenCoaxLaidEndToEnd(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        placeDipole(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> placeCoaxRun(helper))
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenSucceed();
    }

    /** Coax first, then the radio and balun placed onto its ends. Devices connect when their block entity loads. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioFindsDipoleWhenPlacedOntoExistingCoax(GameTestHelper helper) {
        placeCoaxRun(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
                    placeDipole(helper);
                })
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenSucceed();
    }

    /** Everything placed in the same tick, in the worst order: balun, coax, then the radio. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioFindsDipoleWhenAllPlacedAtOnce(GameTestHelper helper) {
        placeDipole(helper);
        placeCoaxRun(helper);
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.succeedWhen(() -> assertRadioSeesMatchedDipole(helper));
    }

    /** Cutting the coax in the middle splits the network, so the radio no longer sees the antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioLosesDipoleWhenCoaxIsCut(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        placeDipole(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> placeCoaxRun(helper))
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenExecute(() -> helper.setBlock(new BlockPos(8, 2, RADIO.getZ() + 2), Blocks.AIR))
                .thenWaitUntil(() -> {
                    int count = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas().size();
                    helper.assertTrue(count == 0, "Radio still sees " + count + " antennas after the coax was cut");
                })
                .thenSucceed();
    }

    /**
     * A powered radio with a matched antenna reports full static factor (SWR 1 means every bit of band noise gets
     * through); switching it off drops the factor back to 0.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void poweredRadioReportsStaticFactor(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        placeDipole(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> placeCoaxRun(helper))
                .thenWaitUntil(() -> assertRadioSeesMatchedDipole(helper))
                .thenExecute(() -> {
                    RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
                    helper.assertTrue(radio.getStaticVolume() == 0.0D, "Static factor should be 0 while off, was " + radio.getStaticVolume());
                    radio.toggle();
                })
                .thenWaitUntil(() -> {
                    RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
                    helper.assertTrue(radio.wasPowered(), "Client-visible power flag not set");
                    helper.assertTrue(radio.getStaticVolume() == 1.0D, "Static factor at SWR 1 should be 1, was " + radio.getStaticVolume());
                })
                .thenExecute(() -> GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class).toggle())
                .thenWaitUntil(() -> {
                    RadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, RadioBlockEntity.class);
                    helper.assertFalse(radio.wasPowered(), "Client-visible power flag not cleared");
                    helper.assertTrue(radio.getStaticVolume() == 0.0D, "Static factor should be 0 after power off, was " + radio.getStaticVolume());
                })
                .thenSucceed();
    }

    private static HFRadioBlockEntity reload(GameTestHelper helper, HFRadioBlockEntity radio) {
        CompoundTag tag = radio.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(radio.getBlockPos(), radio.getBlockState(), tag, helper.getLevel().registryAccess());
        if (!(loaded instanceof HFRadioBlockEntity copy)) {
            helper.fail("Reload produced " + loaded);
            return null;
        }
        copy.setLevel(helper.getLevel());
        return copy;
    }

    /** Tuning, mode and gain survive a save/load round trip. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void radioSettingsSurviveSaveAndLoad(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        radio.updateFrequency(25);
        radio.setCWEnabled(true);
        radio.setSpeakerGain(1.5F);

        HFRadioBlockEntity copy = reload(helper, radio);
        helper.assertTrue(copy.getFrequency() == radio.getFrequency(), "Frequency " + radio.getFrequency() + " reloaded as " + copy.getFrequency());
        helper.assertTrue(copy.getCWEnabled(), "CW mode was lost");
        helper.assertTrue(copy.getSpeakerGain() == 1.5F, "Speaker gain reloaded as " + copy.getSpeakerGain());
        helper.succeed();
    }

    /** A frequency that is outside the band (e.g. from an edited save) is reset to the bottom of the band. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void outOfBandFrequencyIsResetOnLoad(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        radio.setFrequency(7_100_000); // 40m, not 20m.

        HFRadioBlockEntity copy = reload(helper, radio);
        helper.assertTrue(copy.getFrequency() == copy.getBand().minFrequency(), "Expected reset to band bottom, got " + copy.getFrequency());
        helper.succeed();
    }

    /**
     * Network objects are not persisted, so after a world reload the radio's object is created fresh. The block entity
     * remembers it was on and switches the new object back on.
     */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void radioPowerStateSurvivesReload(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        radio.toggle();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(radio.wasPowered(), "Power flag not synced to the block entity yet"))
                .thenExecute(() -> {
                    HFRadioBlockEntity copy = reload(helper, radio);
                    BlockPos abs = helper.absolutePos(RADIO);
                    IBENetworks.removeObject(helper.getLevel(), abs); // Simulate the object being gone after a restart.
                    RadioNetworkObject fresh = (RadioNetworkObject) copy.getNetworkObject(helper.getLevel(), abs);
                    helper.assertTrue(fresh.isPowered, "Reloaded radio should still be switched on");
                })
                .thenSucceed();
    }

    /** A radio fresh out of the box is in SSB: power it and it listens and plays static, no button needed. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void freshRadioStartsInSsb(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        helper.assertTrue(radio.getSSBEnabled() && !radio.getCWEnabled(), "A new radio should come up in SSB");
        helper.assertFalse(radio.getVoiceReceiver().isReceiving(), "An unpowered radio should not listen");
        radio.toggle();
        helper.assertTrue(radio.getVoiceReceiver().isReceiving(), "A new radio should listen as soon as it is powered");
        helper.assertTrue(radio.shouldPlayStatic(), "A new powered radio should play static");
        helper.succeed();
    }

    /** Pressing PTT mutes the receiver, and the clients are told so their static can go quiet too. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void pttMutesTheReceiverAndIsSynced(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.HF_RADIO_20M.get());
        HFRadioBlockEntity radio = GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
        radio.toggle();
        helper.assertTrue(radio.getVoiceReceiver().isReceiving(), "Radio should listen before PTT");

        radio.setPTTDown(true);
        helper.assertTrue(radio.isReceiverMuted(), "Receiver should be muted while PTT is down");
        helper.assertFalse(radio.getVoiceReceiver().isReceiving(), "Voice receive should stop while PTT is down");
        helper.assertTrue(radio.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("pttDown"), "PTT state should be in the client sync tag");

        radio.setPTTDown(false);
        helper.assertFalse(radio.isReceiverMuted(), "Receiver should be live again after PTT");
        helper.assertTrue(radio.getVoiceReceiver().isReceiving(), "Voice receive should resume after PTT");
        helper.assertFalse(radio.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("pttDown"), "PTT release should be synced too");
        helper.succeed();
    }
}

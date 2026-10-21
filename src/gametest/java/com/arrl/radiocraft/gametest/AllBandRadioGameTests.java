package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * The all-band transceiver: band knob, SSB / CW / FM selection and a real contact after switching bands.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AllBandRadioGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 8);
    /** Frequency offset of its own (see TransmissionGameTests), so this contact is not heard by other tests. */
    private static final int F_ALL_BAND = 320;

    private static HFRadioAllBandBlockEntity allBand(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, HFRadioAllBandBlockEntity.class);
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void bandKnobSwitchesBandAndFrequency(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.ALL_BAND_RADIO.get());
        HFRadioAllBandBlockEntity radio = allBand(helper, RADIO);
        List<String> expected = List.of("80m", "40m", "20m", "10m", "2m");
        for(int i = 0; i < expected.size(); i++) {
            radio.setBandIndex(i);
            Band band = radio.getBand();
            helper.assertTrue(band != null && band.name().equals(expected.get(i)), "Knob position " + i + " gave " + (band == null ? "no band" : band.name()));
            helper.assertTrue(radio.getFrequency() == band.minFrequency(), "Frequency should go to the bottom of " + band.name() + ", was " + radio.getFrequency());
        }
        radio.setBandIndex(7);
        helper.assertTrue(radio.getBandIndex() == 4, "An out of range position should be ignored");

        radio.setBandIndex(1);
        CompoundTag saved = radio.saveWithoutMetadata(helper.getLevel().registryAccess());
        HFRadioAllBandBlockEntity copy = new HFRadioAllBandBlockEntity(helper.absolutePos(RADIO.east()), radio.getBlockState());
        copy.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(copy.getBandIndex() == 1, "The band should survive a save, got position " + copy.getBandIndex());
        helper.succeed();
    }

    /**
     * On 2m the frequency steps like on any other band (it used to stick at 144.000: 144.001 MHz does not fit a float
     * exactly, so the off-grid check threw every step away), and the stepped frequency survives a save.
     */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void tuningWorksOn2m(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.ALL_BAND_RADIO.get());
        HFRadioAllBandBlockEntity radio = allBand(helper, RADIO);
        radio.setBandIndex(4);
        radio.updateFrequency(3);
        helper.assertTrue(Math.abs(radio.getFrequency() - 144_003_000.0D) < 20.0D, "Three steps up should be 144.003 MHz, was " + radio.getFrequency());
        radio.updateFrequency(-1);
        helper.assertTrue(Math.abs(radio.getFrequency() - 144_002_000.0D) < 20.0D, "One step down should be 144.002 MHz, was " + radio.getFrequency());

        CompoundTag saved = radio.saveWithoutMetadata(helper.getLevel().registryAccess());
        HFRadioAllBandBlockEntity copy = new HFRadioAllBandBlockEntity(helper.absolutePos(RADIO.east()), radio.getBlockState());
        copy.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(copy.getFrequency() == radio.getFrequency(), "The 2m frequency should survive a save, got " + copy.getFrequency());

        radio.updateFrequency(1_000_000);
        helper.assertTrue(radio.getFrequency() == radio.getBand().maxFrequency(), "Should stop at the top of 2m, was " + radio.getFrequency());
        helper.succeed();
    }

    /** One mode at a time; FM is voice without the static. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void modesAreExclusive(GameTestHelper helper) {
        helper.setBlock(RADIO, RadiocraftBlocks.ALL_BAND_RADIO.get());
        HFRadioAllBandBlockEntity radio = allBand(helper, RADIO);

        radio.setFMEnabled(true);
        helper.assertTrue(radio.getFMEnabled() && radio.getSSBEnabled() && !radio.getCWEnabled() && !radio.isSSBSelected(), "FM: voice on, SSB button out, CW off");
        helper.assertFalse(radio.shouldPlayStatic(), "FM is squelched");

        radio.setCWEnabled(true);
        helper.assertTrue(radio.getCWEnabled() && !radio.getFMEnabled() && !radio.getSSBEnabled(), "CW releases FM and voice");
        helper.assertTrue(radio.shouldPlayStatic(), "CW plays static");

        radio.setSSBEnabled(true);
        helper.assertTrue(radio.isSSBSelected() && !radio.getCWEnabled() && !radio.getFMEnabled(), "SSB releases CW");

        radio.setFMEnabled(true);
        radio.setSSBEnabled(true);
        helper.assertTrue(radio.isSSBSelected() && !radio.getFMEnabled(), "SSB releases FM");

        radio.setFMEnabled(true);
        CompoundTag saved = radio.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getBoolean("fmEnabled"), "FM should be saved");
        helper.succeed();
    }

    /** Switched on and turned to 20m, the all-band talks to a 20m radio over a dipole each. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void allBandWorksOn20m(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        buildStation(helper, RADIO_B, BALUN_B);
        helper.setBlock(RADIO_A, RadiocraftBlocks.ALL_BAND_RADIO.get()); // Swap station A's radio for an all-band.
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (BlockPos pos : List.of(RADIO_A, RADIO_B)) {
                        var antennas = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).getAntennas();
                        helper.assertTrue(antennas.size() == 1 && antennas.getFirst().getAntenna() != null, "Station at " + pos.toShortString() + " not ready");
                    }
                })
                .thenExecute(() -> {
                    allBand(helper, RADIO_A).setBandIndex(2); // 20m
                    setupSsb(helper, RADIO_A, F_ALL_BAND);
                    setupSsb(helper, RADIO_B, F_ALL_BAND);
                    helper.assertTrue(GameTestUtil.networkObject(helper, RADIO_A, RadioNetworkObject.class).isPowered, "All-band did not power on");
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, EXPECTED_VOICE_STRENGTH);
                })
                .thenSucceed();
    }
}

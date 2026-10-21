package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.voice.Detuning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Signals a little off frequency ({@link Detuning}): SSB is heard weaker up to 10 kHz off, FM only up to 2 kHz, and
 * from 3 kHz an FM signal stays behind the squelch but splatters. These run in a batch of their own, so they have the
 * 20m band to themselves; each test is 40 kHz from the next.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DetuningGameTests {

    private static final String BATCH = "detuning";
    private static final int F_SSB_NEAR = 20;
    private static final int F_SSB_FAR = 60;
    private static final int F_FM_NEAR = 100;
    private static final int F_FM_SPLATTER = 140;

    private static HFRadioAllBandBlockEntity allBand(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, HFRadioAllBandBlockEntity.class);
    }

    /** Two 20m stations; with {@code allBand}, both radios are all-bands turned to 20m. */
    private static GameTestSequence stations(GameTestHelper helper, boolean allBand) {
        buildStation(helper, RADIO_A, BALUN_A);
        buildStation(helper, RADIO_B, BALUN_B);
        if(allBand) {
            helper.setBlock(RADIO_A, RadiocraftBlocks.ALL_BAND_RADIO.get());
            helper.setBlock(RADIO_B, RadiocraftBlocks.ALL_BAND_RADIO.get());
        }
        return helper.startSequence()
                .thenWaitUntil(() -> {
                    for (BlockPos pos : List.of(RADIO_A, RADIO_B)) {
                        var antennas = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).getAntennas();
                        helper.assertTrue(antennas.size() == 1 && antennas.getFirst().getAntenna() != null, "Station at " + pos.toShortString() + " not ready");
                    }
                })
                .thenExecute(() -> {
                    if(allBand) {
                        allBand(helper, RADIO_A).setBandIndex(2); // 20m
                        allBand(helper, RADIO_B).setBandIndex(2);
                    }
                });
    }

    /** SSB 2 kHz off is still heard, a little weaker. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void ssbOffFrequencyIsHeardWeaker(GameTestHelper helper) {
        stations(helper, false)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_SSB_NEAR);
                    setupSsb(helper, RADIO_B, F_SSB_NEAR + 2);
                    double expected = EXPECTED_VOICE_STRENGTH * radio(helper, RADIO_B).getDetuning(radio(helper, RADIO_A).getFrequency()).strength();
                    helper.assertTrue(expected < EXPECTED_VOICE_STRENGTH, "2 kHz off should be weaker");
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, expected);
                })
                .thenSucceed();
    }

    /** SSB more than 10 kHz off is not heard at all. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void ssbFarOffFrequencyHearsNothing(GameTestHelper helper) {
        stations(helper, false)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_SSB_FAR);
                    setupSsb(helper, RADIO_B, F_SSB_FAR + 11);
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** FM 2 kHz off sounds as usual, a little weaker. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void fmTwoKilohertzOffIsHeardWeaker(GameTestHelper helper) {
        stations(helper, true)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_FM_NEAR);
                    setupSsb(helper, RADIO_B, F_FM_NEAR - 2);
                    allBand(helper, RADIO_A).setFMEnabled(true);
                    allBand(helper, RADIO_B).setFMEnabled(true);
                    Detuning.Effect effect = radio(helper, RADIO_B).getDetuning(radio(helper, RADIO_A).getFrequency());
                    helper.assertTrue(effect.audible() && effect.pitchShiftHz() == 0.0D, "FM 2 kHz off should be heard, unshifted");
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, EXPECTED_VOICE_STRENGTH * effect.strength());
                })
                .thenSucceed();
    }

    /** FM 5 kHz off keeps the squelch shut but splatters onto the receiver. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void fmFiveKilohertzOffOnlySplatters(GameTestHelper helper) {
        stations(helper, true)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_FM_SPLATTER);
                    setupSsb(helper, RADIO_B, F_FM_SPLATTER + 5);
                    allBand(helper, RADIO_A).setFMEnabled(true);
                    allBand(helper, RADIO_B).setFMEnabled(true);
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                    HFRadioAllBandBlockEntity b = allBand(helper, RADIO_B);
                    helper.assertTrue(b.getAdjacentQrmGameTime() == helper.getLevel().getGameTime(), "The signal 5 kHz off should splatter");
                    helper.assertTrue(b.getAdjacentQrm() > 0.0D, "Splatter level should be above 0, was " + b.getAdjacentQrm());
                })
                .thenSucceed();
    }
}

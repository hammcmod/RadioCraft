package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AutoTunerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.TunerMath;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The automatic antenna tuner on the coax between a radio and a 2+2 dipole: SWR 4 on 20m (in its range), SWR 10 on
 * 40m (past it). Power draw is off on the GameTest server, so it always runs. Needs the generated block tags
 * ({@code ./gradlew runData}) to join the coax.
 * <pre>
 *   z=1  radio
 *   z=2  coax
 *   z=3  automatic tuner
 *   z=4  coax
 *   z=5  ---balun---
 * </pre>
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AutoTunerGameTests {

    private static final BlockPos RADIO = new BlockPos(8, 2, 1);
    private static final BlockPos TUNER = new BlockPos(8, 2, 3);
    private static final BlockPos BALUN = new BlockPos(8, 2, 5);

    private static void build(GameTestHelper helper, Block radio) {
        helper.setBlock(RADIO, radio);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(2));
        GameTestUtil.wire(helper, BALUN, BALUN.east(2));
        GameTestUtil.placeWire(helper, RADIO.south(), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(TUNER, RadiocraftBlocks.AUTO_TUNER.get());
        GameTestUtil.placeWire(helper, TUNER.south(), RadiocraftBlocks.COAX_WIRE.get());
    }

    private static AutoTunerNetworkObject tuner(GameTestHelper helper) {
        return GameTestUtil.networkObject(helper, TUNER, AutoTunerNetworkObject.class);
    }

    private static HFRadioBlockEntity radio(GameTestHelper helper) {
        return GameTestUtil.blockEntity(helper, RADIO, HFRadioBlockEntity.class);
    }

    private static void antennaReady(GameTestHelper helper) {
        var antennas = GameTestUtil.networkObject(helper, RADIO, RadioNetworkObject.class).getAntennas();
        helper.assertTrue(antennas.size() == 1 && antennas.getFirst().getAntenna() != null, "Antenna not recognized yet");
    }

    /**
     * Switched on, the radio sees SWR 3 while the tuner clicks for a second, then SWR 1. A move of less than 1 MHz
     * within the band does not make it tune again.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void tunesInASecondAndHoldsWithinAMegahertz(GameTestHelper helper) {
        build(helper, RadiocraftBlocks.HF_RADIO_20M.get());
        long[] tuningFrom = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> antennaReady(helper))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO, 100))
                .thenWaitUntil(() -> {
                    helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.TUNING, "Tuner state " + tuner(helper).getState());
                    helper.assertTrue(radio(helper).getAntennaSWR() == TunerMath.TUNING_SWR, "Radio sees " + radio(helper).getAntennaSWR() + " while tuning");
                    tuningFrom[0] = helper.getLevel().getGameTime();
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.TUNED, "Tuner state " + tuner(helper).getState());
                    helper.assertTrue(radio(helper).getAntennaSWR() == 1.0D, "Radio sees " + radio(helper).getAntennaSWR() + " once tuned");
                })
                .thenExecute(() -> {
                    long took = helper.getLevel().getGameTime() - tuningFrom[0];
                    helper.assertTrue(took >= TunerMath.TUNE_TICKS - 2 && took <= TunerMath.TUNE_TICKS + 3, "Tuning took " + took + " ticks");
                    helper.assertFalse(radio(helper).isTuneFailed(), "Radio shows TUNE FAIL");
                    radio(helper).updateFrequency(200); // 200 kHz up the band.
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.TUNED, "A 200 kHz move made it tune again: " + tuner(helper).getState());
                    helper.assertTrue(radio(helper).getAntennaSWR() == 1.0D, "Radio sees " + radio(helper).getAntennaSWR());
                })
                .thenSucceed();
    }

    /** SWR 10 is past the tuner's range: it hunts for a while, never tunes, and the radio shows TUNE FAIL and sees SWR 10. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400)
    public static void givesUpOnAFarOffAntenna(GameTestHelper helper) {
        build(helper, RadiocraftBlocks.HF_RADIO_40M.get());
        helper.startSequence()
                .thenWaitUntil(() -> antennaReady(helper))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO, 50))
                .thenWaitUntil(() -> helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.SEARCHING, "Tuner state " + tuner(helper).getState()))
                .thenIdle(TunerMath.FAIL_SEARCH_TICKS / 2)
                .thenExecute(() -> helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.SEARCHING, "Gave up too soon"))
                .thenWaitUntil(() -> {
                    helper.assertTrue(tuner(helper).getState() == AutoTunerNetworkObject.State.FAILED, "Tuner state " + tuner(helper).getState());
                    helper.assertTrue(radio(helper).isTuneFailed(), "Radio does not show TUNE FAIL");
                    helper.assertTrue(radio(helper).getAntennaSWR() == 10.0D, "Radio sees " + radio(helper).getAntennaSWR());
                })
                .thenSucceed();
    }
}

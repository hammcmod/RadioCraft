package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.SWRHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The antenna tuner on the coax between a radio and an antenna that is off resonance.
 * <pre>
 *   z=1  radio A
 *   z=2  coax
 *   z=3  tuner (or coax in the control tests)
 *   z=4  coax
 *   z=5  ---balun A---   short dipole, 3+3 on 20m: SWR 3 on its own
 *   z=12 ---balun B---   matched 5+5 dipole
 *   z=13 coax
 *   z=14 radio B
 * </pre>
 * The baluns are 7 blocks apart, so a voice packet arrives at (1 - 7/300) times the match efficiency: 0.75 for the
 * bare short dipole, 1 - 0.25 * 0.25 = 0.9375 through the tuner (config defaults: max SWR 6, loss 0.25).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AntennaTunerGameTests {

    private static final BlockPos RADIO_A = new BlockPos(8, 2, 1);
    private static final BlockPos TUNER = new BlockPos(8, 2, 3);
    private static final BlockPos BALUN_A = new BlockPos(8, 2, 5);
    private static final BlockPos BALUN_B = new BlockPos(8, 2, 12);
    private static final BlockPos RADIO_B = new BlockPos(8, 2, 14);

    // Frequencies (kHz above the band bottom), see TransmissionGameTests for why each test needs its own.
    private static final int F_TUNED_VOICE = 290;
    private static final int F_UNTUNED_VOICE = 305;
    private static final int F_SWR = 320;
    private static final int F_OUT_OF_RANGE = 50; // On 40m (7.05 MHz, quarter wave 10.6 -> 11 blocks); nobody else uses 40m.
    private static final int F_REMOVED = 335;

    private static final double BARE_EFFICIENCY = SWRHelper.getEfficiencyMultiplier(3.0);  // 0.75
    private static final double TUNED_EFFICIENCY = 1.0 - (1.0 - BARE_EFFICIENCY) * 0.25;   // 0.9375
    private static final double PATH = 1.0 - 7.0 / 300.0;

    /** Radio A, coax with a tuner (or plain coax) in the middle, and a 3+3 dipole at balun A. */
    private static void buildShortStation(GameTestHelper helper, Block radio, boolean withTuner, int armLength) {
        helper.setBlock(RADIO_A, radio);
        helper.setBlock(BALUN_A, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN_A, BALUN_A.west(armLength));
        GameTestUtil.wire(helper, BALUN_A, BALUN_A.east(armLength));
        GameTestUtil.placeWire(helper, RADIO_A.south(), RadiocraftBlocks.COAX_WIRE.get());
        if (withTuner)
            helper.setBlock(TUNER, RadiocraftBlocks.ANTENNA_TUNER.get());
        else
            GameTestUtil.placeWire(helper, TUNER, RadiocraftBlocks.COAX_WIRE.get());
        GameTestUtil.placeWire(helper, TUNER.south(), RadiocraftBlocks.COAX_WIRE.get());
    }

    private static void buildMatchedStation(GameTestHelper helper) {
        TransmissionGameTests.buildStation(helper, RADIO_B, BALUN_B);
    }

    private static AntennaNetworkObject antennaOf(GameTestHelper helper, BlockPos radio) {
        var antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
        helper.assertTrue(antennas.size() == 1, "Radio at " + radio.toShortString() + " sees " + antennas.size() + " antennas");
        helper.assertTrue(antennas.getFirst().getAntenna() != null, "Antenna of radio at " + radio.toShortString() + " not recognized yet");
        return antennas.getFirst();
    }

    private static void assertClose(GameTestHelper helper, String what, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < TransmissionGameTests.TOLERANCE, what + " was " + actual + ", expected " + expected);
    }

    /** Through the tuner the radio sees SWR 1 while the antenna itself is still SWR 3, and the tuner's readout agrees. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void tunerMatchesShortDipole(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_20M.get(), true, 3);
        helper.startSequence()
                .thenWaitUntil(() -> antennaOf(helper, RADIO_A))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO_A, F_SWR))
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = antennaOf(helper, RADIO_A);
                    helper.assertTrue(antenna.hasTuner(), "Antenna does not see the tuner");
                    assertClose(helper, "raw SWR", antenna.getRawSWR(TransmissionGameTests.radio(helper, RADIO_A).getFrequency()), 3.0);
                    assertClose(helper, "tuned SWR", antenna.getSWR(TransmissionGameTests.radio(helper, RADIO_A).getFrequency()), 1.0);
                    assertClose(helper, "match efficiency", antenna.getMatchEfficiency(TransmissionGameTests.radio(helper, RADIO_A).getFrequency()), TUNED_EFFICIENCY);
                    assertClose(helper, "SWR the radio measured", TransmissionGameTests.radio(helper, RADIO_A).getAntennaSWR(), 1.0);

                    AntennaTunerBlockEntity.Reading reading = GameTestUtil.blockEntity(helper, TUNER, AntennaTunerBlockEntity.class).read();
                    helper.assertTrue(reading.status() == AntennaTunerBlockEntity.STATUS_MATCHED, "Tuner readout status " + reading.status());
                    assertClose(helper, "readout antenna SWR", reading.rawSwr(), 3.0);
                    assertClose(helper, "readout radio SWR", reading.tunedSwr(), 1.0);
                })
                .thenSucceed();
    }

    /** Control: the same short dipole without a tuner is SWR 3 at the radio. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void shortDipoleWithoutTunerIsMismatched(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_20M.get(), false, 3);
        helper.startSequence()
                .thenWaitUntil(() -> antennaOf(helper, RADIO_A))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO_A, F_SWR))
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = antennaOf(helper, RADIO_A);
                    helper.assertFalse(antenna.hasTuner(), "No tuner was placed");
                    assertClose(helper, "SWR the radio measured", TransmissionGameTests.radio(helper, RADIO_A).getAntennaSWR(), 3.0);
                    assertClose(helper, "match efficiency", antenna.getMatchEfficiency(TransmissionGameTests.radio(helper, RADIO_A).getFrequency()), BARE_EFFICIENCY);
                })
                .thenSucceed();
    }

    /** A voice packet from the tuned short dipole arrives stronger than from the bare one: path loss times 0.9375. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void tunedStationIsHeardLouder(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_20M.get(), true, 3);
        buildMatchedStation(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    antennaOf(helper, RADIO_A);
                    antennaOf(helper, RADIO_B);
                })
                .thenExecute(() -> {
                    TransmissionGameTests.setupSsb(helper, RADIO_A, F_TUNED_VOICE);
                    TransmissionGameTests.setupSsb(helper, RADIO_B, F_TUNED_VOICE);
                    TransmissionGameTests.speakInto(helper, RADIO_A);
                    TransmissionGameTests.assertHeard(helper, RADIO_B, PATH * TUNED_EFFICIENCY);
                })
                .thenSucceed();
    }

    /** Control: without the tuner the short dipole only delivers path loss times 0.75. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void untunedStationIsHeardQuieter(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_20M.get(), false, 3);
        buildMatchedStation(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    antennaOf(helper, RADIO_A);
                    antennaOf(helper, RADIO_B);
                })
                .thenExecute(() -> {
                    TransmissionGameTests.setupSsb(helper, RADIO_A, F_UNTUNED_VOICE);
                    TransmissionGameTests.setupSsb(helper, RADIO_B, F_UNTUNED_VOICE);
                    TransmissionGameTests.speakInto(helper, RADIO_A);
                    TransmissionGameTests.assertHeard(helper, RADIO_B, PATH * BARE_EFFICIENCY);
                })
                .thenSucceed();
    }

    /** A 2+2 dipole on 40m is SWR 10 (18 blocks short), past the tuner's range: the radio sees 10 and the readout says so. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void tunerCannotMatchFarOffAntenna(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_40M.get(), true, 2);
        helper.startSequence()
                .thenWaitUntil(() -> antennaOf(helper, RADIO_A))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO_A, F_OUT_OF_RANGE))
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = antennaOf(helper, RADIO_A);
                    float frequency = TransmissionGameTests.radio(helper, RADIO_A).getFrequency();
                    helper.assertTrue(antenna.hasTuner(), "Antenna does not see the tuner");
                    assertClose(helper, "raw SWR", antenna.getRawSWR(frequency), 10.0);
                    assertClose(helper, "SWR the radio measured", TransmissionGameTests.radio(helper, RADIO_A).getAntennaSWR(), 10.0);
                    assertClose(helper, "match efficiency", antenna.getMatchEfficiency(frequency), SWRHelper.getEfficiencyMultiplier(10.0));
                    AntennaTunerBlockEntity.Reading reading = GameTestUtil.blockEntity(helper, TUNER, AntennaTunerBlockEntity.class).read();
                    helper.assertTrue(reading.status() == AntennaTunerBlockEntity.STATUS_OUT_OF_RANGE, "Tuner readout status " + reading.status());
                })
                .thenSucceed();
    }

    /** Breaking the tuner (and bridging the gap with coax) puts the radio back on the antenna's own SWR. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 400)
    public static void removingTunerRestoresRawSwr(GameTestHelper helper) {
        buildShortStation(helper, RadiocraftBlocks.HF_RADIO_20M.get(), true, 3);
        helper.startSequence()
                .thenWaitUntil(() -> antennaOf(helper, RADIO_A))
                .thenExecute(() -> TransmissionGameTests.setupSsb(helper, RADIO_A, F_REMOVED))
                .thenWaitUntil(() -> assertClose(helper, "SWR with tuner", TransmissionGameTests.radio(helper, RADIO_A).getAntennaSWR(), 1.0))
                .thenExecute(() -> {
                    helper.setBlock(TUNER, Blocks.AIR);
                    GameTestUtil.placeWire(helper, TUNER, RadiocraftBlocks.COAX_WIRE.get());
                })
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = antennaOf(helper, RADIO_A);
                    helper.assertFalse(antenna.hasTuner(), "Antenna still sees a tuner");
                    assertClose(helper, "SWR without tuner", TransmissionGameTests.radio(helper, RADIO_A).getAntennaSWR(), 3.0);
                })
                .thenSucceed();
    }

    /** A tuner with nothing on its coax reads "no radio" rather than crashing. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
    public static void loneTunerReadsNoRadio(GameTestHelper helper) {
        helper.setBlock(TUNER, RadiocraftBlocks.ANTENNA_TUNER.get());
        helper.succeedWhen(() -> {
            AntennaTunerBlockEntity.Reading reading = GameTestUtil.blockEntity(helper, TUNER, AntennaTunerBlockEntity.class).read();
            helper.assertTrue(reading.status() == AntennaTunerBlockEntity.STATUS_NO_RADIO, "Tuner readout status " + reading.status());
        });
    }
}

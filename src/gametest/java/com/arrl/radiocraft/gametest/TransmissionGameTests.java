package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Two complete 20m stations in one test area, talking to each other through the HF antenna network: radio, coax,
 * 1:1 balun and a matched dipole on each side.
 * <pre>
 *   z=1  radio A        z=14 radio B
 *   z=2  coax           z=13 coax
 *   z=3  ---balun A---  z=12 ---balun B---   (dipole arms 5 blocks west and east of each balun)
 * </pre>
 * The baluns are 9 blocks apart, well inside the 20m line of sight range (300 by default), so a voice packet should
 * arrive at strength 1 - 9/300 = 0.97 (both dipoles have 0 dBi gain and SWR 1 at 14.1-14.22 MHz). CW counts distance at
 * 1/1.5, giving 1 - 6/300 = 0.98.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransmissionGameTests {

    static final BlockPos RADIO_A = new BlockPos(8, 2, 1);
    static final BlockPos BALUN_A = new BlockPos(8, 2, 3);
    static final BlockPos RADIO_B = new BlockPos(8, 2, 14);
    static final BlockPos BALUN_B = new BlockPos(8, 2, 12);

    /*
     * Every test tunes its two radios to a frequency of its own (band bottom + N kHz). The GameTest server runs all
     * tests at the same time in neighbouring plots, and antennas from every plot share the one HF antenna network, so
     * two tests on the same frequency would hear each other. The dipole is matched (SWR 1) across the whole 20m band.
     */
    private static final int F_VOICE = 100;
    private static final int F_BOTH_WAYS = 110;
    private static final int F_OTHER_FREQ = 120;
    private static final int F_UNPOWERED = 130;
    private static final int F_CW_MODE_VOICE = 140;
    private static final int F_NO_ANTENNA = 150;
    private static final int F_CW = 160;
    private static final int F_CW_SSB = 170;
    private static final int F_CW_POWER = 180;
    private static final int F_PARROT_VOICE = 200;
    private static final int F_PARROT_CW = 210;
    private static final int F_PARROT_OFF = 220;
    // 230-260 are used by ParrotControllerGameTests, 270 by ComparatorGameTests.
    static final double EXPECTED_VOICE_STRENGTH = 1.0D - 9.0D / 300.0D;
    static final double EXPECTED_CW_STRENGTH = 1.0D - (9.0D / 1.5D) / 300.0D;
    static final double TOLERANCE = 1e-6;

    private static final UUID OPERATOR = UUID.nameUUIDFromBytes("gametest-operator".getBytes());

    /** Radio, one coax block and a balun with a 5+5 dipole running east-west. */
    static void buildStation(GameTestHelper helper, BlockPos radio, BlockPos balun) {
        helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
        helper.setBlock(balun, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, balun, balun.west(5));
        GameTestUtil.wire(helper, balun, balun.east(5));
        int step = Integer.signum(balun.getZ() - radio.getZ());
        for (BlockPos p = radio.relative(Direction.SOUTH, step); !p.equals(balun); p = p.relative(Direction.SOUTH, step))
            GameTestUtil.placeWire(helper, p, RadiocraftBlocks.COAX_WIRE.get());
    }

    private static void buildBothStations(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        buildStation(helper, RADIO_B, BALUN_B);
    }

    static HFRadioBlockEntity radio(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, HFRadioBlockEntity.class);
    }

    /** Both radios see exactly one antenna that has been recognized as a matched dipole. */
    private static void assertStationsReady(GameTestHelper helper) {
        for (BlockPos pos : List.of(RADIO_A, RADIO_B)) {
            var antennas = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).getAntennas();
            helper.assertTrue(antennas.size() == 1, "Radio at " + pos.toShortString() + " sees " + antennas.size() + " antennas");
            helper.assertTrue(antennas.getFirst().getAntenna() != null, "Antenna of radio at " + pos.toShortString() + " not recognized yet");
            double swr = antennas.getFirst().getSWR(radio(helper, pos).getFrequency());
            helper.assertTrue(swr == 1.0D, "SWR for radio at " + pos.toShortString() + " was " + swr);
        }
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        HFRadioBlockEntity radio = radio(helper, pos);
        if (!GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
            radio.toggle();
        helper.assertTrue(GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered, "Radio at " + pos.toShortString() + " did not power on");
    }

    static void powerOff(GameTestHelper helper, BlockPos pos) {
        if (GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered)
            radio(helper, pos).toggle();
        helper.assertFalse(GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).isPowered, "Radio at " + pos.toShortString() + " did not power off");
    }

    /** Powers the radio up in SSB on {@code steps} kHz above the band bottom. */
    static void setupSsb(GameTestHelper helper, BlockPos pos, int steps) {
        HFRadioBlockEntity radio = radio(helper, pos);
        radio.updateFrequency(steps);
        radio.setSSBEnabled(true);
        powerOn(helper, pos);
    }

    /** Powers the radio up in CW on {@code steps} kHz above the band bottom. */
    static void setupCw(GameTestHelper helper, BlockPos pos, int steps) {
        HFRadioBlockEntity radio = radio(helper, pos);
        radio.updateFrequency(steps);
        radio.setCWEnabled(true);
        powerOn(helper, pos);
    }

    /** One frame of a tone, like Simple Voice Chat would hand the transmitter. */
    private static short[] toneFrame() {
        short[] audio = new short[960];
        for (int i = 0; i < audio.length; i++)
            audio[i] = (short) (Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
        return audio;
    }

    /** Transmits one voice frame from the radio at pos, the way the voice plugin does once PTT is down. */
    static void speakInto(GameTestHelper helper, BlockPos pos) {
        HFRadioBlockEntity radio = radio(helper, pos);
        radio.setPTTDown(true);
        helper.assertTrue(radio.canTransmitVoice(), "Radio at " + pos.toShortString() + " cannot transmit voice");
        radio.acceptVoicePacket(() -> helper.getLevel(), toneFrame(), OPERATOR);
        radio.setPTTDown(false);
    }

    static void assertHeard(GameTestHelper helper, BlockPos pos, double expectedStrength) {
        HFRadioBlockEntity radio = radio(helper, pos);
        helper.assertTrue(radio.getLastSignalGameTime() == helper.getLevel().getGameTime(),
                "Radio at " + pos.toShortString() + " did not receive anything this tick (last signal tick " + radio.getLastSignalGameTime() + ")");
        helper.assertTrue(Math.abs(radio.getLastSignalStrength() - expectedStrength) < TOLERANCE,
                "Radio at " + pos.toShortString() + " heard strength " + radio.getLastSignalStrength() + ", expected " + expectedStrength);
    }

    static void assertHeardNothing(GameTestHelper helper, BlockPos pos) {
        HFRadioBlockEntity radio = radio(helper, pos);
        helper.assertTrue(radio.getLastSignalGameTime() < 0, "Radio at " + pos.toShortString() + " heard something at strength " + radio.getLastSignalStrength());
    }

    /** Builds both stations and waits until both antennas are recognized and linked. */
    static GameTestSequence stationsUp(GameTestHelper helper) {
        buildBothStations(helper);
        return helper.startSequence().thenWaitUntil(() -> assertStationsReady(helper));
    }

    /** SSB voice from A reaches B on the same frequency at the strength the propagation model predicts. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void voiceReachesRadioOnSameFrequency(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_VOICE);
                    setupSsb(helper, RADIO_B, F_VOICE);
                    helper.assertTrue(radio(helper, RADIO_B).getVoiceReceiver().isReceiving(), "B is not listening");
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, EXPECTED_VOICE_STRENGTH);
                    assertHeardNothing(helper, RADIO_A); // A transmitter does not hear itself.
                })
                .thenSucceed();
    }

    /** Both ways: after A talks to B, B talks back to A at the same strength. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void voiceWorksInBothDirections(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_BOTH_WAYS);
                    setupSsb(helper, RADIO_B, F_BOTH_WAYS);
                    speakInto(helper, RADIO_B);
                    assertHeard(helper, RADIO_A, EXPECTED_VOICE_STRENGTH);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** A radio tuned 5 kHz away hears nothing. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioOnAnotherFrequencyHearsNothing(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_OTHER_FREQ);
                    setupSsb(helper, RADIO_B, F_OTHER_FREQ);
                    radio(helper, RADIO_B).updateFrequency(5); // 5 kHz above A
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** A radio that is switched off hears nothing, even in SSB mode on the right frequency. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void unpoweredRadioHearsNothing(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_UNPOWERED);
                    HFRadioBlockEntity b = radio(helper, RADIO_B);
                    b.updateFrequency(F_UNPOWERED);
                    b.setSSBEnabled(true);
                    helper.assertFalse(b.getVoiceReceiver().isReceiving(), "An unpowered radio should not be listening");
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** A radio in CW mode does not play voice. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void radioInCwModeDoesNotHearVoice(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_CW_MODE_VOICE);
                    setupCw(helper, RADIO_B, F_CW_MODE_VOICE);
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** Nothing gets out while the transmitter has no antenna: the coax is cut before A speaks. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void noAntennaMeansNoTransmission(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_NO_ANTENNA);
                    setupSsb(helper, RADIO_B, F_NO_ANTENNA);
                    helper.setBlock(RADIO_A.south(), Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, RADIO_A, RadioNetworkObject.class).getAntennas().isEmpty(), "A still has an antenna"))
                .thenExecute(() -> {
                    speakInto(helper, RADIO_A);
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    /** CW keyed at A reaches B in CW mode on the same frequency, at the CW strength (1.5x the voice range). */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void cwReachesRadioOnSameFrequency(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupCw(helper, RADIO_A, F_CW);
                    setupCw(helper, RADIO_B, F_CW);
                    HFRadioBlockEntity a = radio(helper, RADIO_A);
                    helper.assertTrue(a.canTransmitCW(), "A cannot key CW");
                    boolean[] dit = new boolean[CWBuffer.BUFFER_LENGTH];
                    dit[0] = dit[1] = dit[2] = true;
                    a.transmitMorse(List.of(new CWBuffer(0, dit)));
                    assertHeard(helper, RADIO_B, EXPECTED_CW_STRENGTH);
                    assertHeardNothing(helper, RADIO_A);
                })
                .thenSucceed();
    }

    /** CW is only heard by a radio in CW mode: B in SSB stays silent. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void cwIsNotHeardInSsbMode(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupCw(helper, RADIO_A, F_CW_SSB);
                    setupSsb(helper, RADIO_B, F_CW_SSB);
                    radio(helper, RADIO_A).transmitMorse(List.of(new CWBuffer(0, new boolean[] { true, true, true })));
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }

    private static final int PARROT_DELAY = 10;

    /** B set as a parrot echoes A's voice back after the delay, so A hears its own transmission at path strength. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void parrotEchoesVoiceAfterDelay(GameTestHelper helper) {
        long[] spokeAt = new long[1];
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_PARROT_VOICE);
                    setupSsb(helper, RADIO_B, F_PARROT_VOICE);
                    radio(helper, RADIO_B).setParrotDelay(PARROT_DELAY);
                    spokeAt[0] = helper.getLevel().getGameTime();
                    speakInto(helper, RADIO_A);
                    assertHeard(helper, RADIO_B, EXPECTED_VOICE_STRENGTH);
                    assertHeardNothing(helper, RADIO_A);
                })
                .thenExecuteAfter(PARROT_DELAY / 2, () -> assertHeardNothing(helper, RADIO_A)) // Not yet: the parrot waits.
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_A).getLastSignalGameTime() >= 0, "A has not heard the echo yet"))
                .thenExecute(() -> {
                    HFRadioBlockEntity a = radio(helper, RADIO_A);
                    long waited = a.getLastSignalGameTime() - spokeAt[0];
                    helper.assertTrue(waited >= PARROT_DELAY, "Echo came back after " + waited + " ticks, expected at least " + PARROT_DELAY);
                    helper.assertTrue(Math.abs(a.getLastSignalStrength() - EXPECTED_VOICE_STRENGTH) < TOLERANCE,
                            "Echo strength " + a.getLastSignalStrength() + ", expected " + EXPECTED_VOICE_STRENGTH);
                })
                .thenSucceed();
    }

    /** The same for morse: A keys, parrot B re-keys it after the delay, A hears it at CW path strength. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void parrotEchoesCwAfterDelay(GameTestHelper helper) {
        long[] keyedAt = new long[1];
        stationsUp(helper)
                .thenExecute(() -> {
                    setupCw(helper, RADIO_A, F_PARROT_CW);
                    setupCw(helper, RADIO_B, F_PARROT_CW);
                    radio(helper, RADIO_B).setParrotDelay(PARROT_DELAY);
                    keyedAt[0] = helper.getLevel().getGameTime();
                    radio(helper, RADIO_A).transmitMorse(List.of(new CWBuffer(0, new boolean[] { true, true, true })));
                    assertHeard(helper, RADIO_B, EXPECTED_CW_STRENGTH);
                    assertHeardNothing(helper, RADIO_A);
                })
                .thenExecuteAfter(PARROT_DELAY / 2, () -> assertHeardNothing(helper, RADIO_A))
                .thenWaitUntil(() -> helper.assertTrue(radio(helper, RADIO_A).getLastSignalGameTime() >= 0, "A has not heard the echo yet"))
                .thenExecute(() -> {
                    HFRadioBlockEntity a = radio(helper, RADIO_A);
                    long waited = a.getLastSignalGameTime() - keyedAt[0];
                    helper.assertTrue(waited >= PARROT_DELAY, "Echo came back after " + waited + " ticks, expected at least " + PARROT_DELAY);
                    helper.assertTrue(Math.abs(a.getLastSignalStrength() - EXPECTED_CW_STRENGTH) < TOLERANCE,
                            "Echo strength " + a.getLastSignalStrength() + ", expected " + EXPECTED_CW_STRENGTH);
                })
                .thenSucceed();
    }

    /** Switching the parrot off drops anything still waiting in its queue. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void parrotOffDropsQueuedEcho(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_PARROT_OFF);
                    setupSsb(helper, RADIO_B, F_PARROT_OFF);
                    radio(helper, RADIO_B).setParrotDelay(PARROT_DELAY);
                    speakInto(helper, RADIO_A);
                    radio(helper, RADIO_B).setParrotDelay(0);
                })
                .thenExecuteAfter(PARROT_DELAY * 2, () -> assertHeardNothing(helper, RADIO_A))
                .thenSucceed();
    }

    /** CW keyed on a radio that is off or in SSB mode goes nowhere. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void cwNeedsPowerAndCwMode(GameTestHelper helper) {
        stationsUp(helper)
                .thenExecute(() -> {
                    setupCw(helper, RADIO_B, F_CW_POWER);
                    HFRadioBlockEntity a = radio(helper, RADIO_A);
                    a.updateFrequency(F_CW_POWER);
                    a.setCWEnabled(true); // CW mode but off
                    helper.assertFalse(a.canTransmitCW(), "An unpowered radio should not key");
                    a.transmitMorse(List.of(new CWBuffer(0, new boolean[] { true })));
                    assertHeardNothing(helper, RADIO_B);

                    setupSsb(helper, RADIO_A, F_CW_POWER); // on, but SSB
                    helper.assertFalse(a.canTransmitCW(), "An SSB radio should not key CW");
                    a.transmitMorse(List.of(new CWBuffer(1, new boolean[] { true })));
                    assertHeardNothing(helper, RADIO_B);
                })
                .thenSucceed();
    }
}

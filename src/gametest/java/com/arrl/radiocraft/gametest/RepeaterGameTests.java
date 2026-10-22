package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.VHFRadioBlockEntity;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.blocks.antennas.DuplexerBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.BareConnectorAntenna;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The VHF repeater, its duplexer, CTCSS tones and offsets on the VHF radios, and the bare connector of a radio with no
 * antenna. The base stations here have no antenna: they talk through their bare connectors, a few blocks, so each
 * test's radios only reach the repeater in their own plot. Each test still has its own frequencies.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RepeaterGameTests {

    private static final String BATCH = "repeater";
    private static final UUID OPERATOR = UUID.nameUUIDFromBytes("gametest-repeater-operator".getBytes());

    private static final BlockPos REPEATER = new BlockPos(8, 2, 6); // Faces north, so its back is south (+z).
    private static final BlockPos STATION_A = new BlockPos(5, 2, 6);
    private static final BlockPos STATION_B = new BlockPos(10, 2, 6); // Within the bare connector range of A and of the J-pole.

    private static VHFRepeaterBlockEntity repeater(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, VHFRepeaterBlockEntity.class);
    }

    private static VHFRadioBlockEntity station(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, VHFRadioBlockEntity.class);
    }

    private static void jPole(GameTestHelper helper, BlockPos lower) {
        helper.setBlock(lower, RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), RadiocraftBlocks.J_POLE_ANTENNA.get().defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Repeater, duplexer on its back, two blocks of coax and a J-pole; base stations either side. */
    private static void buildWithDuplexer(GameTestHelper helper) {
        helper.setBlock(REPEATER, RadiocraftBlocks.VHF_REPEATER.get());
        helper.setBlock(REPEATER.south(), RadiocraftBlocks.DUPLEXER.get());
        GameTestUtil.placeWire(helper, REPEATER.south(2), RadiocraftBlocks.COAX_WIRE.get());
        GameTestUtil.placeWire(helper, REPEATER.south(3), RadiocraftBlocks.COAX_WIRE.get());
        jPole(helper, REPEATER.south(4));
        buildStations(helper);
    }

    private static void buildStations(GameTestHelper helper) {
        helper.setBlock(STATION_A, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(STATION_B, RadiocraftBlocks.VHF_BASE_STATION.get());
    }

    private static void powerOn(GameTestHelper helper, BlockPos pos) {
        RadioNetworkObject networkObject = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class);
        if(!networkObject.isPowered)
            GameTestUtil.blockEntity(helper, pos, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class).toggle();
        helper.assertTrue(networkObject.isPowered, "Radio at " + pos.toShortString() + " did not power on");
    }

    /** Powers everything up: the repeater on {@code input} / {@code output} MHz*1000 (kHz), A on the input, B on the output. */
    private static void setUp(GameTestHelper helper, int inputKHz, int outputKHz) {
        CompoundTag settings = new CompoundTag();
        settings.putFloat("rxFrequency", inputKHz * 1000.0F);
        settings.putFloat("txFrequency", outputKHz * 1000.0F);
        settings.putInt("hang", 20);
        repeater(helper, REPEATER).applySettings(settings);
        station(helper, STATION_A).setFrequency(inputKHz * 1000.0F);
        station(helper, STATION_B).setFrequency(outputKHz * 1000.0F);
        powerOn(helper, REPEATER);
        powerOn(helper, STATION_A);
        powerOn(helper, STATION_B);
    }

    private static short[] toneFrame() {
        short[] audio = new short[960];
        for (int i = 0; i < audio.length; i++)
            audio[i] = (short) (Math.sin(2 * Math.PI * 440 * i / 48000.0D) * 8000);
        return audio;
    }

    private static void speak(GameTestHelper helper, BlockPos pos) {
        VHFRadioBlockEntity radio = station(helper, pos);
        radio.setPTTDown(true);
        helper.assertTrue(radio.canTransmitVoice(), "Station at " + pos.toShortString() + " cannot transmit");
        radio.acceptVoicePacket(() -> helper.getLevel(), toneFrame(), OPERATOR);
        radio.setPTTDown(false);
    }

    private static boolean heardNow(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity.class).getLastSignalGameTime() == helper.getLevel().getGameTime();
    }

    private static void assertAntennas(GameTestHelper helper, BlockPos pos, int count) {
        int found = GameTestUtil.networkObject(helper, pos, RadioNetworkObject.class).getAntennas().size();
        helper.assertTrue(found == count, "Radio at " + pos.toShortString() + " sees " + found + " antennas, expected " + count);
    }

    /** Through a duplexer the repeater puts what it hears on its input out on its output, then hangs and beeps. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void repeatsThroughDuplexer(GameTestHelper helper) {
        buildWithDuplexer(helper);
        long[] spokeAt = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAntennas(helper, REPEATER, 1);
                    helper.assertTrue(repeater(helper, REPEATER).hasDuplexer(), "Repeater does not see its duplexer");
                })
                .thenExecute(() -> {
                    setUp(helper, 145_000, 145_600);
                })
                .thenIdle(1) // Bare connectors register on the next tick.
                .thenExecute(() -> {
                    spokeAt[0] = helper.getLevel().getGameTime();
                    speak(helper, STATION_A);
                    helper.assertTrue(heardNow(helper, REPEATER), "Repeater did not hear A on its input");
                    helper.assertTrue(heardNow(helper, STATION_B), "B did not hear the repeater's output");
                })
                // After A lets go the repeater holds the transmitter up (hang time) and sends its courtesy beep...
                .thenWaitUntil(() -> helper.assertTrue(station(helper, STATION_B).getLastSignalGameTime() > spokeAt[0] + VHFRepeaterBlockEntity.INPUT_GAP_TICKS,
                        "B heard nothing from the repeater after A let go"))
                // ...and then drops.
                .thenWaitUntil(() -> helper.assertFalse(repeater(helper, REPEATER).isTransmitting(), "Repeater is still transmitting"))
                .thenSucceed();
    }

    /** A duplexer on top of the repeater shows its coax down the repeater's back, faces it, and counts as its duplexer. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = BATCH)
    public static void duplexerSitsOnTop(GameTestHelper helper) {
        BlockPos above = REPEATER.above();
        helper.setBlock(REPEATER, RadiocraftBlocks.VHF_REPEATER.get()); // Faces north: its back is south.
        helper.setBlock(above, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                RadiocraftBlocks.DUPLEXER.get().defaultBlockState(), helper.getLevel(), helper.absolutePos(above)));
        helper.startSequence()
                .thenExecute(() -> {
                    var state = helper.getBlockState(above);
                    helper.assertTrue(state.getValue(DuplexerBlock.ON_REPEATER), "The duplexer does not know it is on the repeater");
                    helper.assertTrue(state.getValue(DuplexerBlock.FACING) == net.minecraft.core.Direction.SOUTH,
                            "The duplexer faces " + state.getValue(DuplexerBlock.FACING) + ", not the repeater's back");
                })
                .thenWaitUntil(() -> helper.assertTrue(repeater(helper, REPEATER).hasDuplexer(), "The repeater does not see the duplexer on top"))
                .thenExecute(() -> helper.setBlock(REPEATER, net.minecraft.world.level.block.Blocks.STONE))
                .thenExecute(() -> helper.assertFalse(helper.getBlockState(above).getValue(DuplexerBlock.ON_REPEATER), "Still on a repeater that is gone"))
                .thenSucceed();
    }

    /** With an access tone set, only signals carrying it get repeated. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void accessToneIsRequired(GameTestHelper helper) {
        buildWithDuplexer(helper);
        helper.startSequence()
                .thenWaitUntil(() -> assertAntennas(helper, REPEATER, 1))
                .thenExecute(() -> {
                    setUp(helper, 145_100, 145_700);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    CompoundTag tone = new CompoundTag();
                    tone.putFloat("rxTone", 100.0F);
                    repeater(helper, REPEATER).applySettings(tone);
                    speak(helper, STATION_A);
                    helper.assertFalse(heardNow(helper, REPEATER), "Repeater opened without its access tone");
                    helper.assertFalse(heardNow(helper, STATION_B), "B heard a repeat without the access tone");
                })
                .thenIdle(30) // Let any hang time from that run out.
                .thenExecute(() -> {
                    station(helper, STATION_A).setCtcssTone(100.0F);
                    speak(helper, STATION_A);
                    helper.assertTrue(heardNow(helper, REPEATER), "Repeater did not open for its access tone");
                    helper.assertTrue(heardNow(helper, STATION_B), "B did not hear the repeat");
                })
                .thenSucceed();
    }

    /** With an antenna but no duplexer, a signal on the input makes the repeater key up and drop over and over. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = BATCH)
    public static void kerchunksWithoutDuplexer(GameTestHelper helper) {
        helper.setBlock(REPEATER, RadiocraftBlocks.VHF_REPEATER.get());
        for(int i = 1; i <= 3; i++)
            GameTestUtil.placeWire(helper, REPEATER.south(i), RadiocraftBlocks.COAX_WIRE.get());
        jPole(helper, REPEATER.south(4));
        buildStations(helper);
        Set<Boolean> transmitting = new HashSet<>();
        helper.startSequence()
                .thenWaitUntil(() -> assertAntennas(helper, REPEATER, 1))
                .thenExecute(() -> {
                    setUp(helper, 145_200, 145_800);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(repeater(helper, REPEATER).isKerchunking(), "No duplexer: the repeater should kerchunk");
                })
                .thenExecuteFor(2 * VHFRepeaterBlockEntity.KERCHUNK_CYCLE_TICKS + 2, () -> {
                    speak(helper, STATION_A);
                    transmitting.add(repeater(helper, REPEATER).isTransmitting());
                })
                .thenExecute(() -> {
                    helper.assertTrue(transmitting.contains(true) && transmitting.contains(false), "Expected bursts with gaps, saw " + transmitting);
                    helper.assertTrue(station(helper, STATION_B).getLastSignalGameTime() >= 0, "B heard none of the bursts");
                })
                .thenSucceed();
    }

    /** Without any antenna the repeater still works, over the few blocks of its bare connector. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void worksOverBareConnectorWithoutAntenna(GameTestHelper helper) {
        helper.setBlock(REPEATER, RadiocraftBlocks.VHF_REPEATER.get());
        buildStations(helper);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> setUp(helper, 145_300, 145_900))
                .thenWaitUntil(() -> helper.assertTrue(repeater(helper, REPEATER).getBareConnector() != null
                        && station(helper, STATION_A).getBareConnector() != null && station(helper, STATION_B).getBareConnector() != null, "No bare connectors yet"))
                .thenExecute(() -> {
                    speak(helper, STATION_A);
                    helper.assertTrue(heardNow(helper, REPEATER), "Repeater did not hear A over the bare connectors");
                    helper.assertTrue(heardNow(helper, STATION_B), "B did not hear the repeat");
                    helper.assertTrue(BareConnectorAntenna.strengthAt(4.0D) > 0.0D && BareConnectorAntenna.strengthAt(1000.0D) == 0.0D, "Bare range");
                })
                .thenSucceed();
    }

    /** A duplexer on the front does nothing; on the top it connects the antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void duplexerOnlyFitsBackTopOrBottom(GameTestHelper helper) {
        BlockPos front = new BlockPos(3, 2, 6);
        helper.setBlock(front, RadiocraftBlocks.VHF_REPEATER.get());
        helper.setBlock(front.north(), RadiocraftBlocks.DUPLEXER.get());
        GameTestUtil.placeWire(helper, front.north(2), RadiocraftBlocks.COAX_WIRE.get());
        jPole(helper, front.north(3));

        BlockPos top = new BlockPos(12, 2, 6);
        helper.setBlock(top, RadiocraftBlocks.VHF_REPEATER.get());
        helper.setBlock(top.above(), RadiocraftBlocks.DUPLEXER.get());
        GameTestUtil.placeWire(helper, top.above().south(), RadiocraftBlocks.COAX_WIRE.get());
        jPole(helper, top.above().south(2));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAntennas(helper, top, 1);
                    helper.assertTrue(repeater(helper, top).hasDuplexer(), "Duplexer on top not seen");
                })
                .thenExecute(() -> {
                    assertAntennas(helper, front, 0);
                    helper.assertFalse(repeater(helper, front).hasDuplexer(), "A duplexer on the front should not count");
                })
                .thenSucceed();
    }

    /** VHF radios: tone squelch only opens for its tone, and the offset moves the transmit frequency. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = BATCH)
    public static void toneSquelchAndOffset(GameTestHelper helper) {
        buildStations(helper);
        float f = 146_000_000.0F;
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    VHFRadioBlockEntity a = station(helper, STATION_A);
                    VHFRadioBlockEntity b = station(helper, STATION_B);
                    a.setFrequency(f);
                    b.setFrequency(f);
                    b.setCtcssTone(100.0F);
                    powerOn(helper, STATION_A);
                    powerOn(helper, STATION_B);
                })
                .thenIdle(1) // Bare connectors register on the tick.
                .thenExecute(() -> {
                    speak(helper, STATION_A);
                    helper.assertFalse(heardNow(helper, STATION_B), "Tone squelch opened for a signal without a tone");
                    station(helper, STATION_A).setCtcssTone(100.0F);
                    speak(helper, STATION_A);
                    helper.assertTrue(heardNow(helper, STATION_B), "Tone squelch did not open for its tone");
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    VHFRadioBlockEntity a = station(helper, STATION_A);
                    a.setFrequency(f + Ctcss.REPEATER_OFFSET_HZ);
                    a.setOffsetHz(-Ctcss.REPEATER_OFFSET_HZ);
                    helper.assertTrue(a.getTransmitFrequency() == f, "Offset should put the transmitter on " + f + ", was " + a.getTransmitFrequency());
                    speak(helper, STATION_A);
                    helper.assertTrue(heardNow(helper, STATION_B), "B did not hear A transmitting through its offset");
                })
                .thenSucceed();
    }
}

package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaTunerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftAntennaTypes;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.DipoleAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EndFedAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.InvertedVAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.RandomWireAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.InvertedVAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.OffCenterFedDipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.RandomWireAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Antenna structures are recognized from the blocks and wires around a balun.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AntennaGameTests {

    private static final BlockPos CENTER = new BlockPos(8, 2, 8);
    private static final float TWENTY_M = 14_000_000;

    private static StaticAntenna<?> match(GameTestHelper helper, BlockPos pos) {
        IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(pos));
        if (!(antenna instanceof StaticAntenna<?> staticAntenna)) {
            helper.fail("No antenna recognized", pos);
            return null;
        }
        return staticAntenna;
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void dipoleIsRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(5));
        GameTestUtil.wire(helper, CENTER, CENTER.east(5));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, CENTER);
            helper.assertTrue(antenna.type instanceof DipoleAntennaType, "Expected dipole, got " + antenna.type.getId());
            DipoleAntennaData data = (DipoleAntennaData) antenna.data;
            helper.assertTrue(data.getArmLength1() == 5 && data.getArmLength2() == 5,
                    "Arm lengths " + data.getArmLength1() + "/" + data.getArmLength2());
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.0D, "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    @GameTest(template = GameTestUtil.EMPTY)
    public static void endFedIsRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.north(6));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, CENTER);
            helper.assertTrue(antenna.type instanceof EndFedAntennaType, "Expected end fed, got " + antenna.type.getId());
            helper.assertTrue(((EndFedAntennaData) antenna.data).getLength() == 6, "Wrong length");
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.5D, "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    /** Centre balun 3 blocks up, ends on the floor 4 blocks out: each arm is 5 along the wire, matched on 20m. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void invertedVIsRecognized(GameTestHelper helper) {
        BlockPos apex = CENTER.above(3);
        helper.setBlock(apex, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, apex, CENTER.west(4));
        GameTestUtil.wire(helper, apex, CENTER.east(4));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, apex);
            helper.assertTrue(antenna.type instanceof InvertedVAntennaType, "Expected inverted V, got " + antenna.type.getId());
            InvertedVAntennaData data = (InvertedVAntennaData) antenna.data;
            helper.assertTrue(Math.abs(data.getArmLength1() - 5.0) < 1e-9 && Math.abs(data.getArmLength2() - 5.0) < 1e-9,
                    "Arm lengths " + data.getArmLength1() + "/" + data.getArmLength2());
            helper.assertTrue(data.getDrop1() == 3 && data.getDrop2() == 3, "Drops " + data.getDrop1() + "/" + data.getDrop2());
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.0D, "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    /** Arms that rise from the centre are neither a dipole nor an inverted V. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void raisedArmsAreNotAnInvertedV(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(4).above(3));
        GameTestUtil.wire(helper, CENTER, CENTER.east(4).above(3));

        helper.runAtTickTime(5, () -> {
            IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(CENTER));
            if (antenna instanceof StaticAntenna<?> s && (s.type instanceof InvertedVAntennaType || s.type instanceof DipoleAntennaType))
                helper.fail("Rising arms should not make a " + s.type.getId(), CENTER);
            helper.succeed();
        });
    }

    /** One arm steeper than 45 degrees is a sloper, not an inverted V. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void steepArmIsNotAnInvertedV(GameTestHelper helper) {
        BlockPos apex = CENTER.above(4);
        helper.setBlock(apex, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, apex, CENTER.west(2)); // 2 across, 4 down
        GameTestUtil.wire(helper, apex, CENTER.east(5));

        helper.runAtTickTime(5, () -> {
            IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(apex));
            if (antenna instanceof StaticAntenna<?> s && s.type instanceof InvertedVAntennaType)
                helper.fail("A steep arm should not make an inverted V", apex);
            helper.succeed();
        });
    }

    /** A 9:1 balun with a wire that bends once at a pole: length is the sum of the segments. Needs the generated block tags. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void randomWireIsRecognized(GameTestHelper helper) {
        BlockPos feed = new BlockPos(2, 2, 2);
        BlockPos corner = feed.south(6);
        helper.setBlock(feed, RadiocraftBlocks.BALUN_NINE_TO_ONE.get());
        GameTestUtil.wire(helper, feed, corner);
        GameTestUtil.wire(helper, corner, corner.east(6));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, feed);
            helper.assertTrue(antenna.type instanceof RandomWireAntennaType, "Expected random wire, got " + antenna.type.getId());
            RandomWireAntennaData data = (RandomWireAntennaData) antenna.data;
            helper.assertTrue(Math.abs(data.getLength() - 12.0) < 1e-9, "Length " + data.getLength());
            helper.assertTrue(data.getSegments() == 2, "Segments " + data.getSegments());
            helper.assertTrue(antenna.getSWR(TWENTY_M) == RandomWireAntennaType.swrForLength(12.0, TWENTY_M), "20m SWR was " + antenna.getSWR(TWENTY_M));
        });
    }

    /** A wire that forks at a pole is not a random wire. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void forkedWireIsNotARandomWire(GameTestHelper helper) {
        BlockPos feed = new BlockPos(2, 2, 2);
        BlockPos corner = feed.south(6);
        helper.setBlock(feed, RadiocraftBlocks.BALUN_NINE_TO_ONE.get());
        GameTestUtil.wire(helper, feed, corner);
        GameTestUtil.wire(helper, corner, corner.east(6));
        GameTestUtil.wire(helper, corner, corner.south(6));

        helper.runAtTickTime(5, () -> {
            IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(feed));
            if (antenna instanceof StaticAntenna<?> s && s.type instanceof RandomWireAntennaType)
                helper.fail("A forked wire should not make a random wire", feed);
            helper.succeed();
        });
    }

    /**
     * The random wire end to end: 9:1 balun on the coax with a tuner, a 20m radio sees SWR 1; the same run without a
     * tuner sees the wire's own SWR. Needs the generated block tags (./gradlew runData) for the 9:1 balun.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void randomWireNeedsTuner(GameTestHelper helper) {
        BlockPos radio = new BlockPos(2, 2, 1);
        BlockPos tuner = new BlockPos(2, 2, 3);
        BlockPos feed = new BlockPos(2, 2, 5);
        helper.setBlock(radio, RadiocraftBlocks.HF_RADIO_20M.get());
        GameTestUtil.placeWire(helper, radio.south(), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(tuner, RadiocraftBlocks.ANTENNA_TUNER.get());
        GameTestUtil.placeWire(helper, tuner.south(), RadiocraftBlocks.COAX_WIRE.get());
        helper.setBlock(feed, RadiocraftBlocks.BALUN_NINE_TO_ONE.get());
        GameTestUtil.wire(helper, feed, feed.south(6));
        GameTestUtil.wire(helper, feed.south(6), feed.south(6).east(6));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    var antennas = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas();
                    helper.assertTrue(antennas.size() == 1, "Radio sees " + antennas.size() + " antennas");
                    helper.assertTrue(antennas.getFirst().getAntenna() != null, "Random wire not recognized yet");
                })
                .thenExecute(() -> {
                    TransmissionGameTests.setupSsb(helper, radio, 330);
                    AntennaNetworkObject antenna = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().getFirst();
                    float frequency = TransmissionGameTests.radio(helper, radio).getFrequency();
                    int[] targets = AntennaTunerNetworkObject.targets(antenna, antenna.getRawSWR(frequency), frequency);
                    for(int i = 0; i < 3; i++) // Turn the manual tuner's dials to their readings, as a player would.
                        GameTestUtil.networkObject(helper, tuner, AntennaTunerNetworkObject.class).setDial(i, targets[i]);
                })
                .thenWaitUntil(() -> {
                    AntennaNetworkObject antenna = GameTestUtil.networkObject(helper, radio, RadioNetworkObject.class).getAntennas().getFirst();
                    float frequency = TransmissionGameTests.radio(helper, radio).getFrequency();
                    double raw = RandomWireAntennaType.swrForLength(12.0, frequency);
                    helper.assertTrue(antenna.hasTuner(), "Antenna does not see the tuner");
                    helper.assertTrue(Math.abs(antenna.getRawSWR(frequency) - raw) < 1e-9, "Raw SWR " + antenna.getRawSWR(frequency) + ", expected " + raw);
                    helper.assertTrue(TransmissionGameTests.radio(helper, radio).getAntennaSWR() == 1.0D, "Radio sees SWR " + TransmissionGameTests.radio(helper, radio).getAntennaSWR());
                })
                .thenExecute(() -> {
                    helper.setBlock(tuner, Blocks.AIR);
                    GameTestUtil.placeWire(helper, tuner, RadiocraftBlocks.COAX_WIRE.get());
                })
                .thenWaitUntil(() -> {
                    float frequency = TransmissionGameTests.radio(helper, radio).getFrequency();
                    double raw = RandomWireAntennaType.swrForLength(12.0, frequency);
                    helper.assertTrue(Math.abs(TransmissionGameTests.radio(helper, radio).getAntennaSWR() - raw) < 1e-9,
                            "Without the tuner the radio sees SWR " + TransmissionGameTests.radio(helper, radio).getAntennaSWR() + ", expected " + raw);
                })
                .thenSucceed();
    }

    /** Arms at 90 degrees are not a dipole, but still an (improvised) antenna, with the bend costing SWR. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void bentDipoleIsImprovised(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(5));
        GameTestUtil.wire(helper, CENTER, CENTER.north(5)); // 90 degrees, not opposite

        helper.runAtTickTime(5, () -> {
            IAntenna antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(CENTER));
            if (!(antenna instanceof StaticAntenna<?> s) || s.type != RadiocraftAntennaTypes.IMPROVISED)
                helper.fail("Perpendicular arms should make an improvised antenna", CENTER);
            else
                helper.assertTrue(s.getSWR(14_175_000.0F) > 2.0D, "The bend should cost SWR, was " + s.getSWR(14_175_000.0F));
            helper.succeed();
        });
    }

    /** A 4 + 7 on a 4:1 balun is an off-centre-fed dipole: matched on 20m (a half wave) and on 10m (two), poor on 15m. */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void offCentreFedDipoleIsRecognized(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_FOUR_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(4));
        GameTestUtil.wire(helper, CENTER, CENTER.east(7));

        helper.succeedWhen(() -> {
            StaticAntenna<?> antenna = match(helper, CENTER);
            helper.assertTrue(antenna.type instanceof OffCenterFedDipoleAntennaType, "Expected OCFD, got " + antenna.type.getId());
            helper.assertTrue(antenna.getSWR(TWENTY_M) == 1.0D, "20m SWR was " + antenna.getSWR(TWENTY_M));
            helper.assertTrue(antenna.getSWR(28_000_000) == 1.0D, "10m SWR was " + antenna.getSWR(28_000_000));
            helper.assertTrue(antenna.getSWR(21_000_000) > 2.0D, "15m SWR was " + antenna.getSWR(21_000_000));
        });
    }

    /**
     * Equal arms on a 4:1 are a dipole on the wrong balun, not an OCFD. Any antenna beats no antenna, so it still works
     * as an improvised antenna, just a worse match than the same dipole on a 1:1.
     */
    @GameTest(template = GameTestUtil.EMPTY)
    public static void centreFedWireOnAFourToOneIsImprovised(GameTestHelper helper) {
        helper.setBlock(CENTER, RadiocraftBlocks.BALUN_FOUR_TO_ONE.get());
        GameTestUtil.wire(helper, CENTER, CENTER.west(5));
        GameTestUtil.wire(helper, CENTER, CENTER.east(5));
        helper.runAfterDelay(5, () -> {
            StaticAntenna<?> antenna = AntennaTypes.match(helper.getLevel(), helper.absolutePos(CENTER));
            helper.assertTrue(antenna != null && antenna.type == RadiocraftAntennaTypes.IMPROVISED, "A 5 + 5 on a 4:1 should be improvised, got " + (antenna == null ? "none" : antenna.type.getId()));
            helper.assertTrue(antenna.getSWR(14_175_000.0F) > 2.0D, "The wrong balun should cost SWR, was " + antenna.getSWR(14_175_000.0F));
            helper.succeed();
        });
    }
}

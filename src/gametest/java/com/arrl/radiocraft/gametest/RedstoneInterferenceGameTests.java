package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.rfi.PickupZone;
import com.arrl.radiocraft.common.radio.rfi.RedstoneInterference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Redstone interference on receive: redstone switching near a station's feedline or antenna raises that radio's
 * interference level. Uses station A of the two-station layout (radio at z=1, coax, balun at z=3 with a 5+5 dipole
 * running east-west). Nothing here transmits, so no frequencies are needed.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RedstoneInterferenceGameTests {

    /** One block west of the radio: on the feedline. */
    private static final BlockPos BY_RADIO = RADIO_A.west();
    /** Two blocks above the east dipole arm's middle: inside the antenna radius, outside the feedline's. */
    private static final BlockPos BY_ANTENNA = BALUN_A.east(3).above(2);
    /** Ten blocks south of the balun: outside both radii. */
    private static final BlockPos FAR = BALUN_A.south(10);

    /**
     * A redstone block appearing is a signal source's neighbour update: one full pop at that block's coupling. Taking
     * it away again notifies with air as the state, which is not electrical and adds nothing.
     */
    private static void pulse(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.REDSTONE_BLOCK);
        helper.setBlock(pos, Blocks.AIR);
    }

    /** What one pulse right next to the radio adds for this radio's band, before decay. */
    private static double popByRadio(GameTestHelper helper) {
        double coupling = 1.0D - 1.0D / PickupZone.FEEDLINE_RADIUS;
        return RedstoneInterference.POP * coupling * RedstoneInterference.bandFactor(radio(helper, RADIO_A).getBand());
    }

    /** A powered station registers a zone that covers its radio, coax and antenna wires, and nothing when switched off. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void poweredStationHasPickupZone(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            PickupZone zone = RedstoneInterference.getZone(helper.getLevel(), helper.absolutePos(RADIO_A));
            helper.assertTrue(zone != null, "Powered radio has no pickup zone");
            helper.assertTrue(zone.feedline().contains(helper.absolutePos(RADIO_A)), "Zone does not include the radio");
            helper.assertTrue(zone.feedline().contains(helper.absolutePos(RADIO_A.south())), "Zone does not include the coax");
            helper.assertTrue(zone.antenna().contains(helper.absolutePos(BALUN_A.east(5))), "Zone does not reach the end of the dipole");
            helper.assertTrue(zone.antenna().contains(helper.absolutePos(BALUN_A.west(2))), "Zone misses the middle of the west arm");
        });
        helper.runAtTickTime(31, () -> TransmissionGameTests.powerOff(helper, RADIO_A));
        helper.runAtTickTime(35, () -> {
            helper.assertTrue(RedstoneInterference.getZone(helper.getLevel(), helper.absolutePos(RADIO_A)) == null, "Zone survived switching the radio off");
            helper.succeed();
        });
    }

    /** Redstone switching by the feedline pops the radio's interference, which then decays away. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void redstoneByFeedlineAddsInterferenceThatDecays(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Interference before any redstone");
            pulse(helper, BY_RADIO);
        });
        helper.runAtTickTime(32, () -> {
            double level = radio(helper, RADIO_A).getInterference();
            double expected = popByRadio(helper); // Minus up to two ticks of decay, depending on tick order.
            helper.assertTrue(level > expected * 0.6D && level <= expected, "Interference after a pulse is " + level + ", expected about " + expected);
        });
        helper.runAtTickTime(32 + 60, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Interference did not decay: " + radio(helper, RADIO_A).getInterference());
            helper.succeed();
        });
    }

    /** Switching near the antenna wire counts too, more weakly with distance; far away it does not. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void antennaPickupFadesWithDistanceAndFarRedstoneIsSilent(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        double[] near = new double[1];
        helper.runAtTickTime(30, () -> pulse(helper, BY_ANTENNA));
        helper.runAtTickTime(32, () -> {
            near[0] = radio(helper, RADIO_A).getInterference();
            helper.assertTrue(near[0] > 0.0D, "No interference from redstone two blocks above the antenna");
        });
        helper.runAtTickTime(30 + 60, () -> pulse(helper, BY_ANTENNA.above(3)));
        helper.runAtTickTime(32 + 60, () -> {
            double further = radio(helper, RADIO_A).getInterference();
            helper.assertTrue(further > 0.0D && further < near[0], "Five blocks above the antenna should be weaker than two: " + further + " vs " + near[0]);
        });
        helper.runAtTickTime(30 + 120, () -> pulse(helper, FAR));
        helper.runAtTickTime(32 + 120, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Far redstone added interference");
            helper.succeed();
        });
    }

    /** A redstone clock keeps the level up instead of letting it decay. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void clockKeepsInterferenceUp(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        for(int tick = 30; tick < 90; tick += 2) {
            final int t = tick;
            helper.runAtTickTime(t, () -> helper.setBlock(BY_RADIO, Blocks.REDSTONE_BLOCK));
            helper.runAtTickTime(t + 1, () -> helper.setBlock(BY_RADIO, Blocks.AIR));
        }
        helper.runAtTickTime(89, () -> {
            double level = radio(helper, RADIO_A).getInterference();
            double singlePop = popByRadio(helper);
            helper.assertTrue(level > singlePop * 1.5D, "A clock should pile up well above one pop (" + singlePop + "), got " + level);
            helper.assertTrue(level <= 1.0D, "Interference above 1: " + level);
            helper.succeed();
        });
    }

    /** Placing a plain block is not electrical, so it makes no noise even right by the radio. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void plainBlocksAreSilent(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        helper.runAtTickTime(30, () -> {
            helper.setBlock(BY_RADIO, Blocks.STONE);
            helper.setBlock(BY_RADIO, Blocks.AIR);
        });
        helper.runAtTickTime(32, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Stone made noise: " + radio(helper, RADIO_A).getInterference());
            helper.succeed();
        });
    }

    /** An unpowered radio has no zone, so nearby redstone does nothing to it. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void unpoweredRadioIgnoresRedstone(GameTestHelper helper) {
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAtTickTime(30, () -> pulse(helper, BY_RADIO));
        helper.runAtTickTime(32, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "Unpowered radio picked up interference");
            helper.succeed();
        });
    }

    /**
     * A weather instrument's reading changing is not switching: Another Weather Mod's wind vane re-reads every second and,
     * in a storm, popped the radio beside it each time (2026-10-30). Runs only when that mod is loaded.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void weatherInstrumentReadingsAreSilent(GameTestHelper helper) {
        net.minecraft.world.level.block.Block vane = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anotherweathermod", "wind_vane"));
        if(vane == Blocks.AIR) {
            helper.succeed(); // Another Weather Mod is not installed.
            return;
        }
        buildStation(helper, RADIO_A, BALUN_A);
        helper.runAfterDelay(5, () -> setupSsb(helper, RADIO_A, 0));
        net.minecraft.world.level.block.state.properties.IntegerProperty power = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER;
        helper.runAtTickTime(30, () -> {
            helper.setBlock(BY_RADIO, vane.defaultBlockState().setValue(power, 3));
            helper.setBlock(BY_RADIO, vane.defaultBlockState().setValue(power, 9));
        });
        helper.runAtTickTime(32, () -> {
            helper.assertTrue(radio(helper, RADIO_A).getInterference() == 0.0D, "The wind vane made noise: " + radio(helper, RADIO_A).getInterference());
            helper.succeed();
        });
    }
}

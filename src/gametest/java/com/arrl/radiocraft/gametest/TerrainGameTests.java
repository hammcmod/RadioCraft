package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.terrain.TerrainAtlas;
import com.arrl.radiocraft.common.radio.terrain.TerrainPropagation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Locale;
import java.util.UUID;

/**
 * Terrain-aware propagation in a real world: a stone wall south of a station, sampled into the terrain map from the
 * loaded chunks. Terrain is off for the rest of the GameTest run (see {@link GameTestDefaults}: neighbouring test
 * plots would otherwise be hills to each other), so these switch it on in a batch of their own and off again.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TerrainGameTests {

    /** The stations float 60 blocks up, above the other test plots around this one (which are hills to it). */
    private static final int UP = 62;
    private static final BlockPos BALUN = new BlockPos(8, UP, 8);
    private static final BlockPos VHF_RADIO = new BlockPos(3, UP, 3);
    private static final BlockPos J_POLE = VHF_RADIO.south(2);
    /** Line-of-sight target: 60 blocks south, past the wall. */
    private static final int LOS_DISTANCE = 60;
    /** 20m daytime skip target, far south (or north). */
    private static final int SKIP_DISTANCE = 4000;
    /** The wall: 28 to 35 blocks south of the balun, 16 wide, floating from 12 below the stations to 20 above (well over
     * the 20m takeoff angle, whatever the 4 block cells it falls in). */
    private static final int WALL_NEAR = 28, WALL_FAR = 35, WALL_HEIGHT = 12, WALL_TOP = 20;

    private static <T extends AntennaData> double efficiency(StaticAntenna<T> antenna, IAntennaPacket packet, BlockPos destination) {
        return antenna.type.getTransmitEfficiency(packet, antenna.data, destination, false);
    }

    /** Strength a voice signal leaves {@code at}'s antenna with towards {@code destination}, worked out afresh. */
    private static double strength(GameTestHelper helper, BlockPos at, String bandName, BlockPos destination) {
        TerrainPropagation.clearCaches();
        StaticAntenna<?> antenna = GameTestUtil.networkObject(helper, at, AntennaNetworkObject.class).getAntenna();
        Band band = Band.getBand(bandName);
        AntennaVoicePacket packet = new AntennaVoicePacket(() -> helper.getLevel(), new short[960], band, band.minFrequency(), 1.0D, antenna, UUID.randomUUID());
        return efficiency(antenna, packet, destination);
    }

    /** The terrain map along a path, for failure messages. */
    private static String describe(GameTestHelper helper, BlockPos from, BlockPos to) {
        BlockPos a = helper.absolutePos(from);
        TerrainAtlas atlas = TerrainPropagation.atlas(helper.getLevel());
        StringBuilder out = new StringBuilder("antenna y " + a.getY() + ", surface along the path:");
        for(int i = 0; i <= 16; i++) {
            BlockPos p = new BlockPos(a.getX() + (to.getX() - a.getX()) * i / 16, 0, a.getZ() + (to.getZ() - a.getZ()) * i / 16);
            out.append(' ').append(atlas.surface(p.getX(), p.getZ()));
        }
        BlockPos ap = GameTestUtil.networkObject(helper, from, AntennaNetworkObject.class).getAntenna().getAntennaPos().position();
        out.append(", antenna at ").append(ap.toShortString()).append(" from ").append(a.toShortString()).append(" to ").append(to.toShortString())
                .append(", 2m los ").append(Band.getBand("2m").losRange())
                .append(", factor ").append(TerrainPropagation.factor(helper.getLevel(), Band.getBand("2m"), ap, false, to, false, false));
        return out.append(", loss ").append(TerrainPropagation.pathLossDb(helper.getLevel(), atlas, Band.getBand("2m"), a, to)).append(" dB").toString();
    }

    /** Takes the floating stations down again: the plot clean-up only clears the template's own 16 blocks. */
    private static void cleanUp(GameTestHelper helper) {
        BlockPos min = helper.absolutePos(new BlockPos(0, UP, 0)), max = helper.absolutePos(new BlockPos(15, UP + 1, 15));
        helper.getLevel().getEntitiesOfClass(AntennaWire.class, new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).forEach(Entity::discard);
        for(BlockPos pos : BlockPos.betweenClosed(min, max))
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static BlockPos south(GameTestHelper helper, BlockPos from, int blocks) {
        return helper.absolutePos(from).south(blocks);
    }

    private static void wall(GameTestHelper helper, BlockState state) {
        BlockPos min = helper.absolutePos(new BlockPos(0, UP - WALL_HEIGHT, BALUN.getZ() + WALL_NEAR));
        BlockPos max = helper.absolutePos(new BlockPos(15, UP + WALL_TOP, BALUN.getZ() + WALL_FAR));
        for(BlockPos pos : BlockPos.betweenClosed(min, max))
            helper.getLevel().setBlock(pos, state, 2);
        TerrainPropagation.refresh(helper.getLevel(), min.offset(-16, 0, -16), max.offset(16, 0, 16));
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "terrain")
    public static void wallBlocksLineOfSightAndLowSkip(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        SolarEventInstance previous = SolarEventManager.getEvent(helper.getLevel());
        SolarEventManager.setEvent(helper.getLevel(), RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id("average")).getInstance()); // 20m open.
        RadiocraftServerConfig.TERRAIN_ENABLED.set(true);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        GameTestUtil.radioWithJPole(helper, VHF_RADIO);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getLevel().isDay(), "Not day yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna() != null, "Dipole not recognized yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, J_POLE, AntennaNetworkObject.class).getAntenna() != null, "J-pole not recognized yet");
                })
                .thenExecute(() -> {
                    TerrainPropagation.refresh(helper.getLevel(), helper.absolutePos(BALUN).offset(-64, 0, -64), helper.absolutePos(BALUN).offset(64, 0, 64));
                    BlockPos vhfTarget = south(helper, J_POLE, LOS_DISTANCE), hfTarget = south(helper, BALUN, LOS_DISTANCE);
                    BlockPos skipSouth = south(helper, BALUN, SKIP_DISTANCE), skipNorth = helper.absolutePos(BALUN).north(SKIP_DISTANCE);
                    double vhfClear = strength(helper, J_POLE, "2m", vhfTarget), hfClear = strength(helper, BALUN, "20m", hfTarget);
                    double skipClear = strength(helper, BALUN, "20m", skipSouth), northClear = strength(helper, BALUN, "20m", skipNorth);
                    helper.assertTrue(vhfClear > 0.0D && hfClear > 0.0D && skipClear > 0.0D, String.format(Locale.ROOT,
                            "Nothing to compare: 2m %.4f, 20m %.4f, skip %.4f", vhfClear, hfClear, skipClear));

                    wall(helper, Blocks.STONE.defaultBlockState());
                    double vhfWall = strength(helper, J_POLE, "2m", vhfTarget), hfWall = strength(helper, BALUN, "20m", hfTarget);
                    double skipWall = strength(helper, BALUN, "20m", skipSouth), northWall = strength(helper, BALUN, "20m", skipNorth);
                    helper.assertTrue(vhfWall < vhfClear * 0.1D, String.format(Locale.ROOT, "2m through the wall kept %.4f of %.4f (%s)", vhfWall, vhfClear, describe(helper, J_POLE, vhfTarget)));
                    helper.assertTrue(hfWall < hfClear * 0.5D && hfWall > vhfWall / vhfClear * hfClear,
                            String.format(Locale.ROOT, "20m over the wall kept %.4f of %.4f (2m kept %.3f of its signal)", hfWall, hfClear, vhfWall / vhfClear));
                    helper.assertTrue(skipWall < skipClear * 0.01D, String.format(Locale.ROOT, "20m skip south over the wall kept %.4f of %.4f", skipWall, skipClear));
                    helper.assertTrue(Math.abs(northWall - northClear) < 1e-9, String.format(Locale.ROOT, "20m skip north changed: %.4f vs %.4f", northWall, northClear));

                    RadiocraftServerConfig.TERRAIN_ENABLED.set(false);
                    double vhfOff = strength(helper, J_POLE, "2m", vhfTarget);
                    helper.assertTrue(vhfOff >= vhfClear, String.format(Locale.ROOT, "With terrain off the wall still cost 2m: %.4f vs %.4f", vhfOff, vhfClear));
                    RadiocraftServerConfig.TERRAIN_ENABLED.set(true);

                    wall(helper, Blocks.AIR.defaultBlockState());
                    double vhfAgain = strength(helper, J_POLE, "2m", vhfTarget);
                    helper.assertTrue(Math.abs(vhfAgain - vhfClear) < 1e-9, String.format(Locale.ROOT, "2m after the wall came down: %.4f, was %.4f", vhfAgain, vhfClear));
                })
                .thenExecute(() -> {
                    RadiocraftServerConfig.TERRAIN_ENABLED.set(false);
                    SolarEventManager.setEvent(helper.getLevel(), previous);
                    cleanUp(helper);
                })
                .thenSucceed();
    }

    /** Times terrain in the real game path (antenna type, config, atlas) against terrain off. Prints, never fails. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "terrain_cost")
    public static void terrainCostInGame(GameTestHelper helper) {
        RadiocraftServerConfig.TERRAIN_ENABLED.set(true);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna() != null, "Dipole not recognized yet"))
                .thenExecute(() -> {
                    StaticAntenna<?> antenna = GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna();
                    Band band = Band.getBand("20m");
                    AntennaVoicePacket packet = new AntennaVoicePacket(() -> helper.getLevel(), new short[960], band, band.minFrequency(), 1.0D, antenna, UUID.randomUUID());
                    BlockPos from = helper.absolutePos(BALUN);
                    for(int cx = from.getX() >> 4; cx <= (from.getX() + 260) >> 4; cx++) // Load and map the ground the paths cross.
                        for(int cz = from.getZ() >> 4; cz <= (from.getZ() + 40) >> 4; cz++)
                            helper.getLevel().getChunk(cx, cz);
                    TerrainPropagation.refresh(helper.getLevel(), from, from.offset(260, 0, 40));
                    int n = 20_000;
                    double recalc = RadiocraftServerConfig.TERRAIN_RECALCULATE_SECONDS.get();
                    double[] perCall = new double[3];
                    String[] names = {"terrain off", "terrain on, worked out every packet", "terrain on, cached"};
                    for(int run = 0; run < 3; run++) {
                        RadiocraftServerConfig.TERRAIN_ENABLED.set(run > 0);
                        RadiocraftServerConfig.TERRAIN_RECALCULATE_SECONDS.set(run == 1 ? 0.0D : 2.0D);
                        TerrainPropagation.clearCaches();
                        double sink = 0.0D;
                        perCall[run] = Double.MAX_VALUE;
                        for(int round = 0; round < 4; round++) { // The best of four, the first being the warm-up.
                            long t0 = System.nanoTime();
                            for(int i = 0; i < n; i++)
                                sink += efficiency(antenna, packet, from.offset(50 + i % 200, 0, i % 37));
                            if(round > 0)
                                perCall[run] = Math.min(perCall[run], (System.nanoTime() - t0) / 1e3D / n);
                        }
                        helper.assertTrue(sink >= 0.0D, "sink");
                    }
                    RadiocraftServerConfig.TERRAIN_RECALCULATE_SECONDS.set(recalc);
                    RadiocraftServerConfig.TERRAIN_ENABLED.set(false);
                    cleanUp(helper);
                    for(int run = 0; run < 3; run++)
                        Radiocraft.LOGGER.info(String.format(Locale.ROOT, "[terrain cost] 20m strength to one receiver, %s: %.2f us", names[run], perCall[run]));
                })
                .thenSucceed();
    }
}

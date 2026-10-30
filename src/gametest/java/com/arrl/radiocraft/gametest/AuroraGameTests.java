package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.AuroraForecast;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Auroras: forecast by a blackout, then VHF skip and an HF condition step for stations under them. The aurora, the
 * solar event and the latitude config are level-wide, so each test has a batch of its own and puts them back. GameTest
 * plots sit near Z = 0, so the propagation test moves the aurora latitude to 0 to put the plot under it.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AuroraGameTests {

    private static final BlockPos BALUN = new BlockPos(8, 2, 8);
    private static final BlockPos STATION = new BlockPos(3, 2, 3);

    /** Strength a voice signal on {@code bandName} leaves the plot's dipole with, towards a point that far south. */
    private static double strength(GameTestHelper helper, String bandName, int distance) {
        StaticAntenna<?> antenna = GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna();
        Band band = Band.getBand(bandName);
        AntennaVoicePacket packet = new AntennaVoicePacket(() -> helper.getLevel(), new short[960], band, band.minFrequency(), 1.0D, antenna, UUID.randomUUID());
        return transmitEfficiency(antenna, packet, antenna.getAntennaPos().position().south(distance));
    }

    private static <T extends AntennaData> double transmitEfficiency(StaticAntenna<T> antenna, IAntennaPacket packet, BlockPos destination) {
        return antenna.type.getTransmitEfficiency(packet, antenna.data, destination, false);
    }

    /**
     * Under an aurora 20m skip drops one step (good to fair, 0.6 of the strength), VHF reaches 2000 blocks where it
     * normally stops at line of sight, and the station says so.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "aurora")
    public static void auroraCarriesVhfAndWeakensHf(GameTestHelper helper) {
        SolarEventInstance previousEvent = SolarEventManager.getEvent(helper.getLevel());
        int previousLatitude = RadiocraftServerConfig.AURORA_LATITUDE.get();
        helper.getLevel().setDayTime(6000);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        helper.setBlock(STATION, RadiocraftBlocks.TELETYPE.get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getLevel().isDay(), "Not day yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna() != null, "Dipole not recognized yet");
                })
                .thenExecute(() -> {
                    SolarEventManager.setEvent(helper.getLevel(), RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id("average")).getInstance());
                    RadiocraftServerConfig.AURORA_LATITUDE.set(0);
                    GameTestDefaults.calmSky(helper.getLevel()); // No aurora and no sporadic E opening before the measurement.
                    double hfBefore = strength(helper, "20m", 4000);
                    double vhfBefore = strength(helper, "2m", 2000);
                    helper.assertTrue(vhfBefore == 0.0D, "VHF should not reach 2000 blocks without an aurora, was " + vhfBefore);

                    Aurora.schedule(helper.getLevel(), helper.getLevel().getGameTime(), 24000);
                    helper.assertTrue(Aurora.isActive(helper.getLevel()), "Aurora did not start");
                    double hfAfter = strength(helper, "20m", 4000);
                    double expected = hfBefore * BandCondition.FAIR.getSkipMultiplier();
                    helper.assertTrue(Math.abs(hfAfter - expected) < 1e-9, "20m under the aurora was " + hfAfter + ", expected " + expected);
                    double vhfAfter = strength(helper, "2m", 2000);
                    helper.assertTrue(vhfAfter > 0.5D, "VHF should skip 2000 blocks off the aurora, was " + vhfAfter);

                    TeletypeBlockEntity station = GameTestUtil.blockEntity(helper, STATION, TeletypeBlockEntity.class);
                    helper.assertTrue(station.getBandCondition("20m") == BandCondition.FAIR, "Station should report 20m fair, says " + station.getBandCondition("20m"));
                    helper.assertTrue(station.auroraLine() != null, "Station does not mention the aurora");

                    Aurora.clear(helper.getLevel());
                    RadiocraftServerConfig.AURORA_LATITUDE.set(previousLatitude);
                    SolarEventManager.setEvent(helper.getLevel(), previousEvent);
                })
                .thenSucceed();
    }

    /** An event with a certain aurora forecasts it, lights it after the delay and puts it out after the duration. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200, batch = "aurora_forecast")
    public static void blackoutForecastsAurora(GameTestHelper helper) {
        SolarEventInstance previousEvent = SolarEventManager.getEvent(helper.getLevel());
        SolarEvent storm = new SolarEvent(1.0F, 1000, 1000, Map.of(), Map.of(), new AuroraForecast(1.0D, 20, 40));
        Aurora.clear(helper.getLevel());
        SolarEventManager.startEvent(helper.getLevel(), storm);
        helper.assertTrue(Aurora.isExpected(helper.getLevel()), "A certain forecast did not schedule an aurora");
        helper.assertFalse(Aurora.isActive(helper.getLevel()), "Aurora started before its delay");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Aurora.isActive(helper.getLevel()), "Aurora never started"))
                .thenWaitUntil(() -> helper.assertFalse(Aurora.isActive(helper.getLevel()), "Aurora never ended"))
                .thenExecute(() -> {
                    Aurora.clear(helper.getLevel());
                    SolarEventManager.setEvent(helper.getLevel(), previousEvent);
                })
                .thenSucceed();
    }

    /** With allow_aurora_at_any_z_coordinate on, a station near Z = 0 is under the aurora; off, only past the latitude. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20, batch = "aurora")
    public static void auroraAtAnyZSetting(GameTestHelper helper) {
        int previousLatitude = RadiocraftServerConfig.AURORA_LATITUDE.get();
        boolean previousAnyZ = RadiocraftServerConfig.AURORA_AT_ANY_Z.get();
        try {
            RadiocraftServerConfig.AURORA_LATITUDE.set(1_000_000);
            RadiocraftServerConfig.AURORA_AT_ANY_Z.set(false);
            helper.assertFalse(Aurora.inZone(new BlockPos(0, 64, 100)), "Under the aurora at Z 100 with the latitude at 1000000");
            RadiocraftServerConfig.AURORA_AT_ANY_Z.set(true);
            helper.assertTrue(Aurora.inZone(new BlockPos(0, 64, 100)), "Not under the aurora at Z 100 with any Z allowed");
            helper.assertTrue(Aurora.latitude() == 0, "Clients would be sent latitude " + Aurora.latitude());
        }
        finally {
            RadiocraftServerConfig.AURORA_LATITUDE.set(previousLatitude);
            RadiocraftServerConfig.AURORA_AT_ANY_Z.set(previousAnyZ);
        }
        helper.succeed();
    }
}

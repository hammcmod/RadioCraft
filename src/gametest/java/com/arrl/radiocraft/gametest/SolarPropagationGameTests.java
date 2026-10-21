package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blockentities.SolarWeatherStationBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Solar weather shaping HF propagation: the event's band conditions scale skip, by day and by night. Each test sets the
 * dimension's solar event and time of day, which are level-wide, so each runs in a batch of its own and puts the event
 * back when done.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarPropagationGameTests {

    private static final BlockPos BALUN = new BlockPos(8, 2, 8);
    private static final BlockPos STATION = new BlockPos(3, 2, 3);
    /** Middle of the 20m daytime skip zone (2000 to 6000 blocks), where skip is at full strength. */
    private static final int SKIP_DISTANCE = 4000;

    private static void useEvent(GameTestHelper helper, String name) {
        SolarEventManager.setEvent(helper.getLevel(), RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id(name)).getInstance());
    }

    /** Strength a 20m voice signal leaves the plot's dipole with, towards a point SKIP_DISTANCE blocks broadside. */
    private static double skipStrength(GameTestHelper helper) {
        StaticAntenna<?> antenna = GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna();
        Band band = Band.getBand("20m");
        AntennaVoicePacket packet = new AntennaVoicePacket(() -> helper.getLevel(), new short[960], band, band.minFrequency(), 1.0D, antenna, UUID.randomUUID());
        return transmitEfficiency(antenna, packet, antenna.getAntennaPos().position().south(SKIP_DISTANCE));
    }

    private static <T extends AntennaData> double transmitEfficiency(StaticAntenna<T> antenna, IAntennaPacket packet, BlockPos destination) {
        return antenna.type.getTransmitEfficiency(packet, antenna.data, destination, false);
    }

    /** By day a blackout closes 20m: skip drops to the closed multiplier of what it is in average weather. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300, batch = "blackout_day")
    public static void blackoutClosesSkipByDay(GameTestHelper helper) {
        SolarEventInstance previous = SolarEventManager.getEvent(helper.getLevel());
        helper.getLevel().setDayTime(6000);
        helper.setBlock(BALUN, RadiocraftBlocks.BALUN_ONE_TO_ONE.get());
        GameTestUtil.wire(helper, BALUN, BALUN.west(5));
        GameTestUtil.wire(helper, BALUN, BALUN.east(5));
        helper.setBlock(STATION, RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getLevel().isDay(), "Not day yet");
                    helper.assertTrue(GameTestUtil.networkObject(helper, BALUN, AntennaNetworkObject.class).getAntenna() != null, "Dipole not recognized yet");
                })
                .thenExecute(() -> {
                    useEvent(helper, "average");
                    double average = skipStrength(helper);
                    helper.assertTrue(average > 0.1D, "20m should skip " + SKIP_DISTANCE + " blocks by day, strength " + average);

                    useEvent(helper, "blackout");
                    double blackout = skipStrength(helper);
                    double expected = average * BandCondition.CLOSED.getSkipMultiplier();
                    helper.assertTrue(Math.abs(blackout - expected) < 1e-9, "Blackout skip was " + blackout + ", expected " + expected);

                    SolarWeatherStationBlockEntity station = GameTestUtil.blockEntity(helper, STATION, SolarWeatherStationBlockEntity.class);
                    for (String band : SolarWeatherStationBlockEntity.REPORTED_BANDS)
                        helper.assertTrue(station.getBandCondition(band) == BandCondition.CLOSED, band + " should be closed by day in a blackout, is " + station.getBandCondition(band));
                    SolarEventManager.setEvent(helper.getLevel(), previous);
                })
                .thenSucceed();
    }

    /** At night a blackout leaves 40m and 80m poor (weak but usable); 10m and 20m stay closed. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100, batch = "blackout_night")
    public static void blackoutLeavesLowBandsPoorAtNight(GameTestHelper helper) {
        SolarEventInstance previous = SolarEventManager.getEvent(helper.getLevel());
        helper.getLevel().setDayTime(18000);
        helper.setBlock(STATION, RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(helper.getLevel().isDay(), "Not night yet"))
                .thenExecute(() -> {
                    useEvent(helper, "blackout");
                    SolarWeatherStationBlockEntity station = GameTestUtil.blockEntity(helper, STATION, SolarWeatherStationBlockEntity.class);
                    helper.assertTrue(station.getBandCondition("10m") == BandCondition.CLOSED, "10m: " + station.getBandCondition("10m"));
                    helper.assertTrue(station.getBandCondition("20m") == BandCondition.CLOSED, "20m: " + station.getBandCondition("20m"));
                    helper.assertTrue(station.getBandCondition("40m") == BandCondition.POOR, "40m: " + station.getBandCondition("40m"));
                    helper.assertTrue(station.getBandCondition("80m") == BandCondition.POOR, "80m: " + station.getBandCondition("80m"));
                    SolarEventManager.setEvent(helper.getLevel(), previous);
                })
                .thenSucceed();
    }
}

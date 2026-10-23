package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SolarWeatherStationBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The solar weather station hears the weather through a radio (maintainer, 2026-10-23): it needs a radio of any kind
 * next to it with a real antenna on its coax. Alone, or with only a dummy load, it reports no signal and its
 * comparator output is 0.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarWeatherStationGameTests {

    private static final String NO_RADIO = "message.radiocraft.solar_weather_station.no_radio";

    private static boolean reportsNoSignal(SolarWeatherStationBlockEntity station) {
        return station.getReport().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && contents.getKey().equals(NO_RADIO);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void aloneItHearsNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
        helper.runAfterDelay(20, () -> {
            SolarWeatherStationBlockEntity station = GameTestUtil.blockEntity(helper, pos, SolarWeatherStationBlockEntity.class);
            helper.assertFalse(station.hasRadio(), "A station on its own thinks it has a radio");
            helper.assertTrue(station.getConditionLevel() == 0, "A station on its own drives a comparator: " + station.getConditionLevel());
            helper.assertTrue(reportsNoSignal(station), "A station on its own reports " + station.getReport().getString());
            helper.succeed();
        });
    }

    /** A dummy load radiates nothing, so a radio with only a dummy load does not count. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
    public static void dummyLoadDoesNotCount(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4), radio = pos.west();
        helper.setBlock(pos, RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
        helper.setBlock(radio, RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.setBlock(radio.north(), RadiocraftBlocks.DUMMY_LOAD.get());
        helper.runAfterDelay(40, () -> {
            SolarWeatherStationBlockEntity station = GameTestUtil.blockEntity(helper, pos, SolarWeatherStationBlockEntity.class);
            helper.assertFalse(station.hasRadio(), "A radio on a dummy load counts as a receiving station");
            helper.assertTrue(reportsNoSignal(station), "With a dummy load the station reports " + station.getReport().getString());
            helper.succeed();
        });
    }

    /** Any radio with a real antenna next to the station brings the report (and the comparator) to life. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 100)
    public static void radioWithAntennaReports(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, RadiocraftBlocks.SOLAR_WEATHER_STATION.get());
        GameTestUtil.radioWithJPole(helper, pos.west());
        helper.succeedWhen(() -> {
            SolarWeatherStationBlockEntity station = GameTestUtil.blockEntity(helper, pos, SolarWeatherStationBlockEntity.class);
            helper.assertTrue(station.hasRadio(), "The station does not see the radio and its J-pole");
            helper.assertFalse(reportsNoSignal(station), "The station still reports no signal");
            helper.assertTrue(station.getCurrentEvent() == null || station.getConditionLevel() > 0, "The station has a radio but its comparator reads 0");
        });
    }
}

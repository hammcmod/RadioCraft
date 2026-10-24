package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.arrl.radiocraft.gametest.TransmissionGameTests.*;

/**
 * Redstone comparators reading RadioCraft blocks: the S-meter on a radio and the condition level of the solar weather
 * station. The radio test uses the two-station layout of {@link TransmissionGameTests} and its own frequency.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ComparatorGameTests {

    private static final int F_METER = 275;

    /**
     * Puts a comparator at {@code pos} reading the block in direction {@code input}, on a stone block so it survives
     * neighbour updates. A comparator only computes its output when a neighbour tells it to, so place it before the
     * block it should read, or change that block afterwards.
     */
    static void placeComparator(GameTestHelper helper, BlockPos pos, Direction input) {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, input));
    }

    static int comparatorOutput(GameTestHelper helper, BlockPos pos) {
        return GameTestUtil.blockEntity(helper, pos, ComparatorBlockEntity.class).getOutputSignal();
    }

    /** A comparator against a radio shows the strength of what it is receiving and drops back to 0 about a second later. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 300)
    public static void comparatorReadsRadioSignalMeter(GameTestHelper helper) {
        BlockPos comparator = RADIO_A.west();
        stationsUp(helper)
                .thenExecute(() -> {
                    setupSsb(helper, RADIO_A, F_METER);
                    setupSsb(helper, RADIO_B, F_METER);
                    placeComparator(helper, comparator, Direction.EAST);
                    helper.assertTrue(radio(helper, RADIO_A).getMeterReading() == 0, "Meter reads " + radio(helper, RADIO_A).getMeterReading() + " before any signal");
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(comparatorOutput(helper, comparator) == 0, "Comparator shows " + comparatorOutput(helper, comparator) + " with nothing received");
                    speakInto(helper, RADIO_B);
                    assertHeard(helper, RADIO_A, EXPECTED_VOICE_STRENGTH);
                    helper.assertTrue(radio(helper, RADIO_A).getMeterReading() == 15, "A 0.97 signal should read 15, got " + radio(helper, RADIO_A).getMeterReading());
                })
                .thenWaitUntil(() -> helper.assertTrue(comparatorOutput(helper, comparator) == 15, "Comparator did not follow the meter: " + comparatorOutput(helper, comparator)))
                .thenExecuteAfter(RadioBlockEntity.METER_HOLD_TICKS + 5, () -> helper.assertTrue(radio(helper, RADIO_A).getMeterReading() == 0, "Meter did not fall back after the signal"))
                .thenWaitUntil(() -> helper.assertTrue(comparatorOutput(helper, comparator) == 0, "Comparator stayed at " + comparatorOutput(helper, comparator)))
                .thenSucceed();
    }

    /** The solar weather station reports the dimension's event and gives a comparator its condition level (1 to 6). */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 200)
    public static void solarWeatherStationReportsConditionsToComparator(GameTestHelper helper) {
        BlockPos station = new BlockPos(8, 2, 8);
        BlockPos comparator = station.east();
        placeComparator(helper, comparator, Direction.WEST);
        helper.setBlock(station, RadiocraftBlocks.TELETYPE.get());
        GameTestUtil.radioWithJPole(helper, station.west()); // The station hears the weather through a radio.
        helper.succeedWhen(() -> {
            SolarEventInstance event = SolarEventManager.getEvent(helper.getLevel());
            helper.assertTrue(event != null, "No solar event for this level");
            TeletypeBlockEntity be = GameTestUtil.blockEntity(helper, station, TeletypeBlockEntity.class);
            helper.assertTrue(be.getCurrentEvent() == event.getEvent(), "Station reports a different event than the manager");
            helper.assertTrue(be.getCurrentEventKey() != null, "Station does not know the event's id");
            int level = be.getConditionLevel();
            helper.assertTrue(level >= 1 && level <= RadiocraftData.SOLAR_EVENTS.getEvents().size(), "Condition level out of range: " + level);
            helper.assertTrue(level == RadiocraftData.SOLAR_EVENTS.conditionLevel(event.getEvent()), "Condition level does not match the event");
            helper.assertTrue(be.isReportedBandOpen() == event.getEvent().isBandOpen(TeletypeBlockEntity.REPORTED_BAND, helper.getLevel().isDay()), "10m open flag disagrees with the event");
            helper.assertTrue(comparatorOutput(helper, comparator) == level, "Comparator shows " + comparatorOutput(helper, comparator) + ", station level is " + level);
        });
    }
}

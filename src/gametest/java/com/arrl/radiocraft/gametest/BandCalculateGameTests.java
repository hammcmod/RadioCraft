package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.commands.BandCalculateCommands;
import com.arrl.radiocraft.common.commands.BandCalculateCommands.Path;
import com.arrl.radiocraft.common.commands.BandCalculateCommands.Result;
import com.arrl.radiocraft.common.radio.solar.BandCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** {@code /bandcalculate} (proposal admin command, 2026-10-23): the best band between two points from ranges and conditions. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BandCalculateGameTests {

    /** Close together, every band reaches by line of sight. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void closeStationsUseLineOfSight(GameTestHelper helper) {
        List<Result> results = BandCalculateCommands.calculate(helper.getLevel(), BlockPos.ZERO, new BlockPos(100, 0, 0));
        helper.assertFalse(results.isEmpty(), "No bands configured");
        helper.assertTrue(results.stream().allMatch(result -> result.path() == Path.LINE_OF_SIGHT), "A band does not reach 100 blocks: " + results);
        helper.succeed();
    }

    /** Far apart, only skip can carry, VHF never does without an aurora, and the best band is listed first. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void farStationsNeedSkip(GameTestHelper helper) {
        List<Result> results = BandCalculateCommands.calculate(helper.getLevel(), BlockPos.ZERO, new BlockPos(0, 0, 2500));
        for(Result result : results) {
            helper.assertFalse(result.path() == Path.LINE_OF_SIGHT, result.band().name() + " reaches 2500 blocks by line of sight");
            if(result.path() == Path.SKIP)
                helper.assertTrue(result.condition() != BandCondition.CLOSED, result.band().name() + " skips while closed");
        }
        for(int i = 1; i < results.size(); i++)
            helper.assertTrue(results.get(i - 1).score() >= results.get(i).score(), "Results are not best first: " + results);
        helper.succeed();
    }
}

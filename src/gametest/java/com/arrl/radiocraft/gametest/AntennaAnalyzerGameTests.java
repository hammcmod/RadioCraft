package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaAnalysis;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The antenna analyzer right clicked on everything a station is made of never throws, and reads a J-pole as a 2m antenna. */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AntennaAnalyzerGameTests {

    private static final String BATCH = "antenna_analyzer";

    private static void click(GameTestHelper helper, Player player, BlockPos pos) {
        ItemStack analyzer = new ItemStack(RadiocraftItems.ANTENNA_ANALYZER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, analyzer);
        BlockPos abs = helper.absolutePos(pos);
        try {
            analyzer.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        } catch(RuntimeException e) {
            helper.fail("Analyzer threw on " + helper.getLevel().getBlockState(abs) + ": " + e, pos);
        }
    }

    @GameTest(template = GameTestUtil.EMPTY, batch = BATCH, timeoutTicks = 100)
    public static void analyzerClicksStationParts(GameTestHelper helper) {
        BlockPos radio = new BlockPos(2, 1, 1);
        GameTestUtil.radioWithJPole(helper, radio);
        helper.runAfterDelay(10, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos[] targets = { radio, radio.south(), radio.south(2), radio.south(3), new BlockPos(0, 0, 0) };
            for(BlockPos pos : targets)
                click(helper, player, pos);
            player.getInventory().add(new ItemStack(RadiocraftItems.VHF_HANDHELD.get()));
            helper.runAfterDelay(5, () -> {
                for(BlockPos pos : targets)
                    click(helper, player, pos);
                StaticAntenna<?> jPole = AntennaTypes.match(helper.getLevel(), helper.absolutePos(radio.south(2)));
                if(jPole == null)
                    helper.fail("No J-pole recognized", radio.south(2));
                AntennaAnalysis.Result result = AntennaAnalysis.analyze(f -> jPole.getSWR((float) f), Band.getDefaults());
                helper.assertTrue(result.verdict() == AntennaAnalysis.Verdict.RESONANT && "2m".equals(result.best().name()),
                        "J-pole should read resonant on 2m, read " + result.verdict() + " on " + (result.best() == null ? "none" : result.best().name()) + " SWR " + result.bestSwr());
                helper.succeed();
            });
        });
    }
}

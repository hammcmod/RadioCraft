package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.compat.sable.SableAssembly;
import com.arrl.radiocraft.compat.sable.Sublevels;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Sable support (compat/sable). The GameTest server runs without Sable, so this checks that Sable Companion loads and
 * leaves every position alone, and that the wire carrying done on assembly moves exactly the wires whose two ends went
 * with the ship. Radios on a real airship need runClient -PwithSable (doc/NeedsTesting.md).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SableGameTests {

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void withoutSableEveryPositionIsItsOwn(GameTestHelper helper) {
        if(ModList.get().isLoaded("sable")) { // Only meaningful without Sable.
            helper.succeed();
            return;
        }
        BlockPos a = helper.absolutePos(new BlockPos(2, 2, 3)), b = helper.absolutePos(new BlockPos(9, 4, 12));
        var level = helper.getLevel();
        helper.assertFalse(Sublevels.onSublevel(level, a), "A ground block is on a sub-level");
        helper.assertTrue(Sublevels.world(level, a).equals(a), "world() moved a ground block");
        helper.assertTrue(Sublevels.worldCenter(level, a).equals(Vec3.atCenterOf(a)), "worldCenter() moved a ground block");
        helper.assertTrue(Sublevels.inFrameOf(level, a, b).equals(b), "inFrameOf() moved a ground block");
        helper.assertTrue(Sublevels.distSqr(level, a, b) == a.distSqr(b), "distSqr() differs from plain distance");
        helper.assertTrue(Sublevels.frame(level, a).isGround(), "A ground block has a ship's frame");
        Vec3 point = new Vec3(1.5D, 2.25D, -3.0D);
        helper.assertTrue(Sublevels.Frame.GROUND.apply(point).equals(point), "The ground frame moves points");
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 60)
    public static void assemblyCarriesWiresWithBothEndsAboard(GameTestHelper helper) {
        // Two wires: one with both poles going aboard, one with only its start going.
        AntennaWire aboard = GameTestUtil.wire(helper, place(helper, new BlockPos(1, 2, 1)), new BlockPos(5, 2, 1));
        AntennaWire halfway = GameTestUtil.wire(helper, place(helper, new BlockPos(1, 2, 4)), new BlockPos(7, 2, 4));
        List<BlockPos> blocks = List.of(helper.absolutePos(new BlockPos(1, 2, 1)), helper.absolutePos(new BlockPos(5, 2, 1)),
                helper.absolutePos(new BlockPos(1, 2, 4)));
        // What Sable does first: the blocks go to their new places (here, six up) and leave their old ones.
        for(BlockPos block : blocks) {
            helper.getLevel().setBlockAndUpdate(block.above(6), helper.getLevel().getBlockState(block));
            helper.getLevel().setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
        }
        int carried = SableAssembly.carryWires(helper.getLevel(), pos -> pos.above(6), blocks);
        helper.assertTrue(carried == 1, "Carried " + carried + " wires, expected 1");
        helper.assertTrue(aboard.getStartPos().equals(helper.absolutePos(new BlockPos(1, 8, 1))), "The aboard wire's start is at " + aboard.getStartPos());
        helper.assertTrue(aboard.getEndPos().equals(helper.absolutePos(new BlockPos(5, 8, 1))), "The aboard wire's end is at " + aboard.getEndPos());
        helper.assertTrue(halfway.getStartPos().equals(helper.absolutePos(new BlockPos(1, 2, 4))), "The wire left behind moved");
        // Still strung between poles a while later (wires drop within 3 s when their end is not on a holder).
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(aboard.isRemoved(), "The carried wire dropped");
            helper.succeed();
        });
    }

    private static BlockPos place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, RadiocraftBlocks.ANTENNA_POLE.get());
        return pos;
    }
}

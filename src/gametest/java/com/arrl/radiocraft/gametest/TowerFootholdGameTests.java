package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.TowerFootholdBlock;
import com.arrl.radiocraft.common.commands.GuideCommands;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Tower footholds: an old one gives way under a player (the break chance set to certain here), a crafted one never
 * does; a broken one cannot be climbed and drops nothing. And the guide book exists (Patchouli is in the dev runtime).
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TowerFootholdGameTests {

    private static final BlockPos WALL = new BlockPos(4, 2, 5);
    private static final BlockPos OLD = WALL.north();
    private static final BlockPos NEW = WALL.north().east();

    private static BlockState foothold(TowerFootholdBlock block) {
        return block.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40, batch = "tower_foothold")
    public static void oldFootholdGivesWayButCraftedOneHolds(GameTestHelper helper) {
        double chance = RadiocraftServerConfig.TOWER_FOOTHOLD_BREAK_CHANCE.get();
        RadiocraftServerConfig.TOWER_FOOTHOLD_BREAK_CHANCE.set(1.0D);
        helper.setBlock(WALL, Blocks.STONE);
        helper.setBlock(WALL.east(), Blocks.STONE);
        helper.setBlock(OLD, foothold(RadiocraftBlocks.OLD_TOWER_FOOTHOLD.get()));
        helper.setBlock(NEW, foothold(RadiocraftBlocks.TOWER_FOOTHOLD.get()));
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("climber".getBytes()), "climber"));
        try {
            BlockPos old = helper.absolutePos(OLD), crafted = helper.absolutePos(NEW);
            helper.assertTrue(helper.getBlockState(OLD).isLadder(helper.getLevel(), old, player), "A whole foothold should be climbable");
            RadiocraftBlocks.OLD_TOWER_FOOTHOLD.get().playerArrived(helper.getLevel(), old, helper.getBlockState(OLD), player);
            RadiocraftBlocks.TOWER_FOOTHOLD.get().playerArrived(helper.getLevel(), crafted, helper.getBlockState(NEW), player);
            helper.assertTrue(helper.getBlockState(OLD).getValue(TowerFootholdBlock.BROKEN), "The old foothold did not give way");
            helper.assertFalse(helper.getBlockState(NEW).getValue(TowerFootholdBlock.BROKEN), "A crafted foothold gave way");
            helper.assertFalse(helper.getBlockState(OLD).isLadder(helper.getLevel(), old, player), "A broken foothold can still be climbed");
            helper.assertTrue(helper.getBlockState(OLD).getCollisionShape(helper.getLevel(), old).isEmpty(), "A broken foothold can still be stood on");
            List<ItemStack> scrap = Block.getDrops(helper.getBlockState(OLD), helper.getLevel(), old, null);
            helper.assertTrue(scrap.isEmpty(), "A broken foothold dropped " + scrap);
            List<ItemStack> whole = Block.getDrops(helper.getBlockState(NEW), helper.getLevel(), crafted, null);
            helper.assertTrue(whole.size() == 1 && whole.getFirst().is(RadiocraftBlocks.TOWER_FOOTHOLD.get().asItem()), "A whole foothold dropped " + whole);
        } finally {
            RadiocraftServerConfig.TOWER_FOOTHOLD_BREAK_CHANCE.set(chance);
        }
        helper.succeed();
    }

    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void guideBookIsTheRadiocraftBook(GameTestHelper helper) {
        ItemStack book = GuideCommands.guideBook();
        helper.assertFalse(book.isEmpty(), "No guide book: is Patchouli missing from the dev runtime?");
        helper.assertTrue(book.getComponents().toString().contains("radiocraft:guide"), "The book is not the RadioCraft guide: " + book.getComponents());
        helper.succeed();
    }
}

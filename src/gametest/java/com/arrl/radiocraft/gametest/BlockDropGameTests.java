package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.datagen.RadiocraftBlockLootProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Block drops (maintainer, 2026-10-23): every craftable block gives its item back when broken with a pickaxe, and
 * wires break by hand. Before this the mod had no loot tables at all, so broken blocks were lost.
 */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlockDropGameTests {

    /** Each block's loot table, mined with an iron pickaxe, gives back exactly its own item. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void everyBlockDropsItself(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        List<String> problems = new ArrayList<>();
        for(Block block : RadiocraftBlockLootProvider.blocks()) {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            BlockState state = block.defaultBlockState();
            if(block instanceof DoubleVHFAntennaBlock)
                state = state.setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER);
            if(state.requiresCorrectToolForDrops() && !pickaxe.isCorrectToolForDrops(state))
                problems.add(name + " cannot be mined with a pickaxe");
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pos, null, null, pickaxe);
            if(drops.size() != 1 || !drops.get(0).is(block.asItem()) || drops.get(0).getCount() != 1)
                problems.add(name + " drops " + drops);
            if(block instanceof DoubleVHFAntennaBlock
                    && !Block.getDrops(state.setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER), helper.getLevel(), pos, null, null, pickaxe).isEmpty())
                problems.add(name + "'s top half drops a second antenna");
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** Wire is instant to break and needs no tool, so a bare hand picks it back up. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void wiresBreakByHand(GameTestHelper helper) {
        for(Block wire : List.of(RadiocraftBlocks.WIRE.get(), RadiocraftBlocks.WATERPROOF_WIRE.get(), RadiocraftBlocks.COAX_WIRE.get()))
            helper.assertFalse(wire.defaultBlockState().requiresCorrectToolForDrops(), BuiltInRegistries.BLOCK.getKey(wire) + " needs a tool to drop");
        helper.succeed();
    }

    /** Breaking a two-tall VHF antenna from either half in survival drops one antenna. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void tallAntennaDropsOnce(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(2, 2, 2), other = new BlockPos(6, 2, 2);
        Block jPole = RadiocraftBlocks.J_POLE_ANTENNA.get();
        placeTall(helper, jPole, bottom);
        placeTall(helper, jPole, other);
        helper.getLevel().destroyBlock(helper.absolutePos(bottom), true);
        helper.getLevel().destroyBlock(helper.absolutePos(other.above()), true);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(jPole, bottom.above());
            helper.assertBlockNotPresent(jPole, other);
            int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(helper.absolutePos(bottom)).inflate(6.0D))
                    .stream().filter(item -> item.getItem().is(jPole.asItem())).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(dropped == 2, "Two antennas broken dropped " + dropped);
            helper.succeed();
        });
    }

    private static void placeTall(GameTestHelper helper, Block block, BlockPos bottom) {
        helper.setBlock(bottom, block.defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(bottom.above(), block.defaultBlockState().setValue(DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.UPPER));
    }
}

package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
            if(block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
                state = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            if(state.requiresCorrectToolForDrops() && !pickaxe.isCorrectToolForDrops(state))
                problems.add(name + " cannot be mined with a pickaxe");
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pos, null, null, pickaxe);
            if(drops.size() != 1 || !drops.get(0).is(block.asItem()) || drops.get(0).getCount() != 1)
                problems.add(name + " drops " + drops);
            if(block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && !Block.getDrops(state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), helper.getLevel(), pos, null, null, pickaxe).isEmpty())
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

    /** Breaking a two-tall block (J-pole, ionosonde, ionospheric processor) from either half in survival drops one. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 40)
    public static void tallBlocksDropOnce(GameTestHelper helper) {
        List<Block> tall = List.of(RadiocraftBlocks.J_POLE_ANTENNA.get(), RadiocraftBlocks.IONOSONDE.get(), RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get());
        for(int i = 0; i < tall.size(); i++) {
            BlockPos bottom = new BlockPos(2, 2, 2 + i * 5), other = new BlockPos(9, 2, 2 + i * 5);
            placeTall(helper, tall.get(i), bottom);
            placeTall(helper, tall.get(i), other);
            helper.getLevel().destroyBlock(helper.absolutePos(bottom), true);
            helper.getLevel().destroyBlock(helper.absolutePos(other.above()), true);
        }
        helper.runAfterDelay(2, () -> {
            for(int i = 0; i < tall.size(); i++) {
                Block block = tall.get(i);
                BlockPos bottom = new BlockPos(2, 2, 2 + i * 5), other = new BlockPos(9, 2, 2 + i * 5);
                helper.assertBlockNotPresent(block, bottom.above());
                helper.assertBlockNotPresent(block, other);
                int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                                new net.minecraft.world.phys.AABB(helper.absolutePos(bottom)).inflate(1.0D, 2.0D, 1.0D)
                                        .minmax(new net.minecraft.world.phys.AABB(helper.absolutePos(other)).inflate(1.0D, 2.0D, 1.0D)))
                        .stream().filter(item -> item.getItem().is(block.asItem())).mapToInt(item -> item.getItem().getCount()).sum();
                helper.assertTrue(dropped == 2, "Two " + BuiltInRegistries.BLOCK.getKey(block) + " broken dropped " + dropped);
            }
            helper.succeed();
        });
    }

    /**
     * The desk mic moves to the side its radio is on, seen from its front: facing north (front to the north, the
     * player looking south), a radio to the east is on the left.
     */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void deskMicStandsBySideOfItsRadio(GameTestHelper helper) {
        BlockPos mic = new BlockPos(5, 2, 5);
        helper.setBlock(mic, RadiocraftBlocks.MICROPHONE.get().defaultBlockState().setValue(com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock.FACING, net.minecraft.core.Direction.NORTH));
        helper.setBlock(mic.west(), RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.assertTrue(helper.getBlockState(mic).getValue(com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock.SIDE) == com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock.Side.RIGHT,
                "A radio to the west of a north-facing mic is not on its right");
        helper.setBlock(mic.west(), net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(mic.east(), RadiocraftBlocks.VHF_BASE_STATION.get());
        helper.assertTrue(helper.getBlockState(mic).getValue(com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock.SIDE) == com.arrl.radiocraft.common.blocks.radios.MicrophoneBlock.Side.LEFT,
                "A radio to the east of a north-facing mic is not on its left");
        helper.succeed();
    }

    /** The processor's valves light the room from its top half; the bottom half gives no light. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void processorValvesGiveLight(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(3, 2, 3);
        placeTall(helper, RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get(), bottom);
        helper.assertTrue(helper.getBlockState(bottom.above()).getLightEmission() == com.arrl.radiocraft.common.blocks.IonosphericProcessorBlock.VALVE_LIGHT,
                "The top half gives light " + helper.getBlockState(bottom.above()).getLightEmission());
        helper.assertTrue(helper.getBlockState(bottom).getLightEmission() == 0, "The bottom half gives light");
        helper.succeed();
    }

    private static void placeTall(GameTestHelper helper, Block block, BlockPos bottom) {
        helper.setBlock(bottom, block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(bottom.above(), block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }
}

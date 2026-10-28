package com.arrl.radiocraft.datagen;

import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import com.arrl.radiocraft.common.blocks.antennas.FiveBtvAntennaBlock;
import com.arrl.radiocraft.common.blocks.TowerFootholdBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/** Every RadioCraft block drops its own item when broken. */
public class RadiocraftBlockLootProvider extends BlockLootSubProvider {

    public RadiocraftBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    /**
     * The blocks given a loot table. Three drop themselves in code instead: the passthrough (itself plus the wall block
     * it replaced), the desk charger (itself, with the radio it holds) and the wire bundle (the cables in it).
     */
    public static List<Block> blocks() {
        return RadiocraftBlocks.BLOCKS.getEntries().stream()
                .<Block>map(holder -> holder.get())
                .filter(block -> block != RadiocraftBlocks.PASSTHROUGH.get() && block != RadiocraftBlocks.DESK_CHARGER.get() && block != RadiocraftBlocks.WIRE_BUNDLE.get())
                .toList();
    }

    @Override
    protected void generate() {
        for(Block block : blocks()) {
            if(block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) // Two blocks tall: only the bottom half drops, like a door.
                add(block, createSinglePropConditionTable(block, BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
            else if(block.defaultBlockState().hasProperty(FiveBtvAntennaBlock.SEGMENT)) // Seven blocks tall: only the base drops.
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FiveBtvAntennaBlock.SEGMENT, 0)))))));
            else if(block instanceof com.arrl.radiocraft.common.blocks.MicrowaveHornBlock) // Sixteen parts, one horn: only the placed part drops.
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.PART_X, com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.ORIGIN_X)
                                        .hasProperty(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.PART_Y, com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.ORIGIN_Y)
                                        .hasProperty(com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.PART_Z, com.arrl.radiocraft.common.blocks.MicrowaveHornBlock.ORIGIN_Z)))))));
            else if(block == RadiocraftBlocks.COMMERCIAL_RADIO_MODULE.get()) // Sealed: breaking it out gives one rare part, from the bottom cell.
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, rareParts(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(com.arrl.radiocraft.common.blocks.StackedBlocks.PART, 0))))));
            else if(block.defaultBlockState().hasProperty(com.arrl.radiocraft.common.blocks.StackedBlocks.PART)) // Four tall: only the bottom drops.
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(com.arrl.radiocraft.common.blocks.StackedBlocks.PART, 0)))))));
            else if(block instanceof TowerFootholdBlock) // A broken foothold is scrap: nothing drops.
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(TowerFootholdBlock.BROKEN, false)))))));
            else
                dropSelf(block);
        }
    }

    /** A pure encased crystal of any band, the all-band board, or the ionospheric processor. */
    private static LootPool.Builder rareParts(LootPool.Builder pool) {
        for(var byQuality : com.arrl.radiocraft.common.init.RadiocraftItems.ENCASED_CRYSTALS.values())
            pool.add(LootItem.lootTableItem(byQuality.get(com.arrl.radiocraft.common.radio.crystal.CrystalQuality.PURE).get()).setWeight(4));
        pool.add(LootItem.lootTableItem(com.arrl.radiocraft.common.init.RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get()).setWeight(3));
        pool.add(LootItem.lootTableItem(com.arrl.radiocraft.common.init.RadiocraftItems.IONOSPHERIC_PROCESSOR.get()).setWeight(1));
        return pool;
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return blocks();
    }
}

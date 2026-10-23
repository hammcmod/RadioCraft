package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
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
     * The blocks given a loot table. Two drop themselves in code instead: the passthrough (itself plus the wall block
     * it replaced) and the desk charger (itself, with the radio it holds).
     */
    public static List<Block> blocks() {
        return RadiocraftBlocks.BLOCKS.getEntries().stream()
                .<Block>map(holder -> holder.get())
                .filter(block -> block != RadiocraftBlocks.PASSTHROUGH.get() && block != RadiocraftBlocks.DESK_CHARGER.get())
                .toList();
    }

    @Override
    protected void generate() {
        for(Block block : blocks()) {
            if(block instanceof DoubleVHFAntennaBlock) // Two blocks tall: only the bottom half drops, like a door.
                add(block, createSinglePropConditionTable(block, DoubleVHFAntennaBlock.HALF, DoubleBlockHalf.LOWER));
            else
                dropSelf(block);
        }
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return blocks();
    }
}

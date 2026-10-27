package com.arrl.radiocraft.common.blocks.antennas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A commercial two-bay folded dipole for 2m, two blocks tall: a mast pipe on the side it faces (where the tower is),
 * with two folded loops standing off it on arms. DC grounded. Not craftable: found on towers in structures.
 */
public class FoldedDipoleAntennaBlock extends DoubleVHFAntennaBlock {
    private static final VoxelShape NORTH = Block.box(6.5D, 0.0D, 0.5D, 9.5D, 16.0D, 15.0D);
    private static final VoxelShape SOUTH = Block.box(6.5D, 0.0D, 1.0D, 9.5D, 16.0D, 15.5D);
    private static final VoxelShape EAST = Block.box(1.0D, 0.0D, 6.5D, 15.5D, 16.0D, 9.5D);
    private static final VoxelShape WEST = Block.box(0.5D, 0.0D, 6.5D, 15.0D, 16.0D, 9.5D);

    public FoldedDipoleAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch(facing) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.folded_dipole_antenna"));
    }
}

package com.arrl.radiocraft.common.blocks.antennas;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** The Ringo: a commercial 2m vertical, two blocks tall, with its matching ring near the foot. DC grounded. */
public class RingoAntennaBlock extends DoubleVHFAntennaBlock {
    public static final VoxelShape BOTTOM = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    public static final VoxelShape TOP = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 15.0D, 9.0D);

    public RingoAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? BOTTOM : TOP;
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D); // Walk past the ring; the mast is solid.
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.ringo_antenna"));
    }
}

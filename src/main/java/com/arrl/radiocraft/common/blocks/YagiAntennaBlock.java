package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class YagiAntennaBlock extends DoubleVHFAntennaBlock {
    public static final VoxelShape POST = Block.box(11.0D, 0.0D, 6.0D, 15.0D, 16.0D, 10.0D);

    public YagiAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return rotateHorizontalPlaneDirection(Direction.WEST, state.getValue(FACING), POST);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return rotateHorizontalPlaneDirection(Direction.WEST, state.getValue(FACING), POST);
    }
}

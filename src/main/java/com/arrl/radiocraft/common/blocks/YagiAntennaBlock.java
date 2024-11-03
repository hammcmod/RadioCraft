package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class YagiAntennaBlock extends DoubleVHFAntennaBlock {

    public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

    static {
        SHAPES.put(Direction.NORTH, Block.box(5.0D, 0.0D, 10.0D, 11.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.SOUTH, Block.box(5.0D, 0.0D, 0.0D, 11.0D, 16.0D, 6.0D));
        SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 5.0D, 6.0D, 16.0D, 11.0D));
        SHAPES.put(Direction.WEST, Block.box(10.0D, 0.0D, 5.0D, 16.0D, 16.0D, 11.0D));
    }

    public YagiAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

}

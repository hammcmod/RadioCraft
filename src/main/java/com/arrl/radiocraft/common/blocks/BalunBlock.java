package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class BalunBlock extends AntennaCenterBlock {

    public static final DirectionProperty PLACED_ON = DirectionProperty.create("placed_on");
    public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

    static {
        SHAPES.put(Direction.NORTH, Block.box(6.0D, 6.0D, 8.0D, 10.0D, 10.0D, 16.0D));
        SHAPES.put(Direction.SOUTH, Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 8.0D));
        SHAPES.put(Direction.EAST, Block.box(0.0D, 6.0D, 6.0D, 8.0D, 10.0D, 10.0D));
        SHAPES.put(Direction.WEST, Block.box(8.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D));
        SHAPES.put(Direction.UP, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D));
        SHAPES.put(Direction.DOWN, Block.box(6.0D, 8.0D, 6.0D, 10.0D, 16.0D, 10.0D));
    }

    public BalunBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(PLACED_ON);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(PLACED_ON));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PLACED_ON, context.getClickedFace());
    }
}

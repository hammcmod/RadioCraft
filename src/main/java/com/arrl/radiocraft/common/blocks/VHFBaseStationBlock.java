package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;

public class VHFBaseStationBlock extends Block {

	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(2.0D, 0.0D, 2.0D, 14.0D, 5.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(2.0D, 0.0D, 0.0D, 14.0D, 5.0D, 14.0D));
		SHAPES.put(Direction.WEST, Block.box(2.0D, 0.0D, 2.0D, 16.0D, 5.0D, 14.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 2.0D, 14.0D, 5.0D, 14.0D));
	}

	public VHFBaseStationBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AbstractRadioBlock.HORIZONTAL_FACING));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(AbstractRadioBlock.HORIZONTAL_FACING);
	}

}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.radio.QRPRadio40mBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.HashMap;

public class QRPRadio40mBlock extends RadioBlock {

	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(0.0D, 0.0D, 5.0D, 16.0D, 7.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 11.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 0.0D, 11.0D, 7.0D, 16.0D));
		SHAPES.put(Direction.WEST, Block.box(5.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D));
	}

	public QRPRadio40mBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new QRPRadio40mBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(RadioBlock.HORIZONTAL_FACING));
	}

}

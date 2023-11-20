package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.VHFReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class VHFReceiverBlock extends RadioBlock {

	public static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 3.0D, 12.0D, 3.0D, 16.0D);

	public VHFReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(RadioBlock.HORIZONTAL_FACING);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VHFReceiverBlockEntity(pos, state);
	}

}

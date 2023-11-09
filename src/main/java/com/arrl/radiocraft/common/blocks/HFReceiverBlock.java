package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.HFReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HFReceiverBlock extends RadioBlock {

	public static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 5.0D, 14.0D, 7.0D, 16.0D);

	public HFReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HFReceiverBlockEntity(pos, state);
	}

}

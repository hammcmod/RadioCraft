package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.radio.VHFReceiverBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class VHFReceiverBlock extends RadioBlock {

	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(4.0D, 0.0D, 3.0D, 12.0D, 3.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 13.0D));
		SHAPES.put(Direction.WEST, Block.box(3.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 4.0D, 13.0D, 3.0D, 12.0D));
	}

	public VHFReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPES.get(state.getValue(RadioBlock.HORIZONTAL_FACING));
	}

    @Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new VHFReceiverBlockEntity(pos, state);
	}

}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class HFRadio10mBlock extends AbstractRadioBlock {

	public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, Block.box(1.0D, 0.0D, 3.0D, 15.0D, 5.0D, 16.0D));
		SHAPES.put(Direction.SOUTH, Block.box(1.0D, 0.0D, 0.0D, 15.0D, 5.0D, 13.0D));
		SHAPES.put(Direction.WEST, Block.box(3.0D, 0.0D, 1.0D, 16.0D, 5.0D, 15.0D));
		SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 1.0D, 13.0D, 5.0D, 15.0D));
	}

	public HFRadio10mBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HFRadio10mBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AbstractRadioBlock.HORIZONTAL_FACING));
	}

}

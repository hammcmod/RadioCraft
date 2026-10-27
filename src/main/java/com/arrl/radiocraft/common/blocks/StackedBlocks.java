package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * For a block placed and broken as a stack of {@code height} cells (the commercial repeater and radio module), each
 * cell's place in it in an integer property, 0 at the bottom: the five-BTV antenna's way, shared. The bottom cell holds
 * the block entity and drops the item.
 */
public final class StackedBlocks {

	/** A cell's place in a four-tall stack, 0 at the bottom. Only cell 0 drops (see the block loot provider). */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
	public static final int HEIGHT = 4;

	private StackedBlocks() {
	}

	/** True if the cells above {@code context}'s position are free for the rest of the stack. */
	public static boolean hasRoom(BlockPlaceContext context, int height) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if(pos.getY() + height > level.getMaxBuildHeight())
			return false;
		for(int i = 1; i < height; i++)
			if(!level.getBlockState(pos.above(i)).canBeReplaced(context))
				return false;
		return true;
	}

	/** Places the cells above the bottom one, each {@code bottom} with its own place in the stack. */
	public static void placeAbove(Level level, BlockPos pos, BlockState bottom, IntegerProperty part, int height) {
		for(int i = 1; i < height; i++)
			level.setBlock(pos.above(i), bottom.setValue(part, i), Block.UPDATE_ALL);
	}

	/** True if the cell lost the one above or below it in its stack (after a neighbour update): it goes too. */
	public static boolean brokenApart(BlockState state, Direction direction, BlockState neighbour, IntegerProperty part, int height) {
		int cell = state.getValue(part);
		if(direction == Direction.UP && cell < height - 1)
			return !(neighbour.is(state.getBlock()) && neighbour.getValue(part) == cell + 1);
		if(direction == Direction.DOWN && cell > 0)
			return !(neighbour.is(state.getBlock()) && neighbour.getValue(part) == cell - 1);
		return false;
	}

	/** A cell above the bottom survives only standing on the cell below it. */
	public static boolean canSurvive(BlockState state, LevelReader level, BlockPos pos, IntegerProperty part) {
		int cell = state.getValue(part);
		if(cell == 0)
			return true;
		BlockState below = level.getBlockState(pos.below());
		return below.is(state.getBlock()) && below.getValue(part) == cell - 1;
	}

	/** The bottom cell of the stack {@code pos} is in. */
	public static BlockPos bottom(BlockPos pos, BlockState state, IntegerProperty part) {
		return pos.below(state.getValue(part));
	}

	/** In creative, breaking an upper cell must not let the bottom drop the item as the rest comes down. */
	public static void creativeBreak(Level level, BlockPos pos, BlockState state, Player player, IntegerProperty part) {
		int cell = state.getValue(part);
		if(level.isClientSide || !player.isCreative() || cell == 0)
			return;
		BlockPos base = pos.below(cell);
		BlockState baseState = level.getBlockState(base);
		if(baseState.is(state.getBlock()) && baseState.getValue(part) == 0) {
			level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			level.levelEvent(player, 2001, base, Block.getId(baseState));
		}
	}
}

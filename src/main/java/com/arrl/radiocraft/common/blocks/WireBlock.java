package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WireBlock extends Block {

	public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
	public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
	public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
	public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;

	public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(
			ImmutableMap.of(
					Direction.NORTH, NORTH,
					Direction.EAST, EAST,
					Direction.SOUTH, SOUTH,
					Direction.WEST, WEST));

	public WireBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(NORTH, RedstoneSide.NONE)
				.setValue(EAST, RedstoneSide.NONE)
				.setValue(SOUTH, RedstoneSide.NONE)
				.setValue(WEST, RedstoneSide.NONE)
		);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getConnectionState(context.getLevel(), context.getClickedPos());
	}

	/**
	 * Calculate desired blockstate for a wire given it's connections.
	 */
	public BlockState getConnectionState(Level level, BlockPos pos) {
		BlockState state = defaultBlockState();

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos checkPos = pos.relative(direction);

			if(level.getBlockState(checkPos.above()).getBlock() == RadiocraftBlocks.WIRE.get())
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.UP);
			else if(level.getBlockState(checkPos).getBlock() == RadiocraftBlocks.WIRE.get())
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.SIDE);
			else if(level.getBlockState(checkPos.below()).getBlock() == RadiocraftBlocks.WIRE.get())
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.SIDE);
		}

		return state;
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		BlockState newState = getConnectionState(level, pos);
		if(!state.equals(newState))
			level.setBlockAndUpdate(pos, newState);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}
}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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
	public BlockState getConnectionState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState();

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos checkPos = pos.relative(direction);
			BlockState checkState = level.getBlockState(checkPos);
			boolean validPlatform = canSurviveOn(level, checkPos, checkState);
			boolean nonSolidAbove = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos);


			if (nonSolidAbove && validPlatform && canConnectTo(level, checkPos.above())) // If block above is connectable
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.UP);
			else if(canConnectTo(level, checkPos)) // Block at side is connectable
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.SIDE);
			else if(!checkState.isRedstoneConductor(level, pos) && canConnectTo(level, checkPos.below())) // If side block is not solid & below is connectable
				state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), RedstoneSide.SIDE);

		}

		return state;
	}

	/**
	 * Get pos for all connected wires in a given direction
	 */
	public List<BlockPos> getConnections(BlockGetter level, BlockPos pos, Direction direction) {
		List<BlockPos> out = new ArrayList<>();
		if(!direction.getAxis().isHorizontal())
			return out;

		BlockPos checkPos = pos.relative(direction);


		BlockState checkState = level.getBlockState(checkPos);
		boolean validPlatform = canSurviveOn(level, checkPos, checkState);
		boolean nonSolidAbove = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos);

		if (nonSolidAbove && validPlatform && canConnectTo(level, checkPos.above())) // If block above is connectable
			out.add(checkPos.above());
		else if(canConnectTo(level, checkPos)) // Block at side is connectable
			out.add(checkPos);
		else if(!checkState.isRedstoneConductor(level, pos) && canConnectTo(level, checkPos.below())) // If side block is not solid & below is connectable
			out.add(checkPos.below());

		return out;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(level.isClientSide)
			return;
		if(oldState.is(this))
			return;

		PowerUtils.mergeWireNetworks(level, pos);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(!newState.is(this)) {
			PowerUtils.splitWireNetwork(level, pos);
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
		if(direction == Direction.DOWN) // Don't need to worry about updates below
			return state;
		// Recalculate connections if not down.
		return getConnectionState(level, currentPos);
	}

	@Override
	public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
		MutableBlockPos mutablePos = new MutableBlockPos(); // MutablePos to reduce resource usage
		for(Direction direction : Plane.HORIZONTAL) {
			if(state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() // Only update connected sides to reduce lag
					&& !level.getBlockState(mutablePos.setWithOffset(pos, direction)).is(this)) { // Do not update direct neighbours

				mutablePos.move(Direction.DOWN);
				if (level.getBlockState(mutablePos).is(this)) { // Only update other wires
					BlockPos blockpos = mutablePos.relative(direction.getOpposite());
					level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(blockpos), mutablePos, blockpos, flags, recursionLeft);
				}

				mutablePos.setWithOffset(pos, direction).move(Direction.UP); // Repetition is a bit ugly, fix later.
				if (level.getBlockState(mutablePos).is(this)) {
					BlockPos blockPos = mutablePos.relative(direction.getOpposite());
					level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(blockPos), mutablePos, blockPos, flags, recursionLeft);
				}

			}
		}
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	private boolean canSurviveOn(BlockGetter level, BlockPos pos, BlockState state) {
		return state.isFaceSturdy(level, pos, Direction.UP);
	}

	private boolean canConnectTo(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).is(this) || level.getBlockEntity(pos) instanceof IPowerNetworkItem;
	}

}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import com.arrl.radiocraft.common.benetworks.power.WireUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WireBlock extends Block {

	public final boolean isPower;

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty ON_GROUND = BooleanProperty.create("on_ground");

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN));

	public WireBlock(Properties properties, boolean isPower) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false).setValue(ON_GROUND, false));
		this.isPower = isPower;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, ON_GROUND);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getBlockState(context.getLevel(), context.getClickedPos());
	}

	/**
	 * Calculate desired blockstate for a wire given its connections.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState();

		state = state.setValue(ON_GROUND, isSturdyTop(level, pos.below()));
		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level.getBlockState(pos.relative(direction)), isPower));

		return state;
	}

	/**
	 * Get direction for all connections
	 */
	public List<Direction> getConnections(BlockGetter level, BlockPos pos) {
		List<Direction> out = new ArrayList<>();

		for(Direction direction : Direction.values())
			if(canConnectTo(level.getBlockState(pos.relative(direction)), isPower))
				out.add(direction);

		return out;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(level.isClientSide)
			return;
		if(oldState.is(this))
			return;

		if(isPower)
			WireUtils.mergeWireNetworks(level, pos,
					wire -> RadiocraftTags.isPowerWire(wire.getBlock()),
					connection -> RadiocraftTags.isPowerBlock(connection.getBlock()),
					network -> network instanceof PowerNetwork,
					PowerNetwork::new);
		else
			WireUtils.mergeWireNetworks(level, pos,
					wire -> RadiocraftTags.isCoaxWire(wire.getBlock()),
					connection -> RadiocraftTags.isCoaxBlock(connection.getBlock()),
					network -> !(network instanceof PowerNetwork),
					BENetwork::new);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(!newState.is(this) && !level.isClientSide) {
			if(isPower)
				WireUtils.splitWireNetwork(level, pos,
						wire -> RadiocraftTags.isPowerWire(wire.getBlock()),
						connection -> RadiocraftTags.isPowerBlock(connection.getBlock()),
						network -> network instanceof PowerNetwork);
			else
				WireUtils.splitWireNetwork(level, pos,
						wire -> RadiocraftTags.isCoaxWire(wire.getBlock()),
						connection -> RadiocraftTags.isCoaxBlock(connection.getBlock()),
						network -> !(network instanceof PowerNetwork));
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(neighborState, isPower));
	}

	private boolean isSturdyTop(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
	}

	private static boolean canConnectTo(BlockState state, boolean isPower) {
		return isWire(state, isPower) || isNetworkItem(state, isPower);
	}

	public static boolean isNetworkItem(BlockState state, boolean isPower) {
		if(isPower)
			return RadiocraftTags.isPowerBlock(state.getBlock());
		return RadiocraftTags.isCoaxBlock(state.getBlock());
	}

	public static boolean isWire(BlockState state, boolean isPower) {
		if(isPower)
			return RadiocraftTags.isPowerWire(state.getBlock());
		return RadiocraftTags.isCoaxWire(state.getBlock());
	}

}

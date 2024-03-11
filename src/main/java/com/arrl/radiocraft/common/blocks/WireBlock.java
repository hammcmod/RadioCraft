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
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireBlock extends Block implements SimpleWaterloggedBlock {

	public final boolean isPower;

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty ON_GROUND = BooleanProperty.create("on_ground");

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
			Direction.NORTH, NORTH, Direction.EAST, EAST,
			Direction.SOUTH, SOUTH, Direction.WEST, WEST,
			Direction.UP, UP, Direction.DOWN, DOWN));

	private static final double HORIZONTAL_SHAPE_PADDING = 3.0D;
	private static final VoxelShape MIDDLE_SHAPE =
			makeHorizontalPaddedBox(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D);
	private static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, makeHorizontalPaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 7.0D));
		SHAPES.put(Direction.SOUTH, makeHorizontalPaddedBox(7.0D, 0.0D, 9.0D, 9.0D, 2.0D, 16.0D));
		SHAPES.put(Direction.WEST, makeHorizontalPaddedBox(0.0D, 0.0D, 7.0D, 7.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.EAST, makeHorizontalPaddedBox(9.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.UP, makeHorizontalPaddedBox(7.0D, 2.0D, 7.0D, 9.0D, 16.0D, 9.0D));
	}

	public WireBlock(Properties properties, boolean isPower) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false)
				.setValue(ON_GROUND, false));
		this.isPower = isPower;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, ON_GROUND, BlockStateProperties.WATERLOGGED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getBlockState(context.getLevel(), context.getClickedPos());
	}

	/**
	 * Calculate desired blockstate for a wire given its connections and the fluid at its location.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
				.setValue(ON_GROUND, isSturdyTop(level, pos.below()));

		for (Direction direction : Direction.values()) {
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
					canConnectTo(level.getBlockState(pos.relative(direction)), isPower));
		}

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
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ?
				Fluids.WATER.getSource(false) : super.getFluidState(state);
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
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
								  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(neighborState, isPower));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape result = MIDDLE_SHAPE;

		for (Map.Entry<Direction, VoxelShape> entry : SHAPES.entrySet()) {
			BooleanProperty property = PROPERTY_BY_DIRECTION.get(entry.getKey());
			if (state.getValue(property)) {
				result = Shapes.or(result, SHAPES.get(entry.getKey()));
			}
		}

		return result;
	}

	private boolean isSturdyTop(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
	}

	private static boolean canConnectTo(BlockState state, boolean isPower) {
		return isWire(state, isPower) || isNetworkItem(state, isPower);
	}

	private static VoxelShape makeHorizontalPaddedBox(double pX1, double pY1, double pZ1,
													  double pX2, double pY2, double pZ2) {
		return Block.box(
				Math.max(pX1 - HORIZONTAL_SHAPE_PADDING, 0D), pY1,
				Math.max(pZ1 - HORIZONTAL_SHAPE_PADDING, 0D),
				Math.min(pX2 + HORIZONTAL_SHAPE_PADDING, 16D), pY2,
				Math.min(pZ2 + HORIZONTAL_SHAPE_PADDING, 16D)
		);
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

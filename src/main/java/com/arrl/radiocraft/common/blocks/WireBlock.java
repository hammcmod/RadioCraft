package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
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

import java.util.*;

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

	private static final double SHAPE_PADDING = 3.0D;
	private static final VoxelShape MIDDLE_SHAPE =
			makePaddedBox(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D);
	private static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 7.0D));
		SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 9.0D, 9.0D, 2.0D, 16.0D));
		SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 7.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.EAST, makePaddedBox(9.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.UP, makePaddedBox(7.0D, 2.0D, 7.0D, 9.0D, 16.0D, 9.0D));
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
	 * Get a {@link List} of {@link Direction}s this block is connected to.
	 */
	public Set<Direction> getConnections(BlockGetter level, BlockPos pos) {
		Set<Direction> out = new HashSet<>();

		BlockState state = level.getBlockState(pos);
		for(Direction dir : Direction.values())
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				out.add(dir);

		return out;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ?
				Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/**
	 * Checks if multiple {@link Block}s surrounding a given position are compatible connections.
	 *
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} to check around.
	 *
	 * @return True if multiple sides are connected, otherwise false.
	 */
	public boolean checkMultipleSides(Level level, BlockPos pos) {
		int count = 0;
		for(Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			Block block = level.getBlockState(checkPos).getBlock();
			if((block instanceof WireBlock wireBlock && wireBlock.isPower == isPower) || (isPower && IBENetworks.getObject(level, checkPos) instanceof PowerNetworkObject)) {
				if(++count >= 2)
					return true;
			}
		}
		return false;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(level.isClientSide)
			return;
		if(oldState.is(this))
			return;

		if(checkMultipleSides(level, pos)) { // Only bother running merge if this wire is actually connecting things.
			if(isPower)
				WireUtils.mergeNetworks(level, pos, no -> no instanceof PowerNetworkObject, PowerBENetwork::new, RadiocraftBlocks.WIRE.get());
			else
				WireUtils.mergeNetworks(level, pos, no -> no instanceof ICoaxNetworkObject, BENetwork::new, RadiocraftBlocks.COAX_WIRE.get());
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(newState.is(this))
			return;
		if(level.isClientSide)
			return;

		if(checkMultipleSides(level, pos)) { // Only bother running split if this wire is actually connecting things.
			if(isPower)
				WireUtils.splitNetworks(level, pos, no -> no instanceof PowerNetworkObject, RadiocraftBlocks.WIRE.get());
			else
				WireUtils.splitNetworks(level, pos, no -> no instanceof ICoaxNetworkObject, RadiocraftBlocks.COAX_WIRE.get());
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
								  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(neighborState, isPower));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape result = MIDDLE_SHAPE;

		for (Map.Entry<Direction, VoxelShape> entry : SHAPES.entrySet()) {
			BooleanProperty property = PROPERTY_BY_DIRECTION.get(entry.getKey());
			if (state.getValue(property))
				result = Shapes.or(result, SHAPES.get(entry.getKey()));
		}

		return result;
	}

	private boolean isSturdyTop(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
	}

	private static boolean canConnectTo(BlockState state, boolean isPower) {
		if(state.getBlock() instanceof WireBlock wire)
			return wire.isPower == isPower; // If identical wire, OR if valid connection
		return isValidConnection(state, isPower);
	}

	private static VoxelShape makePaddedBox(double x, double y, double z, double x2, double y2, double z2) {
		return Block.box(
				Math.max(x - SHAPE_PADDING, 0D),
				Math.max(y - SHAPE_PADDING, 0D),
				Math.max(z - SHAPE_PADDING, 0D),
				Math.min(x2 + SHAPE_PADDING, 16D),
				Math.min(y2 + SHAPE_PADDING, 16D),
				Math.min(z2 + SHAPE_PADDING, 16D)
		);
	}

	public static boolean isValidConnection(BlockState state, boolean isPower) {
		return isPower ? state.is(RadiocraftTags.Blocks.POWER_BLOCKS) : state.is(RadiocraftTags.Blocks.COAX_BLOCKS);
	}

}

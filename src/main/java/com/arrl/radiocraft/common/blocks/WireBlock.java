package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Power wire, waterproof wire and coax. A wire links to the six blocks around it (other wires of its kind and the
 * devices its kind connects to) and, like redstone dust, can <em>climb</em>: a wire at the foot of a solid block
 * whose top carries a wire or device of its kind runs up that block's face and links diagonally to what is on top
 * ({@link #CLIMB_BY_DIRECTION}). The wire on top shows a stub towards the climber (its horizontal connection flag
 * is set) so the cable reads as one run. {@link #getLinks} is the single source of truth for what a wire is linked
 * to; the network code and the Jade overlay walk those links rather than the six neighbours.
 * <p>
 * Climb flags are only computed when the wire is placed and in {@link #refresh}, which is driven by
 * {@link WireClimbUpdater} whenever a block near the wire changes, because the blocks a climb depends on (the one on
 * top of the step) are not direct neighbours and so never reach {@link #updateShape}. {@link #refresh} also merges
 * or splits the networks when a climb link appears or disappears, since the wire itself is not placed or removed then.
 */
public class WireBlock extends Block implements SimpleWaterloggedBlock {

	public final boolean isPower;
	public final boolean isWaterproof;

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty ON_GROUND = BooleanProperty.create("on_ground");
	public static final BooleanProperty CLIMB_NORTH = BooleanProperty.create("climb_north");
	public static final BooleanProperty CLIMB_EAST = BooleanProperty.create("climb_east");
	public static final BooleanProperty CLIMB_SOUTH = BooleanProperty.create("climb_south");
	public static final BooleanProperty CLIMB_WEST = BooleanProperty.create("climb_west");

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
			Direction.NORTH, NORTH, Direction.EAST, EAST,
			Direction.SOUTH, SOUTH, Direction.WEST, WEST,
			Direction.UP, UP, Direction.DOWN, DOWN));

	/** The wire runs up the face of the solid block in that direction and links to the block on top of it. */
	public static final Map<Direction, BooleanProperty> CLIMB_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
			Direction.NORTH, CLIMB_NORTH, Direction.EAST, CLIMB_EAST,
			Direction.SOUTH, CLIMB_SOUTH, Direction.WEST, CLIMB_WEST));

	private static final double SHAPE_PADDING = 3.0D;
	private static final VoxelShape MIDDLE_SHAPE =
			makePaddedBox(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D);
	private static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();
	private static final HashMap<Direction, VoxelShape> CLIMB_SHAPES = new HashMap<>();

	static {
		SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 7.0D));
		SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 9.0D, 9.0D, 2.0D, 16.0D));
		SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 7.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.EAST, makePaddedBox(9.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		SHAPES.put(Direction.UP, makePaddedBox(7.0D, 2.0D, 7.0D, 9.0D, 16.0D, 9.0D));
		// The vertical run up the wall, on top of the horizontal stub to that wall.
		CLIMB_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 2.0D, 0.0D, 9.0D, 16.0D, 2.0D));
		CLIMB_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 2.0D, 14.0D, 9.0D, 16.0D, 16.0D));
		CLIMB_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 2.0D, 7.0D, 2.0D, 16.0D, 9.0D));
		CLIMB_SHAPES.put(Direction.EAST, makePaddedBox(14.0D, 2.0D, 7.0D, 16.0D, 16.0D, 9.0D));
	}

    @SuppressWarnings("this-escape")
	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false)
				.setValue(CLIMB_NORTH, false).setValue(CLIMB_EAST, false)
				.setValue(CLIMB_SOUTH, false).setValue(CLIMB_WEST, false)
				.setValue(ON_GROUND, false));
		this.isPower = isPower;
        this.isWaterproof = isWaterproof;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, CLIMB_NORTH, CLIMB_EAST, CLIMB_SOUTH, CLIMB_WEST, ON_GROUND, BlockStateProperties.WATERLOGGED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getBlockState(context.getLevel(), context.getClickedPos());
	}

	/**
	 * Calculate desired blockstate for a wire given its connections, climbs and the fluid at its location.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
				.setValue(ON_GROUND, isSturdyTop(level, pos.below()));

		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTowards(level, pos, direction));
		for(Direction direction : Direction.Plane.HORIZONTAL)
			state = state.setValue(CLIMB_BY_DIRECTION.get(direction), canClimb(level, pos, direction));

		return state;
	}

	/**
	 * Whether the stub towards {@code direction} shows: a connectable block there, or, for a horizontal direction, a
	 * wire of this kind at the foot of the block below-diagonal that climbs up to us.
	 */
	private boolean connectsTowards(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos next = pos.relative(direction);
		if(canConnectTo(level.getBlockState(next), isPower))
			return true;
		return direction.getAxis().isHorizontal() && climbsTowards(level, next.below(), direction.getOpposite());
	}

	/** True if the block at {@code climberPos} is a wire of this kind whose climb flag towards {@code towards} is set. */
	public boolean climbsTowards(BlockGetter level, BlockPos climberPos, Direction towards) {
		BlockState state = level.getBlockState(climberPos);
		return state.getBlock() instanceof WireBlock wire && wire.isPower == isPower && state.getValue(CLIMB_BY_DIRECTION.get(towards));
	}

	/**
	 * Whether a wire at {@code pos} climbs the block in {@code direction}: that block is not itself something we link
	 * to, its face towards us is solid, a wire or device of our kind sits on top of it, and no wire of our kind sits
	 * directly above us (that one already carries the run up, so the climb would double it).
	 */
	public boolean canClimb(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos wall = pos.relative(direction);
		BlockState wallState = level.getBlockState(wall);
		if(canConnectTo(wallState, isPower) || !wallState.isFaceSturdy(level, wall, direction.getOpposite()))
			return false;
		if(!canConnectTo(level.getBlockState(wall.above()), isPower))
			return false;
		return !isSameKind(level.getBlockState(pos.above()));
	}

	private boolean isSameKind(BlockState state) {
		return state.getBlock() instanceof WireBlock wire && wire.isPower == isPower;
	}

	/**
	 * Get a {@link Set} of {@link Direction}s this block is connected to (its stubs). Prefer {@link #getLinks}: a
	 * horizontal stub may point at a climber below-diagonal rather than at the neighbour itself.
	 */
	public Set<Direction> getConnections(BlockGetter level, BlockPos pos) {
		Set<Direction> out = new HashSet<>();

		BlockState state = level.getBlockState(pos);
		for(Direction dir : Direction.values())
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				out.add(dir);

		return out;
	}

	/**
	 * Every block this wire is linked to, with the side of that block the wire touches (which is what a device's
	 * network map is keyed by; for a linked wire the side is informational). Covers the six neighbours, the block on
	 * top of a climbed wall, and a climber at the foot of the block below-diagonal.
	 */
	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos, BlockState state) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		for(Direction dir : Direction.values()) {
			if(!state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				continue;
			BlockPos next = pos.relative(dir);
			if(canConnectTo(level.getBlockState(next), isPower))
				out.put(next, dir.getOpposite());
			else if(dir.getAxis().isHorizontal() && climbsTowards(level, next.below(), dir.getOpposite()))
				out.put(next.below(), dir); // The stub reaches the top of a climb coming up the block below-diagonal.
		}
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			if(state.getValue(CLIMB_BY_DIRECTION.get(dir))) {
				BlockPos top = pos.relative(dir).above();
				if(canConnectTo(level.getBlockState(top), isPower))
					out.put(top, dir.getOpposite());
			}
		}
		return out;
	}

	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos) {
		return getLinks(level, pos, level.getBlockState(pos));
	}

	/**
	 * The wires of the given kinds that a device at {@code pos} is linked to: its six neighbours and any climber at
	 * the foot of a block below-diagonal that climbs up to it. The value is the side of the device the wire touches.
	 */
	public static Map<BlockPos, Direction> wireLinksAround(BlockGetter level, BlockPos pos, Set<WireBlock> wires) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		for(Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			if(wires.contains(level.getBlockState(next).getBlock()))
				out.put(next, dir);
			if(dir.getAxis().isHorizontal()) {
				BlockPos foot = next.below();
				BlockState footState = level.getBlockState(foot);
				if(footState.getBlock() instanceof WireBlock wire && wires.contains(wire) && footState.getValue(CLIMB_BY_DIRECTION.get(dir.getOpposite())))
					out.put(foot, dir);
			}
		}
		return out;
	}

	@Override
	public @NotNull FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ?
				Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** True if this wire, in {@code state}, links two or more blocks, i.e. it actually joins something. */
	public boolean checkMultipleSides(Level level, BlockPos pos, BlockState state) {
		return getLinks(level, pos, state).size() >= 2;
	}

	/** What counts as a connection on this wire's networks. */
	public Predicate<BENetworkObject> connectionPredicate() {
		return isPower ? no -> no instanceof PowerNetworkObject : no -> no instanceof ICoaxNetworkObject;
	}

	/** New-network factory for this wire's networks. */
	public Supplier<BENetwork> networkSupplier() {
		return isPower ? PowerBENetwork::new : BENetwork::new;
	}

	/** The wire blocks a run of this kind can pass through. */
	public WireBlock[] networkWires() {
		return isPower ? WireUtils.powerWires() : WireUtils.coaxWires();
	}

	@Override
	public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
		if(level.isClientSide)
			return;
		if(oldState.is(this))
			return;

		if(checkMultipleSides(level, pos, state)) // Only bother running merge if this wire is actually connecting things.
			WireUtils.mergeNetworks(level, pos, connectionPredicate(), networkSupplier(), networkWires());
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
		if(newState.is(this))
			return;
		if(level.isClientSide)
			return;

		Map<BlockPos, Direction> links = getLinks(level, pos, state); // The old state: the block at pos is already gone.
		if(links.size() >= 2) // Only bother running split if this wire was actually connecting things.
			WireUtils.splitNetworks(level, pos, links, true, connectionPredicate(), networkWires());
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/**
	 * Recomputes this wire's state from its surroundings (server side). If a climb link appeared or disappeared the
	 * networks are merged or split to match, since no wire was placed or removed to do that. Called by
	 * {@link WireClimbUpdater} for every wire near a changed block; a no-op when nothing changed.
	 */
	public void refresh(Level level, BlockPos pos) {
		if(level.isClientSide)
			return;
		BlockState old = level.getBlockState(pos);
		if(!old.is(this))
			return;
		BlockState fresh = getBlockState(level, pos);
		if(fresh == old)
			return;

		boolean climbChanged = false;
		for(BooleanProperty climb : CLIMB_BY_DIRECTION.values())
			climbChanged |= old.getValue(climb) != fresh.getValue(climb);
		Map<BlockPos, Direction> oldLinks = climbChanged ? getLinks(level, pos, old) : Map.of();

		level.setBlock(pos, fresh, Block.UPDATE_ALL);
		if(!climbChanged)
			return;

		Map<BlockPos, Direction> newLinks = getLinks(level, pos, fresh);
		if(!oldLinks.keySet().containsAll(newLinks.keySet()))
			WireUtils.mergeNetworks(level, pos, connectionPredicate(), networkSupplier(), networkWires());
		if(!newLinks.keySet().containsAll(oldLinks.keySet())) {
			Map<BlockPos, Direction> all = new LinkedHashMap<>(oldLinks);
			all.putAll(newLinks);
			WireUtils.splitNetworks(level, pos, all, false, connectionPredicate(), networkWires());
		}
	}

	@Override
	public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
										   @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		boolean connects = canConnectTo(neighborState, isPower)
				|| (direction.getAxis().isHorizontal() && climbsTowards(level, neighborPos.below(), direction.getOpposite()));
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects); // Climb flags are handled by refresh().
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		VoxelShape result = MIDDLE_SHAPE;

		for (Map.Entry<Direction, VoxelShape> entry : SHAPES.entrySet()) {
			BooleanProperty property = PROPERTY_BY_DIRECTION.get(entry.getKey());
			if (state.getValue(property))
				result = Shapes.or(result, entry.getValue());
		}
		for (Map.Entry<Direction, VoxelShape> entry : CLIMB_SHAPES.entrySet()) {
			if (state.getValue(CLIMB_BY_DIRECTION.get(entry.getKey())))
				result = Shapes.or(result, SHAPES.get(entry.getKey()), entry.getValue()); // Stub to the wall, then up it.
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
		return isPower ? state.is(RadiocraftTags.Blocks.POWER_BLOCKS) : state.is(RadiocraftTags.Blocks.COAX_BLOCKS) || state.getBlock() instanceof RadioBlock;
	}

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (isPower && !isWaterproof) {
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented_crafting_only"));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
        }
    }
}

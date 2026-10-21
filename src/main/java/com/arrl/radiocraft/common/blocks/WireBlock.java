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
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
 * devices its kind connects to). Vertical runs hug a wall: when a wire goes up and a solid block stands beside it,
 * the vertical piece runs up that block's face ({@link #HUG}) instead of floating in the middle of the block, the
 * wire above arrives on the same face ({@link #DOWN_HUG}) and only then turns towards its own connections. Like
 * redstone dust, a wire at the foot of a wall with nothing above it but a wire or device on top of the wall
 * <em>climbs</em>: the hugging run continues to the block on top and links to it diagonally ({@link #CLIMB}); the
 * wire on top shows a stub towards the climber. {@link #getLinks} is the single source of truth for what a wire is
 * linked to; the network code and the Jade overlay walk those links rather than the six neighbours.
 * <p>
 * The climb flag depends on a block that is not a direct neighbour (the one on top of the wall), so it is only
 * computed on placement and in {@link #refresh}, which {@link WireClimbUpdater} runs for every wire near a changed
 * block. {@link #refresh} also merges or splits the networks when a climb link appears or disappears, since no wire
 * is placed or removed then. Everything else (stubs, which wall is hugged) follows direct neighbours and is kept up
 * to date by {@link #updateShape} as well.
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
	/** The wall this wire's run upwards follows (none: up the middle of the block, or no run upwards). */
	public static final EnumProperty<WallSide> HUG = EnumProperty.create("hug", WallSide.class);
	/** The wall the wire below follows, i.e. where a run from below arrives in this block (none: the middle). */
	public static final EnumProperty<WallSide> DOWN_HUG = EnumProperty.create("down_hug", WallSide.class);
	/**
	 * The run up the {@link #HUG} wall continues to what is on top of that wall (a diagonal link). A wire there draws
	 * its own stub towards us; a device does not, so the climb carries on over the wall's top edge to its centre.
	 */
	public static final EnumProperty<ClimbTarget> CLIMB = EnumProperty.create("climb", ClimbTarget.class);

	/** What a climb ends at. */
	public enum ClimbTarget implements StringRepresentable {
		NONE, WIRE, DEVICE;

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
			Direction.NORTH, NORTH, Direction.EAST, EAST,
			Direction.SOUTH, SOUTH, Direction.WEST, WEST,
			Direction.UP, UP, Direction.DOWN, DOWN));

	/** A horizontal side of the block, or none. */
	public enum WallSide implements StringRepresentable {
		NONE(null), NORTH(Direction.NORTH), EAST(Direction.EAST), SOUTH(Direction.SOUTH), WEST(Direction.WEST);

		@Nullable
		private final Direction direction;

		WallSide(@Nullable Direction direction) {
			this.direction = direction;
		}

		@Nullable
		public Direction direction() {
			return direction;
		}

		public static WallSide of(@Nullable Direction direction) {
			if(direction == null)
				return NONE;
			return switch(direction) {
				case NORTH -> NORTH;
				case EAST -> EAST;
				case SOUTH -> SOUTH;
				case WEST -> WEST;
				default -> NONE;
			};
		}

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final double SHAPE_PADDING = 3.0D;
	private static final VoxelShape MIDDLE_SHAPE =
			makePaddedBox(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D);
	private static final VoxelShape CENTRE_VERTICAL_SHAPE = makePaddedBox(7.0D, 2.0D, 7.0D, 9.0D, 16.0D, 9.0D);
	private static final HashMap<Direction, VoxelShape> STUB_SHAPES = new HashMap<>();
	private static final HashMap<Direction, VoxelShape> WALL_SHAPES = new HashMap<>();

	static {
		STUB_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 7.0D));
		STUB_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 9.0D, 9.0D, 2.0D, 16.0D));
		STUB_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 7.0D, 2.0D, 9.0D));
		STUB_SHAPES.put(Direction.EAST, makePaddedBox(9.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		// The vertical run up a wall, floor to ceiling.
		WALL_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 2.0D));
		WALL_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 14.0D, 9.0D, 16.0D, 16.0D));
		WALL_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 2.0D, 16.0D, 9.0D));
		WALL_SHAPES.put(Direction.EAST, makePaddedBox(14.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D));
	}

    @SuppressWarnings("this-escape")
	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false)
				.setValue(HUG, WallSide.NONE).setValue(DOWN_HUG, WallSide.NONE)
				.setValue(CLIMB, ClimbTarget.NONE));
		this.isPower = isPower;
        this.isWaterproof = isWaterproof;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, HUG, DOWN_HUG, CLIMB, BlockStateProperties.WATERLOGGED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getBlockState(context.getLevel(), context.getClickedPos());
	}

	/**
	 * Calculate desired blockstate for a wire given its connections, the walls around it and the fluid at its location.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		BlockState state = defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);

		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTowards(level, pos, direction));
		state = state.setValue(DOWN_HUG, downHug(level, pos, state));

		// A climb if there is one, trying the wall the run from below is already on first so the cable stays on one wall.
		for(Direction dir : wallOrder(state.getValue(DOWN_HUG))) {
			if(canClimb(level, pos, dir)) {
				ClimbTarget target = isSameKind(level.getBlockState(pos.relative(dir).above())) ? ClimbTarget.WIRE : ClimbTarget.DEVICE;
				return state.setValue(HUG, WallSide.of(dir)).setValue(CLIMB, target);
			}
		}
		return state.setValue(HUG, WallSide.of(hugWall(level, pos, state))).setValue(CLIMB, ClimbTarget.NONE);
	}

	/** The four walls, with {@code first} (if any) ahead of the others. */
	private static List<Direction> wallOrder(WallSide first) {
		List<Direction> order = new ArrayList<>(4);
		if(first.direction() != null)
			order.add(first.direction());
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(!order.contains(dir))
				order.add(dir);
		return order;
	}

	/** Which wall the wire below us follows, so a run from below arrives on the same face. */
	private WallSide downHug(BlockGetter level, BlockPos pos, BlockState state) {
		if(!state.getValue(DOWN))
			return WallSide.NONE;
		BlockState below = level.getBlockState(pos.below());
		return isSameKind(below) ? below.getValue(HUG) : WallSide.NONE;
	}

	/** The wall our run upwards (if any, and not a climb) follows: the one the run from below is on if solid, else the first solid one. */
	@Nullable
	private Direction hugWall(BlockGetter level, BlockPos pos, BlockState state) {
		if(!state.getValue(UP))
			return null;
		for(Direction dir : wallOrder(state.getValue(DOWN_HUG)))
			if(isWall(level, pos.relative(dir), dir.getOpposite()))
				return dir;
		return null;
	}

	/** A solid face we can run a cable up: sturdy towards us and not something the wire links to instead. */
	private boolean isWall(BlockGetter level, BlockPos wallPos, Direction faceTowardsUs) {
		BlockState wall = level.getBlockState(wallPos);
		return !canConnectTo(wall, isPower) && wall.isFaceSturdy(level, wallPos, faceTowardsUs);
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

	/** True if the block at {@code climberPos} is a wire of this kind climbing the wall in direction {@code towards}. */
	public boolean climbsTowards(BlockGetter level, BlockPos climberPos, Direction towards) {
		BlockState state = level.getBlockState(climberPos);
		return isSameKind(state) && state.getValue(CLIMB) != ClimbTarget.NONE && state.getValue(HUG).direction() == towards;
	}

	/**
	 * Whether a wire at {@code pos} climbs the block in {@code direction}: that block is a wall, a wire or device of
	 * our kind sits on top of it, and no wire of our kind sits directly above us (that one carries the run up
	 * instead, hugging the same wall).
	 */
	public boolean canClimb(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos wall = pos.relative(direction);
		if(!isWall(level, wall, direction.getOpposite()))
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
		Direction climb = state.getValue(CLIMB) != ClimbTarget.NONE ? state.getValue(HUG).direction() : null;
		if(climb != null) {
			BlockPos top = pos.relative(climb).above();
			if(canConnectTo(level.getBlockState(top), isPower))
				out.put(top, climb.getOpposite());
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
				if(footState.getBlock() instanceof WireBlock wire && wires.contains(wire)
						&& footState.getValue(CLIMB) != ClimbTarget.NONE && footState.getValue(HUG).direction() == dir.getOpposite())
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

	/** The block a climb in {@code state} links to, or null when not climbing. */
	@Nullable
	private static BlockPos climbTarget(BlockPos pos, BlockState state) {
		Direction dir = state.getValue(CLIMB) != ClimbTarget.NONE ? state.getValue(HUG).direction() : null;
		return dir == null ? null : pos.relative(dir).above();
	}

	/**
	 * Recomputes this wire's state from its surroundings (server side). If the climb link appeared, disappeared or
	 * moved, the networks are merged or split to match, since no wire was placed or removed to do that. Called by
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

		boolean climbChanged = !Objects.equals(climbTarget(pos, old), climbTarget(pos, fresh));
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
		// Everything that follows direct neighbours is recomputed; the climb (which depends on a diagonal block and
		// carries network bookkeeping) is left to refresh().
		BlockState fresh = getBlockState(level, pos).setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
		if(state.getValue(CLIMB) != ClimbTarget.NONE)
			return fresh.setValue(CLIMB, state.getValue(CLIMB)).setValue(HUG, state.getValue(HUG));
		if(fresh.getValue(CLIMB) != ClimbTarget.NONE)
			fresh = fresh.setValue(CLIMB, ClimbTarget.NONE).setValue(HUG, WallSide.of(hugWall(level, pos, fresh)));
		return fresh;
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		VoxelShape result = MIDDLE_SHAPE;

		for (Map.Entry<Direction, VoxelShape> entry : STUB_SHAPES.entrySet()) {
			if (state.getValue(PROPERTY_BY_DIRECTION.get(entry.getKey())))
				result = Shapes.or(result, entry.getValue());
		}
		Direction hug = state.getValue(HUG).direction();
		if (hug != null) // Stub to the wall, then up it.
			result = Shapes.or(result, STUB_SHAPES.get(hug), WALL_SHAPES.get(hug));
		else if (state.getValue(UP))
			result = Shapes.or(result, CENTRE_VERTICAL_SHAPE);
		Direction downHug = state.getValue(DOWN_HUG).direction();
		if (downHug != null) // The run from below arrives at that edge.
			result = Shapes.or(result, STUB_SHAPES.get(downHug));

		return result;
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

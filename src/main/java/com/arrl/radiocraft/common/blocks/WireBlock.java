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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Power wire, waterproof wire and coax. A wire links to the six blocks around it (other wires of its kind and the
 * devices its kind connects to). Horizontal runs lie on the floor of the block through its middle. Vertical runs
 * follow an <em>edge</em> of the block: a run going up picks a solid wall to hug ({@link #HUG}), and the wire above
 * keeps the run on that same edge ({@link #ARRIVE} says where a run from below comes in) whether or not there is
 * still a wall there, so a cable going up a post keeps its line above the post. Only where the run must reach
 * something in the middle of a block (a device above it, or a horizontal branch) does it turn in from the edge.
 * Like redstone dust, a wire at the foot of a wall with nothing above it but a wire or device on top of the wall
 * <em>climbs</em>: the run up the wall continues to the block on top and links to it diagonally ({@link #END}).
 * {@link #getLinks} is the single source of truth for what a wire is linked to; the network code and the Jade
 * overlay walk those links rather than the six neighbours.
 * <p>
 * The climb and the arrival from a climber depend on blocks that are not direct neighbours, so they are only
 * computed on placement and in {@link #refresh}, which {@link WireClimbUpdater} runs for every wire near a changed
 * block. {@link #refresh} also merges or splits the networks when the climb link appears, disappears or moves,
 * since no wire is placed or removed then. Everything that follows direct neighbours is kept up to date by
 * {@link #updateShape} as well.
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
	/** The edge this wire's run upwards follows (none: up the middle of the block, or no run upwards). */
	public static final EnumProperty<Edge> HUG = EnumProperty.create("hug", Edge.class);
	/**
	 * The edge where a run from below comes into this block: the edge the wire below follows, or the side a climber
	 * at the foot of the block below-diagonal comes up on. None: from the middle (a wire below running up its
	 * middle), or no run from below.
	 */
	public static final EnumProperty<Edge> ARRIVE = EnumProperty.create("arrive", Edge.class);
	/** How the run upwards ends, when it ends at something that draws nothing towards us. */
	public static final EnumProperty<End> END = EnumProperty.create("end", End.class);

	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
			Direction.NORTH, NORTH, Direction.EAST, EAST,
			Direction.SOUTH, SOUTH, Direction.WEST, WEST,
			Direction.UP, UP, Direction.DOWN, DOWN));

	/** A horizontal side of the block, or none. */
	public enum Edge implements StringRepresentable {
		NONE(null), NORTH(Direction.NORTH), EAST(Direction.EAST), SOUTH(Direction.SOUTH), WEST(Direction.WEST);

		@Nullable
		private final Direction direction;

		Edge(@Nullable Direction direction) {
			this.direction = direction;
		}

		@Nullable
		public Direction direction() {
			return direction;
		}

		public static Edge of(@Nullable Direction direction) {
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

	/** What the run up the {@link #HUG} edge ends at. */
	public enum End implements StringRepresentable {
		/** Nothing special: a wire above continues it, or there is no run upwards. */
		NONE,
		/** A climb up the hugged wall to the wire on top of it; that wire draws the arrival. */
		CLIMB_WIRE,
		/** A climb up the hugged wall to a device on top of it; we draw the cable over the wall's top to its middle. */
		CLIMB_DEVICE,
		/** The run up the hugged edge ends at a device directly above; we draw the cable in to its middle. */
		UP_DEVICE;

		public boolean isClimb() {
			return this == CLIMB_WIRE || this == CLIMB_DEVICE;
		}

		@Override
		public @NotNull String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final double SHAPE_PADDING = 1.0D; // A 2 px cable with 1 px either side: enough to click, tight enough to read as the cable.
	private static final VoxelShape MIDDLE_SHAPE =
			makePaddedBox(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D);
	private static final VoxelShape CENTRE_VERTICAL_SHAPE = makePaddedBox(7.0D, 2.0D, 7.0D, 9.0D, 16.0D, 9.0D);
	private static final HashMap<Direction, VoxelShape> STUB_SHAPES = new HashMap<>();
	private static final HashMap<Direction, VoxelShape> EDGE_SHAPES = new HashMap<>();
	private static final HashMap<Direction, VoxelShape> CORNER_SHAPES = new HashMap<>();

	static {
		CORNER_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 1.0D));
		CORNER_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 15.0D, 9.0D, 2.0D, 16.0D));
		CORNER_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 1.0D, 2.0D, 9.0D));
		CORNER_SHAPES.put(Direction.EAST, makePaddedBox(15.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		STUB_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 2.0D, 7.0D));
		STUB_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 9.0D, 9.0D, 2.0D, 16.0D));
		STUB_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 7.0D, 2.0D, 9.0D));
		STUB_SHAPES.put(Direction.EAST, makePaddedBox(9.0D, 0.0D, 7.0D, 16.0D, 2.0D, 9.0D));
		// The vertical run along an edge, floor to ceiling.
		EDGE_SHAPES.put(Direction.NORTH, makePaddedBox(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 2.0D));
		EDGE_SHAPES.put(Direction.SOUTH, makePaddedBox(7.0D, 0.0D, 14.0D, 9.0D, 16.0D, 16.0D));
		EDGE_SHAPES.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, 7.0D, 2.0D, 16.0D, 9.0D));
		EDGE_SHAPES.put(Direction.EAST, makePaddedBox(14.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D));
	}

    @SuppressWarnings("this-escape")
	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false)
				.setValue(HUG, Edge.NONE).setValue(ARRIVE, Edge.NONE)
				.setValue(END, End.NONE));
		this.isPower = isPower;
        this.isWaterproof = isWaterproof;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, HUG, ARRIVE, END, BlockStateProperties.WATERLOGGED);
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
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level.getBlockState(pos.relative(direction)), isPower));
		state = state.setValue(ARRIVE, Edge.of(arrival(level, pos, state)));

		// A climb if there is one, trying the edge the run from below is already on first so the cable keeps its line.
		for(Direction dir : wallOrder(state.getValue(ARRIVE))) {
			if(canClimb(level, pos, dir)) {
				End end = isSameKind(level.getBlockState(pos.relative(dir).above())) ? End.CLIMB_WIRE : End.CLIMB_DEVICE;
				return state.setValue(HUG, Edge.of(dir)).setValue(END, end);
			}
		}
		Direction hug = hugEdge(level, pos, state);
		End end = End.NONE;
		if(hug != null) {
			BlockState above = level.getBlockState(pos.above());
			if(!isSameKind(above) && canConnectTo(above, isPower))
				end = End.UP_DEVICE; // The run up the edge ends at a device above: turn in to its middle.
		}
		return state.setValue(HUG, Edge.of(hug)).setValue(END, end);
	}

	/** The four sides, with {@code first} (if any) ahead of the others. */
	private static List<Direction> wallOrder(Edge first) {
		List<Direction> order = new ArrayList<>(4);
		if(first.direction() != null)
			order.add(first.direction());
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(!order.contains(dir))
				order.add(dir);
		return order;
	}

	/**
	 * Where a run from below comes into this block: the edge the wire below follows, or the side a climber at the
	 * foot of the block below-diagonal comes up on. Null for the middle or no run from below.
	 */
	@Nullable
	private Direction arrival(BlockGetter level, BlockPos pos, BlockState state) {
		BlockState below = level.getBlockState(pos.below());
		if(state.getValue(DOWN) && isSameKind(below))
			return below.getValue(HUG).direction();
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(climbsTowards(level, pos.relative(dir).below(), dir.getOpposite()))
				return dir;
		return null;
	}

	/**
	 * The edge our run upwards (if any, and not a climb) follows: the edge the run from below is on, so the cable
	 * keeps its line; else the first solid wall; else null for up the middle.
	 */
	@Nullable
	private Direction hugEdge(BlockGetter level, BlockPos pos, BlockState state) {
		if(!state.getValue(UP))
			return null;
		Direction arrive = state.getValue(ARRIVE).direction();
		if(arrive != null)
			return arrive;
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(isWall(level, pos.relative(dir), dir.getOpposite()))
				return dir;
		return null;
	}

	/** A solid face we can run a cable up: sturdy towards us and not something the wire links to instead. */
	private boolean isWall(BlockGetter level, BlockPos wallPos, Direction faceTowardsUs) {
		BlockState wall = level.getBlockState(wallPos);
		return !canConnectTo(wall, isPower) && wall.isFaceSturdy(level, wallPos, faceTowardsUs);
	}

	/** True if the block at {@code climberPos} is a wire of this kind climbing the wall in direction {@code towards}. */
	public boolean climbsTowards(BlockGetter level, BlockPos climberPos, Direction towards) {
		BlockState state = level.getBlockState(climberPos);
		return isSameKind(state) && state.getValue(END).isClimb() && state.getValue(HUG).direction() == towards;
	}

	/**
	 * Whether a wire at {@code pos} climbs the block in {@code direction}: that block is a wall, a wire or device of
	 * our kind sits on top of it, and no wire of our kind sits directly above us (that one carries the run up
	 * instead, on the same edge).
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
	 * Get a {@link Set} of {@link Direction}s this block is connected to through its six neighbours. Prefer
	 * {@link #getLinks}, which includes the diagonal climb links.
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
	 * top of a climbed wall, and a climber at the foot of the block below-diagonal that comes up to us.
	 */
	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos, BlockState state) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		for(Direction dir : Direction.values()) {
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				out.put(pos.relative(dir), dir.getOpposite());
		}
		Direction arrive = state.getValue(ARRIVE).direction();
		if(arrive != null && !state.getValue(DOWN)) { // Not from the wire below, so from a climber on that side.
			BlockPos climber = pos.relative(arrive).below();
			if(climbsTowards(level, climber, arrive.getOpposite()))
				out.put(climber, arrive);
		}
		if(state.getValue(END).isClimb()) {
			BlockPos top = pos.relative(state.getValue(HUG).direction()).above();
			if(canConnectTo(level.getBlockState(top), isPower))
				out.put(top, state.getValue(HUG).direction().getOpposite());
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
						&& footState.getValue(END).isClimb() && footState.getValue(HUG).direction() == dir.getOpposite())
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
		return state.getValue(END).isClimb() ? pos.relative(state.getValue(HUG).direction()).above() : null;
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
		if(state.getValue(END).isClimb())
			return fresh.setValue(END, state.getValue(END)).setValue(HUG, state.getValue(HUG));
		if(fresh.getValue(END).isClimb())
			fresh = fresh.setValue(END, End.NONE).setValue(HUG, Edge.of(hugEdge(level, pos, fresh)));
		return fresh;
	}

	private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE_CACHE.computeIfAbsent(state, WireBlock::buildShape);
	}

	/**
	 * The outline follows the cable: the same pieces the blockstate draws, by the same rule (tools/wire_assets.py
	 * generates the blockstate from it), except the overhangs, which lie outside the block.
	 * <p>
	 * Cable already lies on the edges in S = {arrive, hug}. The "ports" it must reach are the connected neighbours
	 * (at their edges), the top of the block when the run up goes through the middle, and the bottom when the run
	 * from below comes up the middle. The middle piece is drawn only when the cable has to pass through the middle:
	 * a port not on an edge in S, two different edges in S, no edge cable at all, or a dead end (a nub so the block
	 * is visible). A stub joins the middle to every edge that has cable or a neighbour. A run arriving at an edge
	 * and leaving sideways on that same edge, with nothing else to reach, is just a corner cube on that edge.
	 */
	private static VoxelShape buildShape(BlockState state) {
		Direction hug = state.getValue(HUG).direction();
		Direction arrive = state.getValue(ARRIVE).direction();
		Set<Direction> edgeCable = new HashSet<>();
		if(hug != null)
			edgeCable.add(hug);
		if(arrive != null)
			edgeCable.add(arrive);

		boolean portOffEdge = (state.getValue(UP) && hug == null) || (state.getValue(DOWN) && arrive == null);
		boolean anyPort = portOffEdge;
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
				anyPort = true;
				portOffEdge |= !edgeCable.contains(dir);
			}
		}
		// The cable passes straight through along an edge when it arrives there and goes on up (or climbs).
		boolean through = arrive != null && (state.getValue(UP) || state.getValue(END).isClimb());
		boolean middle = portOffEdge || edgeCable.size() != 1 || (!anyPort && !through);

		VoxelShape result = middle ? MIDDLE_SHAPE : Shapes.empty();
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			boolean connected = state.getValue(PROPERTY_BY_DIRECTION.get(dir));
			if(middle && (connected || edgeCable.contains(dir)))
				result = Shapes.or(result, STUB_SHAPES.get(dir));
			else if(connected && arrive == dir && hug == null)
				result = Shapes.or(result, CORNER_SHAPES.get(dir)); // The corner case: only reachable with no middle.
		}
		if(hug != null)
			result = Shapes.or(result, EDGE_SHAPES.get(hug));
		else if(state.getValue(UP))
			result = Shapes.or(result, CENTRE_VERTICAL_SHAPE);
		return result.isEmpty() ? MIDDLE_SHAPE : result;
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
        tooltipComponents.add(Component.translatable("tooltip.radiocraft." + (isPower ? (isWaterproof ? "waterproof_wire" : "wire") : "coax_wire")));
    }
}

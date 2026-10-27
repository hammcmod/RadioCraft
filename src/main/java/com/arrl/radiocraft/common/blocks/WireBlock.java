package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireFaults;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
 * keeps the run on that same edge (see {@link #look}) whether or not there is
 * still a wall there, so a cable going up a post keeps its line above the post. Only where the run must reach
 * something in the middle of a block (a device above it, or a horizontal branch) does it turn in from the edge.
 * Like redstone dust, a wire at the foot of a wall with nothing above it but a wire or device on top of the wall
 * <em>climbs</em>: the run up the wall continues to the block on top and links to it diagonally ({@link #CLIMB}).
 * A wire placed against the underside of a block hangs from it ({@link #CEILING}): everything above is mirrored, so
 * its runs lie along the ceiling, its runs downwards hug walls, and it climbs <em>down</em> the face of a beam to the
 * wire or device underneath. "Up" in the rules below is away from the surface the wire lies on ({@link #up}).
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
	/** Dyed coax's colour: it only joins coax of the same colour, or plain coax (null). */
	@Nullable
	public final net.minecraft.world.item.DyeColor colour;

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	/** The edge this wire's run upwards follows (none: up the middle of the block, or no run upwards). */
	public static final EnumProperty<Edge> HUG = EnumProperty.create("hug", Edge.class);
	/**
	 * Whether the run up the {@link #HUG} wall is a climb to the block on top of that wall (a diagonal network link),
	 * and in a one-wide trench also up the opposite wall. Stored because the network has to notice when it changes;
	 * everything else about the look is worked out from the neighbours ({@link #look}).
	 */
	public static final EnumProperty<Climb> CLIMB = EnumProperty.create("climb", Climb.class);
	/** True for a wire hanging from the underside of a block: its whole look and climb logic is upside down. */
	public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");

	/** True if {@code state} is a wire hanging from a ceiling (blocks without the property, like the passthrough, are not). */
	public static boolean isCeiling(BlockState state) {
		return state.hasProperty(CEILING) && state.getValue(CEILING);
	}

	/** "Up" for a wire: away from the floor it lies on, or down for one on a ceiling. */
	public static Direction up(boolean ceiling) {
		return ceiling ? Direction.DOWN : Direction.UP;
	}

	private static BlockPos above(BlockPos pos, boolean ceiling) {
		return pos.relative(up(ceiling));
	}

	private static BlockPos below(BlockPos pos, boolean ceiling) {
		return pos.relative(up(ceiling).getOpposite());
	}

	/** The connection property that points "up" (see {@link #up}) and the one that points "down". */
	private static BooleanProperty upLink(boolean ceiling) {
		return PROPERTY_BY_DIRECTION.get(up(ceiling));
	}

	private static BooleanProperty downLink(boolean ceiling) {
		return PROPERTY_BY_DIRECTION.get(up(ceiling).getOpposite());
	}

	/** See {@link #CLIMB}. */
	public enum Climb implements StringRepresentable {
		NONE, ONE, BOTH;

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

	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof) {
		this(properties, isPower, isWaterproof, null);
	}

	/** {@code colour} for dyed coax (null for plain): see {@link #joins}. */
    @SuppressWarnings("this-escape")
	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof, @Nullable net.minecraft.world.item.DyeColor colour) {
		super(properties);
		this.colour = colour;
		// trySetValue: a subclass may define fewer properties (the passthrough only has the six connections).
		registerDefaultState(defaultBlockState()
				.trySetValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(NORTH, false).setValue(EAST, false)
				.setValue(SOUTH, false).setValue(WEST, false)
				.setValue(UP, false).setValue(DOWN, false)
				.trySetValue(HUG, Edge.NONE).trySetValue(CLIMB, Climb.NONE).trySetValue(CEILING, false));
		this.isPower = isPower;
        this.isWaterproof = isWaterproof;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, HUG, CLIMB, CEILING, BlockStateProperties.WATERLOGGED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if(!defaultBlockState().hasProperty(CEILING))
			return getBlockState(context.getLevel(), context.getClickedPos());
		return getBlockState(context.getLevel(), context.getClickedPos(), placesOnCeiling(context));
	}

	/**
	 * A wire hangs from the ceiling when placed against the underside of a block, when it carries on sideways from a
	 * wire that hangs, or when there is a ceiling above it but no floor under it.
	 */
	private static boolean placesOnCeiling(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if(face == Direction.DOWN)
			return true;
		if(face == Direction.UP)
			return false;
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockPos against = pos.relative(face.getOpposite());
		if(level.getBlockState(against).getBlock() instanceof WireBlock)
			return isCeiling(level.getBlockState(against));
		BlockPos floor = pos.below(), ceiling = pos.above();
		return !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) && level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
	}

	/**
	 * Calculate desired blockstate for a wire given its connections, the walls around it and the fluid at its location,
	 * keeping whether the wire already there hangs from the ceiling.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		return getBlockState(level, pos, isCeiling(level.getBlockState(pos)));
	}

	/** As {@link #getBlockState(BlockGetter, BlockPos)}, for a wire on the floor or hanging from the ceiling. */
	public BlockState getBlockState(BlockGetter level, BlockPos pos, boolean ceiling) {
		BlockState state = defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
				.trySetValue(CEILING, ceiling);

		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level.getBlockState(pos.relative(direction))));
		Direction arrive = arrival(level, pos, state);

		// A climb if there is one, trying the edge the run from below is already on first so the cable keeps its line.
		for(Direction dir : wallOrder(Edge.of(arrive))) {
			if(canClimb(level, pos, dir, ceiling)) {
				Direction back = dir.getOpposite();
				boolean climbBack = canClimb(level, pos, back, ceiling) && isSameKind(level.getBlockState(above(pos.relative(back), ceiling)));
				return state.setValue(HUG, Edge.of(dir)).setValue(CLIMB, climbBack ? Climb.BOTH : Climb.ONE);
			}
		}
		return state.setValue(HUG, Edge.of(hugEdge(level, pos, state, arrive))).setValue(CLIMB, Climb.NONE);
	}

	/**
	 * The look of the wire at {@code pos} in {@code state}: the stored climb and hugged edge, plus the edge a run
	 * from below arrives on and how the run up ends, both worked out from the neighbours. Called by the client model
	 * while a chunk is drawn (with a render region as {@code level}) and by the hitbox.
	 */
	public WireLook.Look look(BlockGetter level, BlockPos pos, BlockState state) {
		if(!state.hasProperty(HUG))
			return WireLook.Look.NONE;
		Direction hug = state.getValue(HUG).direction();
		Climb climb = state.getValue(CLIMB);
		boolean ceiling = isCeiling(state);
		WireLook.End end = WireLook.End.NONE;
		if(hug != null && climb != Climb.NONE)
			end = isSameKind(level.getBlockState(above(pos.relative(hug), ceiling))) ? WireLook.End.CLIMB_WIRE : WireLook.End.CLIMB_DEVICE;
		else if(hug != null) {
			BlockState above = level.getBlockState(above(pos, ceiling));
			if(!isSameKind(above) && canConnectTo(above))
				end = WireLook.End.UP_DEVICE; // The run up the edge ends at a device above: turn in to its middle.
		}
		return new WireLook.Look(hug, arrival(level, pos, state), end, climb == Climb.BOTH);
	}

	/**
	 * The six connections of {@code state} as {@link WireLook} sees them: for a wire on a ceiling, up and down swapped,
	 * since its look is drawn upside down.
	 */
	public static Set<Direction> lookConnections(BlockState state) {
		Set<Direction> out = connected(state);
		if(isCeiling(state)) {
			boolean up = out.remove(Direction.UP), down = out.remove(Direction.DOWN);
			if(up)
				out.add(Direction.DOWN);
			if(down)
				out.add(Direction.UP);
		}
		return out;
	}

	/** The six connections of {@code state} as a set. */
	public static Set<Direction> connected(BlockState state) {
		Set<Direction> out = java.util.EnumSet.noneOf(Direction.class);
		for(Direction dir : Direction.values())
			if(state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				out.add(dir);
		return out;
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
		boolean ceiling = isCeiling(state);
		BlockState below = level.getBlockState(below(pos, ceiling));
		if(state.getValue(downLink(ceiling)) && isSameKind(below)) {
			if(isCeiling(below) == ceiling)
				return below.getValue(HUG).direction();
			// A floor wire and a ceiling wire joined end to end (a ceiling run turning up a wall, say): one run between
			// them, on the edge either of them hugs, so each turns its corner on that edge with no stub to the middle.
			Direction shared = below.getValue(HUG).direction();
			return shared != null ? shared : state.getValue(HUG).direction();
		}
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(climbsTowards(level, below(pos.relative(dir), ceiling), dir.getOpposite(), ceiling))
				return dir;
		return null;
	}

	/**
	 * The edge our run upwards (if any, and not a climb) follows: the edge the run from below is on, so the cable
	 * keeps its line; else the first solid wall; else null for up the middle.
	 */
	@Nullable
	private Direction hugEdge(BlockGetter level, BlockPos pos, BlockState state, @Nullable Direction arrive) {
		boolean ceiling = isCeiling(state);
		if(!state.getValue(upLink(ceiling)))
			return null;
		if(arrive != null)
			return arrive;
		// A floor wire and a ceiling wire running towards each other share the run: follow the edge the other is on.
		BlockState facing = level.getBlockState(above(pos, ceiling));
		if(isSameKind(facing) && isCeiling(facing) != ceiling && facing.getValue(HUG).direction() != null)
			return facing.getValue(HUG).direction();
		// Of the solid walls around, prefer the one on the side the run leads to at the top, so the cable goes
		// straight up to the wire it joins instead of up the nearest wall and across.
		for(Direction dir : wallOrder(Edge.of(destinationSide(level, pos, ceiling))))
			if(isWall(level, pos.relative(dir), dir.getOpposite()))
				return dir;
		return null;
	}

	/**
	 * The side the wire directly above leaves towards (its first horizontal connection), null if none. Only one block
	 * up: the client only redraws blocks next to a change, so a look must not depend on anything further away.
	 */
	@Nullable
	private Direction destinationSide(BlockGetter level, BlockPos pos, boolean ceiling) {
		BlockState above = level.getBlockState(above(pos, ceiling));
		if(!isSameKind(above))
			return null;
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(above.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				return dir;
		return null;
	}

	/** A solid face we can run a cable up: sturdy towards us and not something the wire links to instead. */
	private boolean isWall(BlockGetter level, BlockPos wallPos, Direction faceTowardsUs) {
		BlockState wall = level.getBlockState(wallPos);
		return !canConnectTo(wall) && wall.isFaceSturdy(level, wallPos, faceTowardsUs);
	}

	/**
	 * True if the block at {@code climberPos} is a wire of this kind climbing the wall in direction {@code towards},
	 * the same way up as {@code ceiling} (a floor wire climbs up, a ceiling wire down).
	 */
	public boolean climbsTowards(BlockGetter level, BlockPos climberPos, Direction towards, boolean ceiling) {
		BlockState state = level.getBlockState(climberPos);
		return isSameKind(state) && isCeiling(state) == ceiling && climbsTowards(state, towards);
	}

	/** True if a wire in {@code state} climbs the wall in direction {@code towards} (its main climb or the second one). */
	public static boolean climbsTowards(BlockState state, Direction towards) {
		if(!state.hasProperty(CLIMB) || state.getValue(CLIMB) == Climb.NONE)
			return false;
		Direction hug = state.getValue(HUG).direction();
		return hug == towards || (state.getValue(CLIMB) == Climb.BOTH && hug != null && hug.getOpposite() == towards);
	}

	/** The blocks a climb in {@code state} links to: the top of the climbed wall, and of the opposite one for a second climb. */
	private static Set<BlockPos> climbTargets(BlockPos pos, BlockState state) {
		if(!state.hasProperty(CLIMB) || state.getValue(CLIMB) == Climb.NONE)
			return Set.of();
		Direction hug = state.getValue(HUG).direction();
		boolean ceiling = isCeiling(state);
		Set<BlockPos> out = new HashSet<>();
		out.add(above(pos.relative(hug), ceiling));
		if(state.getValue(CLIMB) == Climb.BOTH)
			out.add(above(pos.relative(hug.getOpposite()), ceiling));
		return out;
	}

	/**
	 * Whether a wire at {@code pos} climbs the block in {@code direction}: that block is a wall, a wire or device of
	 * our kind sits on top of it, and no wire of our kind sits directly above us (that one carries the run up
	 * instead, on the same edge).
	 */
	public boolean canClimb(BlockGetter level, BlockPos pos, Direction direction, boolean ceiling) {
		BlockPos wall = pos.relative(direction);
		if(!isWall(level, wall, direction.getOpposite()))
			return false;
		BlockState target = level.getBlockState(above(wall, ceiling));
		if(!canConnectTo(target) || (isSameKind(target) && isCeiling(target) != ceiling))
			return false; // Nothing to climb to, or a wire the other way up (it could not take the climb in).
		return !isSameKind(level.getBlockState(above(pos, ceiling)));
	}

	private boolean isSameKind(BlockState state) {
		return state.getBlock() instanceof WireBlock wire && !wire.conductsAll() && wire.isPower == isPower && joins(wire);
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
		boolean ceiling = isCeiling(state);
		Direction arrive = arrival(level, pos, state);
		if(arrive != null && !state.getValue(downLink(ceiling))) { // Not from the wire below, so from a climber on that side.
			BlockPos climber = below(pos.relative(arrive), ceiling);
			if(climbsTowards(level, climber, arrive.getOpposite(), ceiling))
				out.put(climber, arrive);
		}
		if(state.getValue(CLIMB) != Climb.NONE) {
			Direction hug = state.getValue(HUG).direction();
			BlockPos top = above(pos.relative(hug), ceiling);
			if(canConnectTo(level.getBlockState(top)))
				out.put(top, hug.getOpposite());
			if(state.getValue(CLIMB) == Climb.BOTH) {
				BlockPos back = above(pos.relative(hug.getOpposite()), ceiling);
				if(canConnectTo(level.getBlockState(back)))
					out.put(back, hug);
			}
		}
		return out;
	}

	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos) {
		return getLinks(level, pos, level.getBlockState(pos));
	}

	/**
	 * The wires of the given kinds that a device at {@code pos} is linked to: its six neighbours, any climber at the
	 * foot of a block below-diagonal that climbs up to it, and any ceiling wire above-diagonal that climbs down to it.
	 * The value is the side of the device the wire touches.
	 */
	public static Map<BlockPos, Direction> wireLinksAround(BlockGetter level, BlockPos pos, Set<WireBlock> wires) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		WireBlock self = level.getBlockState(pos).getBlock() instanceof WireBlock here ? here : null; // A device joins any wire.
		for(Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			if(wires.contains(level.getBlockState(next).getBlock()) && (self == null || self.joins((WireBlock)level.getBlockState(next).getBlock())))
				out.put(next, dir);
			if(dir.getAxis().isHorizontal()) {
				for(boolean ceiling : new boolean[] { false, true }) {
					BlockPos foot = below(next, ceiling); // Below-diagonal for a floor climber, above for a ceiling one.
					BlockState footState = level.getBlockState(foot);
					if(footState.getBlock() instanceof WireBlock wire && wires.contains(wire) && (self == null || self.joins(wire)) && isCeiling(footState) == ceiling
							&& climbsTowards(footState, dir.getOpposite()))
						out.put(foot, dir);
				}
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
		if(isPower && level instanceof ServerLevel serverLevel)
			WireFaults.check(serverLevel, pos); // Shorted, wet, or an open end in the rain.
	}

	/** Power wire ticks now and then to notice rain on an open end, or faults again after a restart (see WireFaults). */
	@Override
	protected boolean isRandomlyTicking(@NotNull BlockState state) {
		return isPower;
	}

	@Override
	protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		WireFaults.check(level, pos);
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

		boolean climbChanged = !climbTargets(pos, old).equals(climbTargets(pos, fresh));
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
		if(isPower && level instanceof ServerLevel serverLevel)
			WireFaults.queue(serverLevel, pos); // Water next to it, a wire joined on: look again next tick.
		// Everything that follows direct neighbours is recomputed; the climb (which depends on a diagonal block and
		// carries network bookkeeping) is left to refresh().
		BlockState fresh = getBlockState(level, pos).setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
		if(state.getValue(CLIMB) != Climb.NONE)
			return fresh.setValue(CLIMB, state.getValue(CLIMB)).setValue(HUG, state.getValue(HUG));
		if(fresh.getValue(CLIMB) != Climb.NONE)
			fresh = fresh.setValue(CLIMB, Climb.NONE).setValue(HUG, Edge.of(hugEdge(level, pos, fresh, arrival(level, pos, fresh))));
		return fresh;
	}

	private record ShapeKey(List<WireLook.Placed> parts, boolean ceiling) {}

	private static final Map<ShapeKey, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

	/**
	 * The shape depends on the neighbours (the look is not stored), so Minecraft must not cache it per state. A
	 * dynamic shape also keeps Minecraft from building a shape cache for every possible state when a world loads.
	 */
	@Override
	public boolean hasDynamicShape() {
		return true;
	}

	/** The outline follows the cable: the pieces {@link WireLook#parts} picks, except the overhangs outside the block. */
	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		List<WireLook.Placed> parts = WireLook.parts(look(level, pos, state), lookConnections(state));
		return SHAPE_CACHE.computeIfAbsent(new ShapeKey(parts, isCeiling(state)),
				key -> key.ceiling() ? flipVertically(buildShape(key.parts())) : buildShape(key.parts()));
	}

	/** {@code shape} upside down within its block: a ceiling wire's outline. */
	private static VoxelShape flipVertically(VoxelShape shape) {
		VoxelShape out = Shapes.empty();
		for(net.minecraft.world.phys.AABB box : shape.toAabbs())
			out = Shapes.or(out, Shapes.box(box.minX, 1.0D - box.maxY, box.minZ, box.maxX, 1.0D - box.minY, box.maxZ));
		return out;
	}

	private static VoxelShape buildShape(List<WireLook.Placed> parts) {
		VoxelShape result = Shapes.empty();
		for(WireLook.Placed placed : parts) {
			VoxelShape piece = switch(placed.part()) {
				case MIDDLE -> MIDDLE_SHAPE;
				case HORIZONTAL -> STUB_SHAPES.get(placed.dir());
				case CORNER -> CORNER_SHAPES.get(placed.dir());
				case VERTICAL -> CENTRE_VERTICAL_SHAPE;
				case EDGE, EDGE_FULL -> EDGE_SHAPES.get(placed.dir());
				case CLIMB_OVERHANG, UP_OVERHANG -> Shapes.empty();
			};
			result = Shapes.or(result, piece);
		}
		return result.isEmpty() ? MIDDLE_SHAPE : result;
	}

	/** Most coax a dye recolours at once: the run it is used on, as far as this. */
	public static final int DYE_RUN_MAX = 256;

	/**
	 * A dye used on coax recolours it, and every piece of the same coax joined to it (sneak: only that one), so runs laid
	 * side by side can be kept apart (see {@link #joins}).
	 */
	@Override
	protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull net.minecraft.world.entity.player.Player player, @NotNull net.minecraft.world.InteractionHand hand, @NotNull net.minecraft.world.phys.BlockHitResult hit) {
		if(isPower || conductsAll() || !(stack.getItem() instanceof net.minecraft.world.item.DyeItem dye))
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		WireBlock target = com.arrl.radiocraft.common.init.RadiocraftBlocks.COLOURED_COAX.get(dye.getDyeColor()).get();
		if(target == this)
			return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!level.isClientSide) {
			List<BlockPos> run = new java.util.ArrayList<>();
			java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(pos));
			java.util.Set<BlockPos> seen = new java.util.HashSet<>(List.of(pos));
			while(!queue.isEmpty() && run.size() < DYE_RUN_MAX) {
				BlockPos at = queue.poll();
				run.add(at);
				if(player.isShiftKeyDown())
					break;
				for(BlockPos next : wireLinksAround(level, at, java.util.Set.of(this)).keySet())
					if(seen.add(next))
						queue.add(next);
			}
			for(BlockPos at : run)
				level.setBlock(at, target.withPropertiesOf(level.getBlockState(at)), Block.UPDATE_ALL);
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DYE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
			if(!player.getAbilities().instabuild)
				stack.shrink(1);
		}
		return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	private boolean canConnectTo(BlockState state) {
		if(state.getBlock() instanceof WireBlock wire)
			return joins(wire); // The same kind of wire (of a colour that matches), a passthrough, or a device.
		return isValidConnection(state, isPower);
	}

	/**
	 * True if this wire and {@code other} join up when they touch: the same kind (power or coax), and for coax, the same
	 * colour or either one plain. So dyed runs laid side by side stay apart. A passthrough joins anything.
	 */
	public boolean joins(WireBlock other) {
		if(conductsAll() || other.conductsAll())
			return true;
		if(isPower != other.isPower)
			return false;
		return colour == null || other.colour == null || colour == other.colour;
	}

	/** True for a conductor every kind of wire runs through (the passthrough block). */
	public boolean conductsAll() {
		return false;
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

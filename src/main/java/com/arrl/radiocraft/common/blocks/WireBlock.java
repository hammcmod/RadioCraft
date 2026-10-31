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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
 * A tower beam counts as a wall to run up and climb, though its faces are not solid: coax goes up a tower leg to the
 * antenna on top. The steel is set back in the beam's block, so the cable is drawn out against it
 * ({@link WireLook.Look#hugReach}); where the beam ends under a run that carries on up, the run eases back to the
 * block edge over the next block.
 * <p>
 * The climb and the arrival from a climber depend on blocks that are not direct neighbours, so they are only
 * computed on placement and in {@link #refresh}, which {@link WireClimbUpdater} runs for every wire near a changed
 * block. {@link #refresh} also merges or splits the networks when the climb link appears, disappears or moves,
 * since no wire is placed or removed then. Everything that follows direct neighbours is kept up to date by
 * {@link #updateShape} as well.
 * <p>
 * Several wires can share a block as a {@link WireBundleBlock}: each cable in it (a {@link Strand}) keeps the state
 * it would have as a wire block of its own, and every rule here is written against strands, so a neighbour that is a
 * bundle counts for the kinds of cable it holds. Using a wire item on a wire of another kind makes the bundle.
 */
public class WireBlock extends Block implements SimpleWaterloggedBlock {

	public final boolean isPower;
	/**
	 * Data wire (2026-10-30): the line between radios, their accessories, wired speakers and the stations a microwave
	 * radio serves. It carries no power and joins no power or coax network: what it links is found by walking it
	 * ({@code DataWire}). Power wire no longer links devices.
	 */
	public final boolean isData;
	public final boolean isWaterproof;
	/** A dyed cable's colour (null for plain): it only joins cable of the same colour, plain counting as one (see {@link #joins}). */
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

	private static final double SHAPE_PADDING = 1.0D; // The cable with 1 px either side: enough to click, tight enough to read as the cable.

	/** The outline pieces of a cable {@code w} pixels square, centred on the middle of the block (see tools/wire_assets.py). */
	private record Pieces(VoxelShape middle, VoxelShape vertical, Map<Direction, VoxelShape> stubs, Map<Direction, VoxelShape> edges, Map<Direction, VoxelShape> corners) {
		static Pieces of(double w) {
			double lo = 8.0D - w / 2.0D, hi = 8.0D + w / 2.0D, h = w / 2.0D;
			Map<Direction, VoxelShape> stubs = new HashMap<>(), edges = new HashMap<>(), corners = new HashMap<>();
			corners.put(Direction.NORTH, makePaddedBox(lo, 0.0D, 0.0D, hi, w, h));
			corners.put(Direction.SOUTH, makePaddedBox(lo, 0.0D, 16.0D - h, hi, w, 16.0D));
			corners.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, lo, h, w, hi));
			corners.put(Direction.EAST, makePaddedBox(16.0D - h, 0.0D, lo, 16.0D, w, hi));
			stubs.put(Direction.NORTH, makePaddedBox(lo, 0.0D, 0.0D, hi, w, lo));
			stubs.put(Direction.SOUTH, makePaddedBox(lo, 0.0D, hi, hi, w, 16.0D));
			stubs.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, lo, lo, w, hi));
			stubs.put(Direction.EAST, makePaddedBox(hi, 0.0D, lo, 16.0D, w, hi));
			// The vertical run along an edge, floor to ceiling (it straddles the face; only the half inside the block counts).
			edges.put(Direction.NORTH, makePaddedBox(lo, 0.0D, 0.0D, hi, 16.0D, h));
			edges.put(Direction.SOUTH, makePaddedBox(lo, 0.0D, 16.0D - h, hi, 16.0D, 16.0D));
			edges.put(Direction.WEST, makePaddedBox(0.0D, 0.0D, lo, h, 16.0D, hi));
			edges.put(Direction.EAST, makePaddedBox(16.0D - h, 0.0D, lo, 16.0D, 16.0D, hi));
			return new Pieces(makePaddedBox(lo, 0.0D, lo, hi, w, hi), makePaddedBox(lo, w, lo, hi, 16.0D, hi), stubs, edges, corners);
		}
	}

	private static final Map<Double, Pieces> PIECES = new ConcurrentHashMap<>();

	/** How thick the cable is drawn, pixels: power and data wire 1, coax 2 (thin hookup wire against fat RG-8, so they tell apart however dyed). */
	public double width() {
		return isCoax() ? 2.0D : 1.0D;
	}

	/** True for coax of any colour (not power wire, data wire or a passthrough). */
	public boolean isCoax() {
		return !isPower && !isData && !conductsAll();
	}

	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof) {
		this(properties, isPower, isWaterproof, null);
	}

	/** {@code colour} for dyed coax (null for plain): see {@link #joins}. */
    @SuppressWarnings("this-escape")
	public WireBlock(Properties properties, boolean isPower, boolean isWaterproof, @Nullable net.minecraft.world.item.DyeColor colour) {
		this(properties, isPower, false, isWaterproof, colour);
	}

	/** The data wire: {@code isData} true, {@code isPower} false. */
	public static WireBlock data(Properties properties) {
		return data(properties, null);
	}

	/** Data wire dyed {@code colour} (null for plain): see {@link #joins}. */
	public static WireBlock data(Properties properties, @Nullable net.minecraft.world.item.DyeColor colour) {
		return new WireBlock(properties, false, true, false, colour);
	}

    @SuppressWarnings("this-escape")
	protected WireBlock(Properties properties, boolean isPower, boolean isData, boolean isWaterproof, @Nullable net.minecraft.world.item.DyeColor colour) {
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
		this.isData = isData;
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
		if(Strand.isCable(level.getBlockState(against)))
			return Strand.isCeilingAt(level, against);
		BlockPos floor = pos.below(), ceiling = pos.above();
		return !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) && level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
	}

	/**
	 * Calculate desired blockstate for a wire given its connections, the walls around it and the fluid at its location,
	 * keeping whether the wire already there hangs from the ceiling.
	 */
	public BlockState getBlockState(BlockGetter level, BlockPos pos) {
		return getBlockState(level, pos, Strand.isCeilingAt(level, pos));
	}

	/** As {@link #getBlockState(BlockGetter, BlockPos)}, for a wire on the floor or hanging from the ceiling. */
	public BlockState getBlockState(BlockGetter level, BlockPos pos, boolean ceiling) {
		BlockState state = defaultBlockState()
				.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
				.trySetValue(CEILING, ceiling);

		for(Direction direction : Direction.values())
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, pos.relative(direction)));
		if(state.hasProperty(CEILING) && settledCeiling(level, pos, state, ceiling) != ceiling) {
			ceiling = !ceiling;
			state = state.setValue(CEILING, ceiling);
		}
		Direction arrive = arrival(level, pos, state);

		// A climb if there is one, trying the edge the run from below is already on first so the cable keeps its line.
		for(Direction dir : wallOrder(Edge.of(arrive))) {
			if(canClimb(level, pos, dir, ceiling)) {
				Direction back = dir.getOpposite();
				boolean climbBack = canClimb(level, pos, back, ceiling) && sameKindState(level, above(pos.relative(back), ceiling)) != null;
				return state.setValue(HUG, Edge.of(dir)).setValue(CLIMB, climbBack ? Climb.BOTH : Climb.ONE);
			}
		}
		return state.setValue(HUG, Edge.of(hugEdge(level, pos, state, arrive))).setValue(CLIMB, Climb.NONE);
	}

	/**
	 * Whether the wire in {@code state} (its connections worked out) at {@code pos} should hang from the ceiling, from
	 * {@code ceiling}, whether it hangs now: a cable joined sideways only to cable of its kind on the other level (a
	 * hanging one beside floor wire, say) goes over to that level, since a floor wire and a hanging one side by side
	 * cannot meet. Up and down links do not count: a run up a wall may turn onto a ceiling at its top.
	 */
	private boolean settledCeiling(BlockGetter level, BlockPos pos, BlockState state, boolean ceiling) {
		int hanging = 0, floor = 0;
		for(Direction dir : Direction.Plane.HORIZONTAL) {
			if(!state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				continue;
			BlockState there = sameKindState(level, pos.relative(dir));
			if(there == null || !there.hasProperty(CEILING))
				continue;
			if(isCeiling(there))
				hanging++;
			else
				floor++;
		}
		if(ceiling && floor > 0 && hanging == 0)
			return false;
		if(!ceiling && hanging > 0 && floor == 0)
			return true;
		return ceiling;
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
			end = sameKindState(level, above(pos.relative(hug), ceiling)) != null ? WireLook.End.CLIMB_WIRE : WireLook.End.CLIMB_DEVICE;
		else if(hug != null) {
			BlockPos abovePos = above(pos, ceiling);
			if(sameKindState(level, abovePos) == null && canConnectTo(level, abovePos))
				end = WireLook.End.UP_DEVICE; // The run up the edge ends at a device above: turn in to its middle.
		}
		Direction arrive = arrival(level, pos, state);
		int hugReach = hug == null ? 0 : beamReach(level, pos.relative(hug));
		int backReach = hug != null && climb == Climb.BOTH ? beamReach(level, pos.relative(hug.getOpposite())) : 0;
		int arriveReach = arrive == null ? 0 : arrivalReach(level, pos, state, arrive);
		// The run up the hugged edge, at the bottom and top faces of the block (world): where it carries on into the
		// block beyond, both blocks take the larger of their two walls' reaches there, so a run beside a lattice leg
		// (beam, gap, beam) keeps a straight line against the steel; at an end it takes this block's wall.
		int reachBottom = hug == null ? 0 : runReach(level, pos, hug, Direction.DOWN, hugReach);
		int reachTop = hug == null ? 0 : runReach(level, pos, hug, Direction.UP, hugReach);
		Direction feed = null;
		int feedSide = 0;
		if(isCoax())
			for(Direction dir : Direction.Plane.HORIZONTAL)
				if(state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
					Integer side = hornFeedSide(level, pos, dir);
					if(side != null) {
						feed = dir;
						feedSide = side;
						break;
					}
				}
		int stubs = 0;
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(dir != hug && dir != arrive && state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
				int set = turnsOutTowards(level, pos.relative(dir), dir.getOpposite());
				if(set > 0)
					stubs = WireLook.Look.withStub(stubs, dir, -set);
			}
		return new WireLook.Look(hug, arrive, end, climb == Climb.BOTH, hugReach, arriveReach, backReach, feed, feedSide, reachBottom, reachTop, stubs);
	}

	/**
	 * The reach of the run up edge {@code hug} at the {@code face} (up or down) of the block at {@code pos}: if the run
	 * carries on across it (linked, and the cable there runs on the same edge), the larger of the two blocks' walls;
	 * else this block's own ({@code own}).
	 */
	private int runReach(BlockGetter level, BlockPos pos, Direction hug, Direction face, int own) {
		BlockState here = sameKindState(level, pos);
		if(here == null || !here.getValue(PROPERTY_BY_DIRECTION.get(face)))
			return own;
		BlockPos next = pos.relative(face);
		BlockState there = sameKindState(level, next);
		if(there == null || !there.hasProperty(HUG) || there.getValue(HUG).direction() != hug)
			return own;
		return Math.max(own, beamReach(level, next.relative(hug)));
	}

	/**
	 * If the block on side {@code dir} of a wire at {@code pos} is a microwave horn's base, how far to the side of the
	 * wire's line its feed is, in pixels (along x for north or south, z for east or west): the feed is where the four
	 * base blocks meet, under the throat. Null for anything else.
	 */
	@Nullable
	public static Integer hornFeedSide(BlockGetter level, BlockPos pos, Direction dir) {
		BlockPos hornPos = pos.relative(dir);
		BlockState horn = level.getBlockState(hornPos);
		if(!(horn.getBlock() instanceof MicrowaveHornBlock) || horn.getValue(MicrowaveHornBlock.PART_Y) != 0)
			return null;
		double[] feed = MicrowaveHornBlock.feedXZ(MicrowaveHornBlock.originOf(horn, hornPos), horn.getValue(MicrowaveHornBlock.FACING));
		double lateral = dir.getAxis() == Direction.Axis.Z ? feed[0] - (pos.getX() + 0.5D) : feed[1] - (pos.getZ() + 0.5D);
		return (int)Math.round(lateral * 16.0D);
	}

	/**
	 * Pixels from the face of the block at {@code wallPos} to the steel of the tower beam there (8 minus half its
	 * width), which a cable running up it is drawn out against; 0 for anything else (a solid wall: the face itself).
	 */
	public static int beamReach(BlockGetter level, BlockPos wallPos) {
		return level.getBlockState(wallPos).getBlock() instanceof TowerBeamBlock beam ? 8 - beam.getWidth() / 2 : 0;
	}

	/**
	 * How far out the run from below sits where it comes in on edge {@code arrive} ({@link WireLook.Look#arriveReach}):
	 * the wire below ran up a beam on that side, or a climber beside the beam under us ran up it (set back into this
	 * block, so negative). Only blocks next to the ones below us, so a redraw of the blocks around a change covers it.
	 */
	private int arrivalReach(BlockGetter level, BlockPos pos, BlockState state, Direction arrive) {
		boolean ceiling = isCeiling(state);
		BlockPos below = below(pos, ceiling);
		if(state.getValue(downLink(ceiling)) && sameKindState(level, below) != null) // Where it crosses the face: the larger wall.
			return Math.max(beamReach(level, below.relative(arrive)), beamReach(level, pos.relative(arrive)));
		return -beamReach(level, below);
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
		BlockState below = sameKindState(level, below(pos, ceiling));
		if(state.getValue(downLink(ceiling)) && below != null) {
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
		BlockState facing = sameKindState(level, above(pos, ceiling));
		if(facing != null && isCeiling(facing) != ceiling && facing.getValue(HUG).direction() != null)
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
		BlockState above = sameKindState(level, above(pos, ceiling));
		if(above == null)
			return null;
		for(Direction dir : Direction.Plane.HORIZONTAL)
			if(above.getValue(PROPERTY_BY_DIRECTION.get(dir)))
				return dir;
		return null;
	}

	/**
	 * Something we can run a cable up: a face sturdy towards us, or a tower beam, and not something the wire links to
	 * instead.
	 */
	private boolean isWall(BlockGetter level, BlockPos wallPos, Direction faceTowardsUs) {
		if(canConnectTo(level, wallPos))
			return false;
		BlockState wall = level.getBlockState(wallPos);
		return wall.getBlock() instanceof TowerBeamBlock || wall.isFaceSturdy(level, wallPos, faceTowardsUs);
	}

	/**
	 * True if the block at {@code climberPos} is a wire of this kind climbing the wall in direction {@code towards},
	 * the same way up as {@code ceiling} (a floor wire climbs up, a ceiling wire down).
	 */
	public boolean climbsTowards(BlockGetter level, BlockPos climberPos, Direction towards, boolean ceiling) {
		BlockState state = sameKindState(level, climberPos);
		return state != null && isCeiling(state) == ceiling && climbsTowards(state, towards);
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
		BlockPos targetPos = above(wall, ceiling);
		BlockState target = sameKindState(level, targetPos);
		if(!canConnectTo(level, targetPos) || (target != null && isCeiling(target) != ceiling))
			return false; // Nothing to climb to, or a wire the other way up (it could not take the climb in).
		return sameKindState(level, above(pos, ceiling)) == null;
	}

	/** True if {@code other} is a wire this one runs on as (power with power, coax with coax of a colour that joins). */
	public boolean isSameKind(WireBlock other) {
		return !other.conductsAll() && other.isPower == isPower && other.isData == isData && joins(other);
	}

	/**
	 * The state of the wire of this kind at {@code pos}: the wire block there if it is one, or in a bundle the strand of
	 * exactly this kind, else the first strand of the same kind. Null if there is none.
	 */
	@Nullable
	public BlockState sameKindState(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() instanceof WireBlock wire)
			return isSameKind(wire) ? state : null;
		if(!(state.getBlock() instanceof WireBundleBlock))
			return null;
		BlockState fallback = null;
		for(BlockState strand : Strand.statesAt(level, pos)) {
			WireBlock kind = (WireBlock)strand.getBlock();
			if(kind == this)
				return strand;
			if(fallback == null && isSameKind(kind))
				fallback = strand;
		}
		return fallback;
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
			if(canConnectTo(level, top))
				out.put(top, hug.getOpposite());
			if(state.getValue(CLIMB) == Climb.BOTH) {
				BlockPos back = above(pos.relative(hug.getOpposite()), ceiling);
				if(canConnectTo(level, back))
					out.put(back, hug);
			}
		}
		return out;
	}

	public Map<BlockPos, Direction> getLinks(BlockGetter level, BlockPos pos) {
		return getLinks(level, pos, level.getBlockState(pos));
	}

	/**
	 * The cables of the given kinds that a device at {@code pos} is linked to: its six neighbours, any climber at the
	 * foot of a block below-diagonal that climbs up to it, and any ceiling wire above-diagonal that climbs down to it.
	 * A bundle counts if any of its strands is of one of the kinds. The value is the side of the device the wire touches.
	 */
	public static Map<BlockPos, Direction> wireLinksAround(BlockGetter level, BlockPos pos, Set<WireBlock> wires) {
		Map<BlockPos, Direction> out = new LinkedHashMap<>();
		for(Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			for(Strand strand : Strand.at(level, next))
				if(wires.contains(strand.kind())) {
					out.put(next, dir);
					break;
				}
			if(dir.getAxis().isHorizontal()) {
				for(boolean ceiling : new boolean[] { false, true }) {
					BlockPos foot = below(next, ceiling); // Below-diagonal for a floor climber, above for a ceiling one.
					for(BlockState footState : Strand.statesAt(level, foot))
						if(wires.contains((WireBlock)footState.getBlock()) && isCeiling(footState) == ceiling && climbsTowards(footState, dir.getOpposite())) {
							out.put(foot, dir);
							break;
						}
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
		strandPlaced(level, pos, state);
	}

	/** The network bookkeeping for a strand of this kind that has just appeared at {@code pos} (as a block or in a bundle). */
	public void strandPlaced(Level level, BlockPos pos, BlockState state) {
		if(isData) {
			com.arrl.radiocraft.common.be_networks.DataWire.changed(level); // No network: what it links is walked when asked.
			return;
		}
		if(checkMultipleSides(level, pos, state)) // Only bother running merge if this wire is actually connecting things.
			WireUtils.mergeNetworks(level, new Strand(pos, this), connectionPredicate(), networkSupplier(), networkWires());
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
		if(newState.getBlock() instanceof WireBundleBlock)
			return; // Another cable is joining it: this one carries on as a strand of the bundle (see WireBundleBlock#add).

		strandRemoved(level, pos, state, Set.of(new Strand(pos, this)));
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/**
	 * The network bookkeeping for a strand of this kind that was at {@code pos} in {@code state} and is gone, with
	 * every strand gone at the same time in {@code gone} (the walk must not pass through any of them).
	 */
	public void strandRemoved(Level level, BlockPos pos, BlockState state, Set<Strand> gone) {
		if(isData) {
			com.arrl.radiocraft.common.be_networks.DataWire.changed(level);
			return;
		}
		Map<BlockPos, Direction> links = getLinks(level, pos, state); // The old state: the strand is already gone.
		if(links.size() >= 2) // Only bother running split if this wire was actually connecting things.
			WireUtils.splitNetworks(level, new Strand(pos, this), gone, links, connectionPredicate(), networkWires());
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
		refreshStrand(level, pos, old, fresh -> level.setBlock(pos, fresh, Block.UPDATE_ALL));
	}

	/**
	 * {@link #refresh} for a strand of this kind at {@code pos} whose current state is {@code old}: works out the fresh
	 * state and, if it differs, hands it to {@code store} (which must make it the strand's state before returning, as
	 * the network walk that follows reads it back) and settles the networks. Returns whether anything changed.
	 */
	public boolean refreshStrand(Level level, BlockPos pos, BlockState old, Consumer<BlockState> store) {
		BlockState fresh = getBlockState(level, pos, isCeiling(old));
		if(fresh == old)
			return false;

		boolean climbChanged = !climbTargets(pos, old).equals(climbTargets(pos, fresh));
		Map<BlockPos, Direction> oldLinks = climbChanged ? getLinks(level, pos, old) : Map.of();

		store.accept(fresh);
		if(isData)
			com.arrl.radiocraft.common.be_networks.DataWire.changed(level);
		if(!climbChanged || isData)
			return true;

		Map<BlockPos, Direction> newLinks = getLinks(level, pos, fresh);
		Strand strand = new Strand(pos, this);
		if(!oldLinks.keySet().containsAll(newLinks.keySet()))
			WireUtils.mergeNetworks(level, strand, connectionPredicate(), networkSupplier(), networkWires());
		if(!newLinks.keySet().containsAll(oldLinks.keySet())) {
			Map<BlockPos, Direction> all = new LinkedHashMap<>(oldLinks);
			all.putAll(newLinks);
			WireUtils.splitNetworks(level, strand, Set.of(), all, connectionPredicate(), networkWires());
		}
		return true;
	}

	@Override
	public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
										   @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if(isPower && level instanceof ServerLevel serverLevel)
			WireFaults.queue(serverLevel, pos); // Water next to it, a wire joined on: look again next tick.
		if(isData && level instanceof ServerLevel serverLevel)
			com.arrl.radiocraft.common.be_networks.DataWire.changed(serverLevel); // A device came or went at its end.
		return followNeighbours(level, pos, state);
	}

	/**
	 * {@code state} with everything that follows direct neighbours recomputed; the climb (which depends on a diagonal
	 * block and carries network bookkeeping) is left to {@link #refresh}.
	 */
	public BlockState followNeighbours(BlockGetter level, BlockPos pos, BlockState state) {
		BlockState fresh = getBlockState(level, pos, isCeiling(state)).setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
		if(state.getValue(CLIMB) != Climb.NONE)
			return fresh.setValue(CLIMB, state.getValue(CLIMB)).setValue(HUG, state.getValue(HUG));
		if(fresh.getValue(CLIMB) != Climb.NONE)
			fresh = fresh.setValue(CLIMB, Climb.NONE).setValue(HUG, Edge.of(hugEdge(level, pos, fresh, arrival(level, pos, fresh))));
		return fresh;
	}

	private record ShapeKey(List<WireLook.Placed> parts, boolean ceiling, WireLook.Lateral offset, WireLook.Faces faces, double width) {}

	/**
	 * Where a strand of this kind at {@code pos}, drawn {@code offset} off the middle, meets the cable in each
	 * neighbouring block: halfway between its offset and the neighbour's ({@link Strand#offsetOf}), so both sides draw
	 * the crossing in the same place.
	 */
	public WireLook.Faces faces(BlockGetter level, BlockPos pos, WireLook.Lateral offset) {
		WireLook.Lateral[] at = new WireLook.Lateral[6];
		BlockState own = sameKindState(level, pos);
		for(Direction dir : Direction.values()) {
			// Halfway to the cable we link to there; where we link to nothing (a run up a wall or beam, a dead end) the
			// cable stays at our own place, not drawn towards the middle of a block that has no cable to meet. Where
			// one side's cable is a run up its edge on that face (not a stub), the meeting is at that run: the run is
			// a straight line, and the other side's stub goes all the way to it.
			boolean linked = own != null && own.getValue(PROPERTY_BY_DIRECTION.get(dir));
			if(!linked) {
				at[dir.ordinal()] = offset;
				continue;
			}
			WireLook.Lateral theirs = Strand.offsetOf(level, pos.relative(dir), this);
			if(dir.getAxis().isHorizontal() && own.hasProperty(HUG) && (own.getValue(HUG).direction() == dir
					|| (own.getValue(HUG).direction() == null && arrival(level, pos, own) == dir)))
				at[dir.ordinal()] = offset; // Our run up this edge, or our corner turning out through it: our line.
			else if(dir.getAxis().isHorizontal() && runsUpFace(level, pos.relative(dir), dir.getOpposite()))
				at[dir.ordinal()] = theirs;
			else
				at[dir.ordinal()] = offset.halfwayTo(theirs);
		}
		return new WireLook.Faces(at[Direction.DOWN.ordinal()], at[Direction.UP.ordinal()], at[Direction.NORTH.ordinal()],
				at[Direction.SOUTH.ordinal()], at[Direction.WEST.ordinal()], at[Direction.EAST.ordinal()]);
	}

	/**
	 * Whether this kind's cable at {@code pos} runs up its {@code face} edge, or comes up that edge and turns out
	 * through it (a corner): either way it meets that face at its own line, not a stub's.
	 */
	private boolean runsUpFace(BlockGetter level, BlockPos pos, Direction face) {
		BlockState there = sameKindState(level, pos);
		if(there == null || !there.hasProperty(HUG))
			return false;
		Direction hug = there.getValue(HUG).direction();
		return hug == face || (hug == null && arrival(level, pos, there) == face);
	}

	/**
	 * How far past its {@code face} the corner of this kind's cable at {@code pos} sits, where it comes up that edge
	 * against a beam and turns out through it (a run up a beam turning back over the beam's top); 0 for anything else.
	 */
	private int turnsOutTowards(BlockGetter level, BlockPos pos, Direction face) {
		BlockState there = sameKindState(level, pos);
		if(there == null || !there.hasProperty(HUG) || there.getValue(HUG).direction() != null || arrival(level, pos, there) != face
				|| !there.getValue(PROPERTY_BY_DIRECTION.get(face)))
			return 0;
		return Math.max(0, arrivalReach(level, pos, there, face));
	}

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
	/**
	 * Skylight passes a cable (unless it is under water), decided without its shape: the shape reads the neighbours,
	 * and the light engine asks from its own thread, where reading a neighbour in a chunk not loaded yet waits on the
	 * server thread, which may be waiting on the light (a hang, as when a radio site generated).
	 */
	@Override
	protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
		return state.getFluidState().isEmpty();
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return shapeFor(level, pos, state, Strand.loneOffset(level, pos, state));
	}

	/** The outline of a wire of this kind in {@code state} at {@code pos}, drawn {@code offset} sixteenths off the middle. */
	public VoxelShape shapeFor(BlockGetter level, BlockPos pos, BlockState state, WireLook.Lateral offset) {
		List<WireLook.Placed> parts = WireLook.parts(look(level, pos, state), lookConnections(state));
		WireLook.Faces faces = faces(level, pos, offset);
		return SHAPE_CACHE.computeIfAbsent(new ShapeKey(parts, isCeiling(state), offset, faces, width()), key -> buildShape(key));
	}

	/** {@code shape} upside down within its block: a ceiling wire's outline. */
	private static VoxelShape flipVertically(VoxelShape shape) {
		VoxelShape out = Shapes.empty();
		for(AABB box : shape.toAabbs())
			out = Shapes.or(out, Shapes.box(box.minX, 1.0D - box.maxY, box.minZ, box.maxX, 1.0D - box.minY, box.maxZ));
		return out;
	}

	/** The pieces of {@code key}, each moved and slanted for its strand ({@link WireLook#affine}), flipped for a ceiling. */
	private static VoxelShape buildShape(ShapeKey key) {
		Pieces pieces = PIECES.computeIfAbsent(key.width(), Pieces::of);
		boolean plain = key.offset().isZero() && key.faces().equals(WireLook.Faces.all(0.0D));
		VoxelShape result = Shapes.empty();
		for(WireLook.Placed placed : key.parts()) {
			VoxelShape piece = switch(placed.part()) {
				case MIDDLE -> pieces.middle();
				case HORIZONTAL -> pieces.stubs().get(placed.dir());
				case CORNER -> pieces.corners().get(placed.dir());
				case VERTICAL -> pieces.vertical();
				case EDGE, EDGE_FULL -> pieces.edges().get(placed.dir());
				case CLIMB_OVERHANG, UP_OVERHANG -> Shapes.empty();
			};
			if(key.ceiling())
				piece = flipVertically(piece);
			// The hitbox keeps cable against a beam at the block edge: out at the steel it would be in the beam's block.
			result = Shapes.or(result, plain ? piece : moved(piece, WireLook.affine(placed.plain(), key.offset(), key.faces(), key.ceiling())));
		}
		if(!result.isEmpty())
			return result;
		VoxelShape middle = key.ceiling() ? flipVertically(pieces.middle()) : pieces.middle();
		return plain ? middle : moved(middle, WireLook.affine(new WireLook.Placed(WireLook.Part.MIDDLE, null), key.offset(), key.faces(), key.ceiling()));
	}

	/** {@code shape} with {@code affine} applied to its boxes (the box round each box's moved corners), kept inside the block. */
	private static VoxelShape moved(VoxelShape shape, WireLook.Affine affine) {
		VoxelShape out = Shapes.empty();
		for(AABB box : shape.toAabbs()) {
			double x0 = 1.0D, x1 = 0.0D, z0 = 1.0D, z1 = 0.0D;
			for(int corner = 0; corner < 8; corner++) {
				double x = (corner & 1) == 0 ? box.minX : box.maxX, y = (corner & 2) == 0 ? box.minY : box.maxY, z = (corner & 4) == 0 ? box.minZ : box.maxZ;
				double mx = affine.x(x, y, z), mz = affine.z(x, y, z);
				x0 = Math.min(x0, mx);
				x1 = Math.max(x1, mx);
				z0 = Math.min(z0, mz);
				z1 = Math.max(z1, mz);
			}
			x0 = Math.max(0.0D, x0);
			x1 = Math.min(1.0D, x1);
			z0 = Math.max(0.0D, z0);
			z1 = Math.min(1.0D, z1);
			if(x1 > x0 && z1 > z0)
				out = Shapes.or(out, Shapes.box(x0, box.minY, z0, x1, box.maxY, z1));
		}
		return out;
	}

	/** Most coax a dye recolours at once: the run it is used on, as far as this. */
	public static final int DYE_RUN_MAX = 256;

	/**
	 * A dye used on coax or power wire recolours it, and every piece of the same cable joined to it (sneak: only that
	 * one), so runs laid side by side can be kept apart (see {@link #joins}); waterproof wire stays plain. A wire item
	 * of another kind used on a wire lays that cable in the same block, making a {@link WireBundleBlock}; the same kind
	 * (or sneaking) places it next to it as usual.
	 */
	@Override
	protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull net.minecraft.world.entity.player.Player player, @NotNull net.minecraft.world.InteractionHand hand, @NotNull net.minecraft.world.phys.BlockHitResult hit) {
		if(!conductsAll() && stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof WireBlock kind
				&& !kind.conductsAll() && kind != this && WireBundleBlock.canAdd(level, pos, kind)) {
			if(!level.isClientSide) {
				WireBundleBlock.add(level, pos, kind);
				level.playSound(null, pos, getSoundType(state, level, pos, player).getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
				if(!player.getAbilities().instabuild)
					stack.shrink(1);
			}
			return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if(isWaterproof || conductsAll() || !(stack.getItem() instanceof net.minecraft.world.item.DyeItem dye))
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		WireBlock target = (isPower ? com.arrl.radiocraft.common.init.RadiocraftBlocks.COLOURED_WIRE
				: isData ? com.arrl.radiocraft.common.init.RadiocraftBlocks.COLOURED_DATA_WIRE
				: com.arrl.radiocraft.common.init.RadiocraftBlocks.COLOURED_COAX).get(dye.getDyeColor()).get();
		if(target == this)
			return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if(!level.isClientSide) {
			dyeRun(level, new Strand(pos, this), target, player.isShiftKeyDown());
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DYE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
			if(!player.getAbilities().instabuild)
				stack.shrink(1);
		}
		return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/**
	 * Recolours the strand {@code start} to {@code target} and, unless {@code single}, every strand of exactly its
	 * kind joined to it, up to {@link #DYE_RUN_MAX}. In a bundle that already holds {@code target} the strand is left as
	 * it is (two cables of one kind in a block would be one cable).
	 */
	public static void dyeRun(Level level, Strand start, WireBlock target, boolean single) {
		WireBlock kind = Objects.requireNonNull(start.kind());
		List<Strand> run = new ArrayList<>();
		ArrayDeque<Strand> queue = new ArrayDeque<>(List.of(start));
		Set<Strand> seen = new HashSet<>(List.of(start));
		while(!queue.isEmpty() && run.size() < DYE_RUN_MAX) {
			Strand at = queue.poll();
			BlockState state = at.state(level);
			if(state == null)
				continue;
			run.add(at);
			if(single)
				break;
			for(BlockPos next : kind.getLinks(level, at.pos(), state).keySet()) {
				Strand same = new Strand(next, kind);
				if(same.state(level) != null && seen.add(same))
					queue.add(same);
			}
		}
		for(Strand at : run) {
			BlockState state = at.state(level);
			if(state == null)
				continue;
			if(level.getBlockState(at.pos()).getBlock() == kind)
				level.setBlock(at.pos(), target.withPropertiesOf(state), Block.UPDATE_ALL);
			else
				WireBundleBlock.recolour(level, at.pos(), kind, target);
		}
	}

	/**
	 * True if a wire of this kind at some position links to the block at {@code pos}: a cable there of a kind it
	 * joins (a wire block, a strand of a bundle, or a passthrough), or a device its kind connects to.
	 */
	public boolean canConnectTo(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() instanceof MicrowaveHornBlock) // Coax goes to the feed, in the base: only the bottom layer takes it.
			return isCoax() && state.getValue(MicrowaveHornBlock.PART_Y) == 0;
		if(state.getBlock() instanceof WireBlock wire)
			return joins(wire); // The same kind of wire (of a colour that matches), a passthrough, or a device.
		if(state.getBlock() instanceof WireBundleBlock) {
			for(Strand strand : Strand.at(level, pos))
				if(joins(strand.kind()))
					return true;
			return false;
		}
		return isData ? isDataConnection(state) : isValidConnection(state, isPower);
	}

	/**
	 * True if this wire and {@code other} join up when they touch: the same kind (power or coax), and for coax, the same
	 * colour, plain being a colour of its own (maintainer, 2026-10-28: plain wire used to join every colour). So runs of
	 * different colours laid side by side stay apart, plain beside dyed too. A passthrough joins anything; devices take
	 * any colour (they are not wires).
	 */
	public boolean joins(WireBlock other) {
		if(conductsAll() || other.conductsAll())
			return true;
		if(isPower != other.isPower || isData != other.isData)
			return false;
		return colour == other.colour;
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

	/** True for a block data wire runs into: one tagged {@code radiocraft:data_blocks}, or any radio. */
	public static boolean isDataConnection(BlockState state) {
		return state.is(RadiocraftTags.Blocks.DATA_BLOCKS) || state.getBlock() instanceof RadioBlock;
	}

	public static boolean isValidConnection(BlockState state, boolean isPower) {
		return isPower ? state.is(RadiocraftTags.Blocks.POWER_BLOCKS) : state.is(RadiocraftTags.Blocks.COAX_BLOCKS) || state.getBlock() instanceof RadioBlock;
	}

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft." + (isData ? "data_wire" : isPower ? (isWaterproof ? "waterproof_wire" : "wire") : "coax_wire")));
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.wire_bundle").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which way round a run of bundles lays its strands, so they keep their order round bends. A bundle lays its strands
 * out across the run, in a fixed order; across a north-south run that is along x, across an east-west run along z.
 * Round a bend from one to the other, the strand on the inside must stay on the inside: for a bend between north and
 * west, or south and east, the two layouts run the same way; between north and east, or south and west, opposite ways.
 * With one fixed way for each axis, half the bends would cross every strand over the others.
 * <p>
 * So each bundle may lay either axis out turned round ({@link WireBundleBlockEntity#flipX}, {@code flipZ}), and this
 * works out the turns for a whole connected run of bundles at once: bundles linked through a north or south face share
 * the x way, through east or west the z way, up or down the way along the wall the run goes up (both up the middle); a
 * bend ties a bundle's two ways together, the same or opposite. A climb (a run up a wall onto the block on top of it,
 * a diagonal link) shares the way along the wall. Solved as a parity union-find; where a loop of bends cannot all be kept (an odd loop) the last bend is let
 * cross. The turns already set are kept where they can be, so adding one bundle does not flip a whole run. Server side:
 * changes are queued ({@link #queue}) and solved at the end of the tick; the turns sync to clients with the bundles.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class BundleOrientation {

	/** Most bundles one solve looks at: a longer run is solved in pieces, each fine on its own. */
	public static final int MAX_BUNDLES = 4096;

	private static final Map<ResourceKey<Level>, Set<BlockPos>> PENDING = new HashMap<>();

	/** Bundles just loaded, whose strands are worked out again before the solve (a save from an older version). */
	private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

	private BundleOrientation() {}

	/** A bundle at {@code pos} was loaded: refresh its strands ({@link WireBundleBlock#refresh}) and solve its run. */
	public static void queueLoaded(Level level, BlockPos pos) {
		if(level instanceof ServerLevel)
			LOADED.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(pos.immutable());
		queue(level, pos);
	}

	/** The bundle at {@code pos} (or one beside it) changed: solve its run at the end of the tick. */
	public static void queue(Level level, BlockPos pos) {
		if(level instanceof ServerLevel)
			PENDING.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(pos.immutable());
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		Set<BlockPos> loaded = LOADED.remove(level.dimension());
		if(loaded != null)
			for(BlockPos pos : loaded)
				if(level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof WireBundleBlock bundle)
					bundle.refresh(level, pos);
		Set<BlockPos> pending = PENDING.remove(level.dimension());
		if(pending == null)
			return;
		Set<BlockPos> done = new HashSet<>();
		for(BlockPos pos : pending)
			for(BlockPos start : around(pos))
				if(!done.contains(start) && bundle(level, start) != null)
					solve(level, start, done);
	}

	private static List<BlockPos> around(BlockPos pos) {
		List<BlockPos> out = new ArrayList<>(7);
		out.add(pos);
		for(Direction dir : Direction.values())
			out.add(pos.relative(dir));
		return out;
	}

	private static WireBundleBlockEntity bundle(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof WireBundleBlock && level.getBlockEntity(pos) instanceof WireBundleBlockEntity entity
				&& entity.strands().size() > 1 ? entity : null;
	}

	/** Whether a strand of the bundle at {@code pos} carries on through its {@code dir} face into the bundle there. */
	private static boolean linked(Level level, BlockPos pos, Direction dir, List<BlockState> strands) {
		BlockPos next = pos.relative(dir);
		for(BlockState strand : strands)
			if(strand.getValue(WireBlock.PROPERTY_BY_DIRECTION.get(dir)) && ((WireBlock)strand.getBlock()).sameKindState(level, next) != null)
				return true;
		return false;
	}

	/**
	 * Which way a run up or down through {@code dir} carries across: up a north or south wall the strands lie along
	 * x (1), up an east or west wall along z (2), up the middle both (3). Only that way is shared with the block
	 * beyond; the other is free to turn at either end (a run up a wall from a north-south run, onto an east-west one).
	 */
	private static int verticalMask(Level level, BlockPos pos, Direction dir, List<BlockState> strands) {
		BlockPos next = pos.relative(dir);
		for(BlockState strand : strands) {
			WireBlock wire = (WireBlock)strand.getBlock();
			if(!strand.getValue(WireBlock.PROPERTY_BY_DIRECTION.get(dir)) || wire.sameKindState(level, next) == null)
				continue;
			Direction hug = strand.getValue(WireBlock.HUG).direction();
			if(hug == null)
				return 3;
			return hug.getAxis() == Direction.Axis.Z ? 1 : 2;
		}
		return 3;
	}

	/**
	 * The bend in a bundle, by its strands' vote: 0 if its x and z layouts run the same way (a bend between north
	 * and west, or south and east), 1 if opposite (north and east, south and west), -1 for no bend. A strand's ends are
	 * the sides it leaves by on the level: its links, and the wall it runs up or arrives up.
	 */
	static int bend(Level level, BlockPos pos, List<BlockState> strands) {
		int same = 0, opposite = 0;
		for(BlockState strand : strands) {
			WireBlock wire = (WireBlock)strand.getBlock();
			Set<Direction> ends = new HashSet<>();
			for(Direction dir : Direction.Plane.HORIZONTAL)
				if(strand.getValue(WireBlock.PROPERTY_BY_DIRECTION.get(dir)))
					ends.add(dir);
			WireLook.Look look = wire.look(level, pos, strand);
			if(look.hug() != null)
				ends.add(look.hug());
			if(look.arrive() != null)
				ends.add(look.arrive());
			Direction ns = null, ew = null;
			int nsCount = 0, ewCount = 0;
			for(Direction dir : ends) {
				if(dir.getAxis() == Direction.Axis.Z) {
					ns = dir;
					nsCount++;
				}
				else {
					ew = dir;
					ewCount++;
				}
			}
			if(nsCount != 1 || ewCount != 1)
				continue; // Straight, a T, a cross, or no turn on the level: no say.
			if((ns == Direction.NORTH) == (ew == Direction.WEST))
				same++;
			else
				opposite++;
		}
		if(same == 0 && opposite == 0)
			return -1;
		return opposite > same ? 1 : 0;
	}

	/**
	 * Each strand that turns at the bundle at {@code pos} (one north-south end, one east-west) and does not sit in
	 * the middle votes, on each axis, for the way round that puts it on the side it turns towards.
	 */
	private static void vote(Level level, BlockPos pos, List<BlockState> strands, int[] votes, int node) {
		double[] offsets = Strand.offsets(strands);
		for(int k = 0; k < strands.size(); k++) {
			double o = offsets[k];
			if(o == 0.0D)
				continue;
			BlockState strand = strands.get(k);
			WireLook.Look look = ((WireBlock)strand.getBlock()).look(level, pos, strand);
			Set<Direction> ends = new HashSet<>();
			for(Direction dir : Direction.Plane.HORIZONTAL)
				if(strand.getValue(WireBlock.PROPERTY_BY_DIRECTION.get(dir)))
					ends.add(dir);
			if(look.hug() != null)
				ends.add(look.hug());
			if(look.arrive() != null)
				ends.add(look.arrive());
			Direction ns = null, ew = null;
			int nsCount = 0, ewCount = 0;
			for(Direction dir : ends) {
				if(dir.getAxis() == Direction.Axis.Z) {
					ns = dir;
					nsCount++;
				}
				else {
					ew = dir;
					ewCount++;
				}
			}
			if(nsCount != 1 || ewCount != 1)
				continue;
			// Across a north-south run (x) it wants to be on its east-west side; across an east-west run (z), on its
			// north-south side. Its place there is o, or -o turned round: turned round if o points the other way.
			// An outer strand turning off crosses more strands than an inner one: it weighs more (its distance out).
			int weight = (int)Math.round(Math.abs(o) * 2.0D);
			votes[2 * node] += Math.signum(o) * ew.getStepX() < 0 ? weight : -weight;
			votes[2 * node + 1] += Math.signum(o) * ns.getStepZ() < 0 ? weight : -weight;
		}
	}

	/** Solves the run of bundles that {@code start} is in, and syncs any bundle whose turns changed. */
	static void solve(Level level, BlockPos start, Set<BlockPos> done) {
		// The run: bundles linked to each other through their faces.
		Map<BlockPos, Integer> index = new LinkedHashMap<>();
		List<BlockPos> nodes = new ArrayList<>();
		List<int[]> links = new ArrayList<>(); // {a, b, axis mask: 1 x, 2 z}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		index.put(start, 0);
		nodes.add(start);
		queue.add(start);
		while(!queue.isEmpty() && nodes.size() < MAX_BUNDLES) {
			BlockPos pos = queue.poll();
			WireBundleBlockEntity here = bundle(level, pos);
			if(here == null)
				continue;
			List<BlockState> strands = here.strands();
			for(Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if(bundle(level, next) == null || !linked(level, pos, dir, strands))
					continue;
				Integer other = index.get(next);
				if(other == null) {
					other = nodes.size();
					index.put(next, other);
					nodes.add(next);
					queue.add(next);
				}
				int mask = dir.getAxis() == Direction.Axis.Z ? 1 : dir.getAxis() == Direction.Axis.X ? 2 : verticalMask(level, pos, dir, strands);
				links.add(new int[] { index.get(pos), other, mask });
			}
			// Climbs: a strand that runs up a wall onto the block on top of it links diagonally. The run up the wall
			// lies along it (x up a north or south wall, z up an east or west one), so that way carries across.
			for(BlockState strand : strands)
				for(BlockPos next : ((WireBlock)strand.getBlock()).getLinks(level, pos, strand).keySet()) {
					int dx = next.getX() - pos.getX(), dy = next.getY() - pos.getY(), dz = next.getZ() - pos.getZ();
					if(dy == 0 || (dx == 0 && dz == 0) || bundle(level, next) == null)
						continue; // A face link (above), or not a climb to a bundle.
					Integer other = index.get(next);
					if(other == null) {
						other = nodes.size();
						index.put(next.immutable(), other);
						nodes.add(next.immutable());
						queue.add(next.immutable());
					}
					links.add(new int[] { index.get(pos), other, dz != 0 ? 1 : 2 });
				}
		}
		done.addAll(nodes);

		// Variables: 2n is a bundle's x way, 2n + 1 its z way. Shared ways first (they always hold), then the bends.
		Parity uf = new Parity(nodes.size() * 2);
		for(int[] link : links) {
			if((link[2] & 1) != 0)
				uf.union(2 * link[0], 2 * link[1], 0);
			if((link[2] & 2) != 0)
				uf.union(2 * link[0] + 1, 2 * link[1] + 1, 0);
		}
		for(int i = 0; i < nodes.size(); i++) {
			WireBundleBlockEntity bundle = bundle(level, nodes.get(i));
			int bend = bundle == null ? -1 : bend(level, nodes.get(i), bundle.strands());
			if(bend >= 0 && !uf.union(2 * i, 2 * i + 1, bend)) // Refused (an odd loop): that bend crosses.
				Radiocraft.LOGGER.debug("Bundle bend at {} cannot be kept (a loop of bends): its strands cross there", nodes.get(i));
		}

		// What is left free is chosen by the strands that turn off a run (a split): each would sit on the side of the
		// run it turns towards, so it leaves without crossing the strands that carry on. Strands that turn together
		// (a bend) vote both ways and cancel out. A tie keeps the turn the group's first bundle already had.
		int[] votes = new int[nodes.size() * 2]; // > 0: wants turned round.
		for(int i = 0; i < nodes.size(); i++) {
			WireBundleBlockEntity bundle = bundle(level, nodes.get(i));
			if(bundle != null)
				vote(level, nodes.get(i), bundle.strands(), votes, i);
		}
		Map<Integer, Integer> rootVotes = new HashMap<>();
		for(int v = 0; v < votes.length; v++)
			rootVotes.merge(uf.find(v), uf.parity(v) == 0 ? votes[v] : -votes[v], Integer::sum);
		Map<Integer, Boolean> rootValue = new LinkedHashMap<>();
		boolean[] wasX = new boolean[nodes.size()], wasZ = new boolean[nodes.size()];
		for(int i = 0; i < nodes.size(); i++) {
			WireBundleBlockEntity bundle = bundle(level, nodes.get(i));
			wasX[i] = bundle != null && bundle.flipX();
			wasZ[i] = bundle != null && bundle.flipZ();
		}
		for(int v = 0; v < nodes.size() * 2; v++) {
			int root = uf.find(v);
			if(rootValue.containsKey(root))
				continue;
			int tally = rootVotes.getOrDefault(root, 0);
			boolean current = v % 2 == 0 ? wasX[v / 2] : wasZ[v / 2];
			rootValue.put(root, tally != 0 ? tally > 0 : current ^ (uf.parity(v) == 1));
		}

		List<List<BlockState>> wasOrder = new ArrayList<>();
		java.util.TreeSet<WireBlock> present = new java.util.TreeSet<>(java.util.Comparator.comparingInt(WireBundleBlock::rank)
				.thenComparing(k -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(k).toString()));
		for(BlockPos pos : nodes) {
			WireBundleBlockEntity bundle = bundle(level, pos);
			wasOrder.add(bundle == null ? List.of() : bundle.strands());
			if(bundle != null)
				for(BlockState strand : bundle.strands())
					present.add((WireBlock)strand.getBlock());
		}

		// The votes are a guess (a strand that comes up a wall and leaves across the block does not vote, say). Check
		// it: each free choice turned the other way, kept if fewer strands then run through each other. Then the order
		// of the colours across the run, from their rank: one colour moved at a time, kept if fewer then cross (where
		// strands turn off in an order no turning round gives, as down a row of radios).
		apply(level, nodes, uf, rootValue);
		if(nodes.size() <= MAX_CHECKED) {
			List<WireBlock> order = new ArrayList<>(present);
			boolean ordering = nodes.size() <= MAX_ORDERED && order.size() <= MAX_ORDERED_KINDS && order.size() > 1;
			if(ordering)
				setOrder(level, nodes, order);
			int best = settle(level, nodes, uf, rootValue);
			for(int pass = 0; ordering && best > 0 && pass < 4; pass++) {
				boolean improved = false;
				for(int from = 0; from < order.size() && best > 0; from++)
					for(int to = 0; to < order.size() && best > 0; to++) {
						if(to == from)
							continue;
						List<WireBlock> trial = new ArrayList<>(order);
						trial.add(to, trial.remove(from));
						Map<Integer, Boolean> trialValue = new LinkedHashMap<>(rootValue);
						setOrder(level, nodes, trial);
						int count = settle(level, nodes, uf, trialValue);
						if(count < best) {
							best = count;
							order = trial;
							rootValue = trialValue;
							improved = true;
						}
						else {
							setOrder(level, nodes, order);
							apply(level, nodes, uf, rootValue);
						}
					}
				if(!improved)
					break;
			}
		}
		for(int i = 0; i < nodes.size(); i++) {
			WireBundleBlockEntity bundle = bundle(level, nodes.get(i));
			if(bundle != null && (bundle.flipX() != wasX[i] || bundle.flipZ() != wasZ[i] || !bundle.strands().equals(wasOrder.get(i)))) {
				BlockState state = level.getBlockState(nodes.get(i));
				level.sendBlockUpdated(nodes.get(i), state, state, Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Most bundles in a run whose turns are checked by counting crossings ({@link WireGeometry#crossings}). */
	static final int MAX_CHECKED = 512;
	/** Most bundles, and most kinds of cable, in a run whose colour order is searched too. */
	static final int MAX_ORDERED = 128, MAX_ORDERED_KINDS = 8;

	/**
	 * From the choices in {@code rootValue}, turns each free choice the other way where that makes fewer strands run
	 * through each other, updating {@code rootValue}; leaves the best applied and returns its count.
	 */
	private static int settle(Level level, List<BlockPos> nodes, Parity uf, Map<Integer, Boolean> rootValue) {
		apply(level, nodes, uf, rootValue);
		int best = WireGeometry.crossings(level, nodes);
		for(Integer root : List.copyOf(rootValue.keySet())) {
			if(best == 0)
				break;
			rootValue.put(root, !rootValue.get(root));
			apply(level, nodes, uf, rootValue);
			int count = WireGeometry.crossings(level, nodes);
			if(count < best)
				best = count;
			else {
				rootValue.put(root, !rootValue.get(root));
				apply(level, nodes, uf, rootValue);
			}
		}
		return best;
	}

	/** Puts every bundle's strands in {@code order} ({@link WireBundleBlockEntity#setOrder}). */
	private static void setOrder(Level level, List<BlockPos> nodes, List<WireBlock> order) {
		for(BlockPos pos : nodes) {
			WireBundleBlockEntity bundle = bundle(level, pos);
			if(bundle != null)
				bundle.setOrder(order);
		}
	}

	/** Sets every bundle's turns from its group's choice (server side; synced once the solve is done). */
	private static void apply(Level level, List<BlockPos> nodes, Parity uf, Map<Integer, Boolean> rootValue) {
		for(int i = 0; i < nodes.size(); i++) {
			WireBundleBlockEntity bundle = bundle(level, nodes.get(i));
			if(bundle == null)
				continue;
			boolean x = rootValue.get(uf.find(2 * i)) ^ (uf.parity(2 * i) == 1);
			boolean z = rootValue.get(uf.find(2 * i + 1)) ^ (uf.parity(2 * i + 1) == 1);
			bundle.setFlips(x, z);
		}
	}

	/** Union-find where each member knows whether it is the same as (0) or opposite to (1) its group's root. */
	static final class Parity {
		private final int[] parent, rank, parity;

		Parity(int size) {
			parent = new int[size];
			rank = new int[size];
			parity = new int[size];
			for(int i = 0; i < size; i++)
				parent[i] = i;
		}

		int find(int v) {
			if(parent[v] == v)
				return v;
			int p = parent[v];
			int root = find(p);
			parity[v] ^= parity[p]; // p's parity is now relative to the root.
			parent[v] = root;
			return root;
		}

		/** v's parity relative to its root. */
		int parity(int v) {
			find(v);
			return parity[v];
		}

		/** Ties a and b: the same (0) or opposite (1). False, and nothing changed, if that contradicts what is known. */
		boolean union(int a, int b, int relation) {
			int ra = find(a), rb = find(b), pa = parity[a], pb = parity[b];
			if(ra == rb)
				return (pa ^ pb) == relation;
			if(rank[ra] < rank[rb]) {
				int t = ra; ra = rb; rb = t;
			}
			parent[rb] = ra;
			parity[rb] = pa ^ pb ^ relation;
			if(rank[ra] == rank[rb])
				rank[ra]++;
			return true;
		}
	}
}

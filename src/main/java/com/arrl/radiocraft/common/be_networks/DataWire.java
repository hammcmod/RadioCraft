package com.arrl.radiocraft.common.be_networks;

import com.arrl.radiocraft.common.blocks.Strand;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * The data wire (maintainer, 2026-10-30): the line between a radio and its accessories, wired speakers, and the stations
 * a microwave radio serves, when they are not side by side. It carries no power and builds no network: the devices a
 * block reaches are found by walking the wire when asked, and kept for {@link #CACHE_TICKS} or until a data wire
 * changes anywhere in the level. Touching still links everything it always did; the wire only adds reach.
 */
public final class DataWire {

	/** How long a walk's answer is kept, in ticks (a device placed at the far end is noticed within this). */
	public static final int CACHE_TICKS = 40;
	/** The most strands one walk follows, so a huge web of wire cannot stall a tick. */
	public static final int MAX_STRANDS = 4096;

	private record Entry(long tick, long generation, List<BlockPos> reached) {}

	private static final Map<Level, Map<BlockPos, Entry>> CACHE = new WeakHashMap<>();
	private static final Map<Level, Long> GENERATION = new WeakHashMap<>();

	private DataWire() {}

	/** The wire blocks the walk runs through: the data wire, and the passthrough (which every kind runs through). */
	public static Set<WireBlock> wires() {
		return Set.of(RadiocraftBlocks.DATA_WIRE.get(), RadiocraftBlocks.PASSTHROUGH.get());
	}

	/** A data wire was laid, lifted or changed in {@code level}: every cached walk there is stale. */
	public static synchronized void changed(Level level) {
		GENERATION.merge(level, 1L, Long::sum);
	}

	/** Every device the data wire touching {@code pos} reaches, not counting {@code pos} itself. */
	public static synchronized List<BlockPos> reached(Level level, BlockPos pos) {
		long generation = GENERATION.getOrDefault(level, 0L);
		long now = level.getGameTime();
		Map<BlockPos, Entry> cache = CACHE.computeIfAbsent(level, l -> new HashMap<>());
		Entry entry = cache.get(pos);
		if(entry != null && entry.generation() == generation && now - entry.tick() < CACHE_TICKS && now >= entry.tick())
			return entry.reached();
		List<BlockPos> reached = walk(level, pos);
		if(cache.size() > 4096)
			cache.clear(); // Positions of devices long gone pile up otherwise.
		cache.put(pos.immutable(), new Entry(now, generation, reached));
		return reached;
	}

	/** The blocks {@code pos} is linked to: its six neighbours, then everything its data wire reaches. */
	public static List<BlockPos> linked(Level level, BlockPos pos) {
		List<BlockPos> out = new ArrayList<>(6);
		for(Direction direction : Direction.values())
			out.add(pos.relative(direction));
		for(BlockPos at : reached(level, pos))
			if(!out.contains(at))
				out.add(at);
		return out;
	}

	/** The loaded block entities of {@code type} linked to {@code pos} ({@link #linked}), nearest first. */
	public static <T> List<T> linked(Level level, BlockPos pos, Class<T> type) {
		List<T> out = new ArrayList<>();
		for(BlockPos at : linked(level, pos))
			if(level.isLoaded(at) && type.isInstance(level.getBlockEntity(at))) {
				T found = type.cast(level.getBlockEntity(at));
				if(!out.contains(found))
					out.add(found);
			}
		return out;
	}

	/** The loaded block entities of {@code type} reached over data wire only (not touching). */
	public static <T> List<T> wired(Level level, BlockPos pos, Class<T> type) {
		List<T> out = new ArrayList<>();
		for(BlockPos at : reached(level, pos)) {
			BlockEntity be = level.isLoaded(at) ? level.getBlockEntity(at) : null;
			if(type.isInstance(be) && !out.contains(type.cast(be)))
				out.add(type.cast(be));
		}
		return out;
	}

	private static List<BlockPos> walk(Level level, BlockPos start) {
		Set<WireBlock> wires = wires();
		Set<Strand> seen = new HashSet<>();
		Set<BlockPos> devices = new LinkedHashSet<>();
		ArrayDeque<Strand> queue = new ArrayDeque<>();
		for(BlockPos wirePos : WireUtils.wireLinksOf(level, start, wires).keySet())
			enter(level, wirePos, null, wires, seen, queue);
		while(!queue.isEmpty() && seen.size() <= MAX_STRANDS) {
			Strand strand = queue.poll();
			WireBlock kind = strand.kind();
			BlockState state = strand.state(level);
			if(kind == null || state == null)
				continue;
			for(Map.Entry<BlockPos, Direction> link : kind.getLinks(level, strand.pos(), state).entrySet()) {
				BlockPos target = link.getKey();
				if(!level.isLoaded(target))
					continue;
				if(!Strand.at(level, target).isEmpty()) {
					enter(level, target, kind, wires, seen, queue);
					continue;
				}
				BlockPos device = WireUtils.linkTarget(level, target, link.getValue()).immutable();
				if(!device.equals(start))
					devices.add(device);
			}
		}
		return List.copyOf(devices);
	}

	private static void enter(Level level, BlockPos pos, WireBlock from, Set<WireBlock> wires, Set<Strand> seen, ArrayDeque<Strand> queue) {
		for(Strand strand : Strand.at(level, pos))
			if(strand.kind() != null && wires.contains(strand.kind()) && (from == null || from.joins(strand.kind())) && seen.add(strand))
				queue.add(strand);
	}
}

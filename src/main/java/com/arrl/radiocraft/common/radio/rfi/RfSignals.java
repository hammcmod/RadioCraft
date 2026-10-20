package com.arrl.radiocraft.common.radio.rfi;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redstone levels that transmitters are currently inducing in dust. Redstone dust works its power out from its
 * neighbours on every update, so a level written into its block state does not survive; instead
 * {@code MixinRedStoneWireBlock} asks here when dust recalculates and the dust settles at the larger of the real and
 * the induced level. If that mixin fails to apply (a Minecraft update renaming the method) this registry is simply
 * never read and dust stops reacting, while everything else about redstone interference keeps working.
 * <p>
 * The fast path for the mixin is {@link #ANY}: one volatile read while no station is transmitting near redstone.
 */
public final class RfSignals {

	private static final Map<ResourceKey<Level>, Map<Long, Integer>> LEVELS = new ConcurrentHashMap<>();
	private static volatile boolean ANY = false;

	private RfSignals() {}

	/** The induced level at {@code pos}, 0 when none. Called from inside dust's own power calculation. */
	public static int get(Level level, BlockPos pos) {
		if(!ANY)
			return 0;
		Map<Long, Integer> levels = LEVELS.get(level.dimension());
		if(levels == null)
			return 0;
		Integer value = levels.get(pos.asLong());
		return value == null ? 0 : value;
	}

	/** Starts inducing {@code levels} and makes the affected dust recalculate right away. */
	public static void apply(Level level, Map<BlockPos, Integer> levels) {
		if(levels.isEmpty())
			return;
		Map<Long, Integer> map = LEVELS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
		for(Map.Entry<BlockPos, Integer> entry : levels.entrySet())
			map.merge(entry.getKey().asLong(), entry.getValue(), Math::max);
		ANY = true;
		for(BlockPos pos : levels.keySet())
			refreshDust(level, pos);
	}

	/** Stops inducing at {@code positions}; the dust there recalculates back to whatever really drives it. */
	public static void remove(Level level, Collection<BlockPos> positions) {
		Map<Long, Integer> map = LEVELS.get(level.dimension());
		if(map == null)
			return;
		for(BlockPos pos : positions)
			map.remove(pos.asLong());
		if(map.isEmpty())
			LEVELS.remove(level.dimension());
		ANY = !LEVELS.isEmpty();
		for(BlockPos pos : positions)
			refreshDust(level, pos);
	}

	public static void clear(Level level) {
		LEVELS.remove(level.dimension());
		ANY = !LEVELS.isEmpty();
	}

	/** Runs the dust's own neighbour-changed logic, which recomputes its power (through the mixin) and tells its neighbours. */
	private static void refreshDust(Level level, BlockPos pos) {
		if(!level.isLoaded(pos))
			return;
		BlockState state = level.getBlockState(pos);
		if(state.is(Blocks.REDSTONE_WIRE))
			state.handleNeighborChanged(level, pos, Blocks.REDSTONE_WIRE, pos, false);
	}
}

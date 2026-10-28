package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.WireBundleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One cable at a position: a kind of wire (power wire, waterproof wire, plain or dyed coax, or the passthrough) at
 * {@code pos}. A {@link WireBlock} is one strand; a {@link WireBundleBlock} holds up to {@link WireBundleBlock#MAX_STRANDS}
 * of them, each with the state (connections, hugged edge, climb) it would have as a wire block of its own there. The
 * network walk ({@code WireUtils}) moves between strands, so two colours of coax in one bundle stay as far apart as
 * they would in two blocks. A {@code null} kind stands for a device at {@code pos} in the walk's visited set.
 */
public record Strand(BlockPos pos, @Nullable WireBlock kind) {

	/** The walk's marker for a device (network object) at {@code pos}. */
	public static Strand device(BlockPos pos) {
		return new Strand(pos.immutable(), null);
	}

	public Strand {
		pos = pos.immutable();
	}

	/** The strands at {@code pos}: a wire block is itself, a bundle its cables, anything else none. */
	public static List<Strand> at(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() instanceof WireBlock wire)
			return List.of(new Strand(pos, wire));
		if(state.getBlock() instanceof WireBundleBlock && level.getBlockEntity(pos) instanceof WireBundleBlockEntity bundle)
			return bundle.strands().stream().map(s -> new Strand(pos, (WireBlock)s.getBlock())).toList();
		return List.of();
	}

	/** The strand states at {@code pos} (see {@link #at}): each a state of its kind's block. */
	public static List<BlockState> statesAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() instanceof WireBlock)
			return List.of(state);
		if(state.getBlock() instanceof WireBundleBlock && level.getBlockEntity(pos) instanceof WireBundleBlockEntity bundle)
			return bundle.strands();
		return List.of();
	}

	/** The state of the strand of exactly {@code kind} at {@code pos}, or null if there is none. */
	@Nullable
	public static BlockState stateOf(BlockGetter level, BlockPos pos, WireBlock kind) {
		BlockState state = level.getBlockState(pos);
		if(state.getBlock() == kind)
			return state;
		if(state.getBlock() instanceof WireBundleBlock && level.getBlockEntity(pos) instanceof WireBundleBlockEntity bundle)
			return bundle.strand(kind);
		return null;
	}

	/** The state of this strand, or null if it is gone. */
	@Nullable
	public BlockState state(BlockGetter level) {
		return kind == null ? null : stateOf(level, pos, kind);
	}

	/** True if there is a cable of any kind at {@code pos} (a wire block or a bundle). */
	public static boolean isCable(BlockState state) {
		return state.getBlock() instanceof WireBlock || state.getBlock() instanceof WireBundleBlock;
	}

	/** True if the cable at {@code pos} hangs from the ceiling (a bundle's strands all hang together). */
	public static boolean isCeilingAt(BlockGetter level, BlockPos pos) {
		List<BlockState> states = statesAt(level, pos);
		return !states.isEmpty() && WireBlock.isCeiling(states.getFirst());
	}

	/**
	 * Where the strands of a bundle are drawn relative to a lone wire, in sixteenths of a block, for the strand states
	 * in display order: side by side with a pixel between, centred ({@link WireLook#layout}), so one cable sits where
	 * it always did and two sit either side of the middle.
	 */
	public static double[] offsets(List<BlockState> strands) {
		double[] widths = new double[strands.size()];
		for(int i = 0; i < widths.length; i++)
			widths[i] = ((WireBlock)strands.get(i).getBlock()).width();
		return WireLook.layout(widths);
	}

	/**
	 * Where the cable {@code kind} would meet at {@code pos} sits laterally (see {@link #offsets}): a lone wire or a
	 * device at 0, a bundle's strand of that kind (or of a kind it runs on as) at its place in the bundle.
	 */
	public static double offsetOf(BlockGetter level, BlockPos pos, WireBlock kind) {
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof WireBundleBlock) || !(level.getBlockEntity(pos) instanceof WireBundleBlockEntity bundle))
			return 0.0D;
		List<BlockState> strands = bundle.strands();
		double[] offsets = offsets(strands);
		int fallback = -1;
		for(int i = 0; i < strands.size(); i++) {
			WireBlock there = (WireBlock)strands.get(i).getBlock();
			if(there == kind)
				return offsets[i];
			if(fallback < 0 && kind.isSameKind(there))
				fallback = i;
		}
		return fallback < 0 ? 0.0D : offsets[fallback];
	}
}

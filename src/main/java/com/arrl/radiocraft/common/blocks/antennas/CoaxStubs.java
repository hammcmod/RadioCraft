package com.arrl.radiocraft.common.blocks.antennas;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Coax laid on the ground beside an antenna's foot shows a run in to its mast (model {@code block/antenna_coax}): one
 * property per side, absolute (not turned with FACING), true while a coax wire is on that side. The Ionosonde Antenna
 * and the 5-BTV use it on their bottom block.
 */
public final class CoaxStubs {

	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;

	private CoaxStubs() {}

	public static BooleanProperty side(Direction direction) {
		return switch(direction) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> throw new IllegalArgumentException("No coax run from " + direction);
		};
	}

	/** {@code state} with no coax runs, for the default state. */
	public static BlockState none(BlockState state) {
		return state.setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false);
	}

	/** {@code state} with a run from each side that has coax on it, for placing. */
	public static BlockState around(BlockState state, LevelReader level, BlockPos pos) {
		for(Direction direction : Direction.Plane.HORIZONTAL)
			state = state.setValue(side(direction), isCoax(level.getBlockState(pos.relative(direction))));
		return state;
	}

	/** {@code state} after the block on {@code direction} became {@code neighbour}, for updateShape. */
	public static BlockState update(BlockState state, Direction direction, BlockState neighbour) {
		return direction.getAxis().isHorizontal() ? state.setValue(side(direction), isCoax(neighbour)) : state;
	}

	public static boolean isCoax(BlockState state) {
		return com.arrl.radiocraft.common.be_networks.WireUtils.isCoax(state); // Any colour.
	}
}

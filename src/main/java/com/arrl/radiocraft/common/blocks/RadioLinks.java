package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Predicate;

/**
 * The cables a radio accessory shows along the floor to the blocks it works with beside it: a radio interface to its
 * radios and terminals, a terminal to the bus, a teletype to its radio (the desk microphone stands against its radio
 * instead, see MicrophoneBlock). One property per side, absolute (not turned with FACING); the blockstate draws a short
 * cable from that side in to the middle (model {@code block/radio_link}). Both ends of a link draw their half, so two accessories side by side show one cable; a
 * radio does not draw, so the cable runs up to it. A radio hung from a ceiling gets no floor cable.
 */
public final class RadioLinks {

	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;

	/** A radio standing on the floor. */
	public static final Predicate<BlockState> RADIO = state -> state.getBlock() instanceof RadioBlock && !state.getValue(RadioBlock.CEILING);
	/** A block on the station bus: a radio interface or one of its terminals. */
	public static final Predicate<BlockState> STATION = state -> state.getBlock() instanceof RadioInterfaceBlock
			|| state.getBlock() instanceof AprsTerminalBlock || state.getBlock() instanceof RadioFaxBlock;

	private RadioLinks() {}

	public static BooleanProperty side(Direction direction) {
		return switch(direction) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> throw new IllegalArgumentException("No floor cable " + direction);
		};
	}

	public static void addTo(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST);
	}

	/** {@code state} with no cables, for the default state. */
	public static BlockState none(BlockState state) {
		return state.setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false);
	}

	/** {@code state} with a cable to each side that has a block {@code linksTo} accepts, for placing. */
	public static BlockState around(BlockState state, LevelReader level, BlockPos pos, Predicate<BlockState> linksTo) {
		for(Direction direction : Direction.Plane.HORIZONTAL)
			state = state.setValue(side(direction), linksTo.test(level.getBlockState(pos.relative(direction))));
		return state;
	}

	/** {@code state} after the block on {@code direction} became {@code neighbour}, for updateShape. */
	public static BlockState update(BlockState state, Direction direction, BlockState neighbour, Predicate<BlockState> linksTo) {
		if(!direction.getAxis().isHorizontal() || !state.hasProperty(NORTH))
			return state;
		return state.setValue(side(direction), linksTo.test(neighbour));
	}
}

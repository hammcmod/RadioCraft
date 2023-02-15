package com.arrl.radiocraft.common.power;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PowerUtils {

	/**
	 * Finds the power network for a specific wire.
	 * @param level level to search in.
	 * @param pos position of a wire block.
	 * @return First network found.
	 */
	public static PowerNetwork findNetwork(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);

		if(state.getBlock() != RadiocraftBlocks.WIRE.get()) // Not valid if there is no wire here.
			return null;

		// TODO: Implement network find function after the wire blockstates-- makes it easier to shortlist the blocks needed to be checked.

		return null;
	}

}

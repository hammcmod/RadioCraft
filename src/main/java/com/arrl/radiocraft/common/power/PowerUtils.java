package com.arrl.radiocraft.common.power;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUtils {

	/**
	 * Finds power networks connections for a specific wire.
	 * @param level level to search in.
	 * @param pos position of a wire block.
	 */
	public static Map<IPowerNetworkItem, Direction> findNetworkConnections(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);

		if(state.getBlock() != RadiocraftBlocks.WIRE.get()) // Not valid if there is no wire here.
			return null;

		// TODO: Implement network find function after the wire blockstates-- makes it easier to shortlist the blocks needed to be checked.#

		List<BlockPos> positionsChecked = new ArrayList<>();
		Map<IPowerNetworkItem, Direction> connections = new HashMap<>();

		getConnections(level, pos, connections, positionsChecked);

		for(IPowerNetworkItem item : connections.keySet())
			Radiocraft.LOGGER.info(((BlockEntity)item).getBlockPos().toShortString());

		return connections;
	}

	/**
	 * Recursively checks for all connections to a wire network
	 */
	private static void getConnections(Level level, BlockPos pos, Map<IPowerNetworkItem, Direction> connections, List<BlockPos> blackList) {
		blackList.add(pos); // Check pos being blacklisted to stop infinite loops

		for(Direction direction : Plane.HORIZONTAL) {
			List<BlockPos> wireConnections = RadiocraftBlocks.WIRE.get().getConnections(level, pos, direction);

			for(BlockPos checkPos : wireConnections) {
				if(blackList.contains(checkPos))
					continue;

				BlockState state = level.getBlockState(checkPos);
				if(state.is(RadiocraftBlocks.WIRE.get())) {
					getConnections(level, checkPos, connections, blackList); // Additional wires will get checked themselves
				}
				else if(level.getBlockEntity(checkPos) instanceof IPowerNetworkItem networkItem) {
					connections.put(networkItem, direction.getOpposite()); // Network items will be added to connections and blacklisted
					blackList.add(checkPos);
				}

			}

		}
	}

}

package com.arrl.radiocraft.common.power;

import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUtils {

	/**
	 * Finds power networks connections for a specific wire.
	 */
	public static Map<IPowerNetworkItem, Direction> findNetworkConnections(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);

		if(state.getBlock() != RadiocraftBlocks.WIRE.get()) // Not valid if there is no wire here.
			return null;

		// TODO: Implement network find function after the wire blockstates-- makes it easier to shortlist the blocks needed to be checked.

		List<BlockPos> positionsChecked = new ArrayList<>();
		Map<IPowerNetworkItem, Direction> connections = new HashMap<>();

		getConnections(level, pos, connections, positionsChecked);

		return connections;
	}

	/**
	 * Merges all connections on a wire and/or creates a new one
	 */
	public static void mergeWireNetworks(Level level, BlockPos pos) {
		Map<IPowerNetworkItem, Direction> connections = findNetworkConnections(level, pos);

		if(connections.size() > 0) {

			List<PowerNetwork> existingNetworks = new ArrayList<>();
			List<IPowerNetworkItem> nullItems = new ArrayList<>(); // These network items don't already have a network.

			for(IPowerNetworkItem networkItem : connections.keySet()) {
				PowerNetwork network = networkItem.getNetwork(connections.get(networkItem));
				if(network != null && !existingNetworks.contains(network))
					existingNetworks.add(network);
				else
					nullItems.add(networkItem);
			}

			PowerNetwork newNetwork = PowerNetwork.merge(existingNetworks.toArray(new PowerNetwork[0]));
			nullItems.forEach(networkItem -> {
				Direction direction = connections.get(networkItem);
				networkItem.setNetwork(direction, newNetwork);
				newNetwork.addConnection(networkItem, networkItem.getDefaultConnectionType(), direction);
			}); // Add new network to any null ones and add null items to the network connections
		}
	}

	/**
	 * Splits the network associated with a given wire block.
	 */
	public static void splitWireNetwork(Level level, BlockPos pos) {
		List<BlockPos> positionsChecked = new ArrayList<>();
		positionsChecked.add(pos);

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos startPos = pos.relative(direction);
			Map<IPowerNetworkItem, Direction> connections = getConnections(level, startPos, new HashMap<>(), positionsChecked);
			connections.keySet().stream().findFirst().ifPresent(
					item -> PowerNetwork.split(item.getNetwork(connections.get(item)), connections.keySet())
			); // Split this wire's network by all connected devices found.
		}
	}

	/**
	 * Recursively checks for all connections to a wire block
	 */
	private static Map<IPowerNetworkItem, Direction> getConnections(Level level, BlockPos pos, Map<IPowerNetworkItem, Direction> connections, List<BlockPos> blackList) {
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
		return connections;
	}

}

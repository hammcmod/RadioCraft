package com.arrl.radiocraft.common.power;

import com.arrl.radiocraft.common.blocks.WireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUtils {

	/**
	 * Finds power networks connections for a specific wire.
	 */
	public static Map<IPowerNetworkItem, Direction> findWireNetworkConnections(BlockGetter level, BlockPos pos) {
		List<BlockPos> positionsChecked = new ArrayList<>();
		Map<IPowerNetworkItem, Direction> connections = new HashMap<>();

		getConnections(level, pos, connections, positionsChecked);

		return connections;
	}

	/**
	 * Merges all connections on a wire and/or creates a new one
	 */
	public static void mergeWireNetworks(BlockGetter level, BlockPos pos) {
		Map<IPowerNetworkItem, Direction> connections = findWireNetworkConnections(level, pos);

		if(connections.size() > 0) {

			List<PowerNetwork> existingNetworks = new ArrayList<>();
			List<IPowerNetworkItem> nullItems = new ArrayList<>(); // These network items don't already have a network.

			for(IPowerNetworkItem networkItem : connections.keySet()) {
				PowerNetwork network = networkItem.getNetwork(connections.get(networkItem));
				if(network != null) {
					if(!existingNetworks.contains(network))
						existingNetworks.add(network);
				}
				else
					nullItems.add(networkItem);
			}

			PowerNetwork newNetwork = PowerNetwork.merge(existingNetworks.toArray(new PowerNetwork[0]));
			nullItems.forEach(networkItem -> { // Shouldn't have any nulls but just in case
				Direction direction = connections.get(networkItem);
				networkItem.setNetwork(direction, newNetwork);
				newNetwork.addConnection(networkItem);
			});
		}
	}

	/**
	 * Splits the network associated with a given wire block.
	 */
	public static void splitWireNetwork(BlockGetter level, BlockPos pos) {
		List<BlockPos> positionsChecked = new ArrayList<>();
		positionsChecked.add(pos);

		for(Direction direction : Direction.values()) {
			BlockPos startPos = pos.relative(direction);

			Map<IPowerNetworkItem, Direction> connectionsMap = new HashMap<>();

			if(level.getBlockEntity(startPos) instanceof IPowerNetworkItem networkItem)
				connectionsMap.put(networkItem, direction.getOpposite()); // If already at endpoint, add item

			Map<IPowerNetworkItem, Direction> connections = getConnections(level, startPos, connectionsMap, positionsChecked);
			connections.keySet().stream().findFirst().ifPresent(item -> {
				PowerNetwork network = item.getNetwork(connections.get(item));
				if(network != null)
					network.split(connections.keySet());
			}
			); // Split this wire's network by all connected devices found.
		}
	}

	/**
	 * Recursively checks for all connections to a wire block
	 */
	private static Map<IPowerNetworkItem, Direction> getConnections(BlockGetter level, BlockPos pos, Map<IPowerNetworkItem, Direction> connections, List<BlockPos> blackList) {
		if(!WireBlock.isWire(level.getBlockState(pos)))
			 return connections;
		blackList.add(pos); // Check pos being blacklisted to stop infinite loops

		List<Direction> wireConnections = WireBlock.getConnections(level, pos);

		for(Direction direction : wireConnections) {
			BlockPos checkPos = pos.relative(direction);
			if(blackList.contains(checkPos))
				continue;

			BlockState state = level.getBlockState(checkPos);
			if(WireBlock.isWire(state))
				getConnections(level, checkPos, connections, blackList); // Additional wires will get checked themselves
			else if(level.getBlockEntity(checkPos) instanceof IPowerNetworkItem networkItem) {
				connections.put(networkItem, direction.getOpposite()); // Network items will be added to connections and blacklisted
				blackList.add(checkPos);
			}

		}

		return connections;
	}

}

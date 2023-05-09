package com.arrl.radiocraft.common.power;

import com.arrl.radiocraft.api.benetworks.IBENetworkItem;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.blocks.WireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WireUtils {

	/**
	 * Finds network connections for a specific wire.
	 */
	public static Map<IBENetworkItem, Direction> findWireNetworkConnections(BlockGetter level, BlockPos pos, Predicate<BlockState> validWire, Predicate<BlockState> validConnection) {
		List<BlockPos> positionsChecked = new ArrayList<>();
		Map<IBENetworkItem, Direction> connections = new HashMap<>();

		getConnections(level, pos, connections, positionsChecked, validWire, validConnection);

		return connections;
	}

	/**
	 * Merges all connections on a wire and/or creates a new one
	 * @param fallbackSupplier provides fallback network object if there were no existing networks present.
	 */
	public static void mergeWireNetworks(BlockGetter level, BlockPos pos, Predicate<BlockState> validWire, Predicate<BlockState> validConnection, Predicate<BENetwork> validNetwork, Supplier<BENetwork> fallbackSupplier) {
		Map<IBENetworkItem, Direction> connections = findWireNetworkConnections(level, pos, validWire, validConnection);

		if(connections.size() > 0) {
			List<BENetwork> existingNetworks = new ArrayList<>();
			Set<IBENetworkItem> nullItems = new HashSet<>(); // These network items don't already have a network.

			for(IBENetworkItem networkItem : connections.keySet()) {
				Set<BENetwork> networks = networkItem.getNetworks(connections.get(networkItem));

				boolean valid = false;
				if(networks != null) {
					if(!networks.isEmpty()) {
						for(BENetwork network : networks) {
							if(validNetwork.test(network)) {
								if(!existingNetworks.contains(network))
									existingNetworks.add(network);
								valid = true;
							}
						}
					}
				}
				if(!valid)
					nullItems.add(networkItem);
			}

			BENetwork newNetwork = BENetwork.merge(existingNetworks, fallbackSupplier); // Only throws NPE if another mod dev made a dodgy network impl
			nullItems.forEach(networkItem -> {
				Direction direction = connections.get(networkItem);
				networkItem.addNetwork(direction, newNetwork);
				newNetwork.addConnection(networkItem);
			});
		}
	}

	/**
	 * Splits the network associated with a given wire block. This method is quite laggy and should be avoided where possible.
	 */
	public static void splitWireNetwork(BlockGetter level, BlockPos pos, Predicate<BlockState> validWire, Predicate<BlockState> validConnection, Predicate<BENetwork> validNetwork) {
		Set<IBENetworkItem> itemsUpdated = new HashSet<>();
		Map<Direction, Map<IBENetworkItem, Direction>> connectionsPerSide = new HashMap<>();

		for(Direction direction : Direction.values()) {
			BlockPos checkPos = pos.relative(direction);

			Map<IBENetworkItem, Direction> connections = new HashMap<>();
			List<BlockPos> positionsChecked = new ArrayList<>();
			positionsChecked.add(pos);

			if(level.getBlockEntity(checkPos) instanceof IBENetworkItem networkItem) {
				connections.put(networkItem, direction.getOpposite()); // If already at end of wire, add to connections.
				connectionsPerSide.put(direction, connections);
			}
			else if(validWire.test(level.getBlockState(checkPos))) {
				getConnections(level, checkPos, connections, positionsChecked, validWire, validConnection); // Else, grab all connections
				connectionsPerSide.put(direction, connections); // Only added if that side actually has connection(s).
			}
		}

		for(Direction direction : connectionsPerSide.keySet()) {
			Map<IBENetworkItem, Direction> connections = connectionsPerSide.get(direction);

			BENetwork networkToReplace = null;
			for(IBENetworkItem networkItem : connections.keySet()) {
				for(BENetwork network : networkItem.getNetworks(connections.get(networkItem))) {
					if(validNetwork.test(network))
						networkToReplace = network;
				}
				if(networkToReplace != null)
					break;
			}

			if(networkToReplace != null) {
				BENetwork newNetwork = networkToReplace.createNetwork();
				for(IBENetworkItem entry : connections.keySet()) {
					if(!itemsUpdated.contains(entry)) {
						newNetwork.addConnection(entry);
						entry.replaceNetwork(networkToReplace, newNetwork); // Replace existing network with new one only containing items from this side.
						itemsUpdated.add(entry); // These items will not get updated again in case they were connected to multiple sides.
					}
				}
			}
		}
	}

	/**
	 * Recursively checks for all connections to a wire block
	 */
	private static void getConnections(BlockGetter level, BlockPos pos, Map<IBENetworkItem, Direction> connections, List<BlockPos> blackList, Predicate<BlockState> validWire, Predicate<BlockState> validConnection) {
		blackList.add(pos); // Add pos to blacklist to stop infinite loops
		BlockState state = level.getBlockState(pos);
		if(validWire.test(state)) {
			WireBlock wire = (WireBlock)state.getBlock(); // If this crashes it's because a mod or pack dev did something stupid.
			List<Direction> wireConnections = wire.getConnections(level, pos);

			for(Direction direction : wireConnections) {
				BlockPos checkPos = pos.relative(direction);
				if(blackList.contains(checkPos))
					continue;

				BlockState checkState = level.getBlockState(checkPos);
				if(validConnection.test(checkState))
					connections.put((IBENetworkItem)level.getBlockEntity(checkPos), direction.getOpposite()); // Add valid connections to list-- if this crashes it's because a mod or pack dev did something stupid.
				else
					getConnections(level, checkPos, connections, blackList, validWire, validConnection); // Check non-connections as potential wires.
			}
		}
	}

}

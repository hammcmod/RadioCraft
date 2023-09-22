package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a {@link BlockEntity} which connects to a coax network (See: {@link BENetwork}). Blocks which connect to a
 * {@link PowerNetwork} are a different type and should implement {@link IPowerNetworkItem}.
 */
public interface IBENetworkItem {

	Map<Direction, Set<BENetwork>> getNetworkMap();

	/**
	 * @param side The {@link Direction} of the connected networks required.
	 *
	 * @return Every {@link BENetwork} this {@link IBENetworkItem} is connected to on the specified side.
	 */
	default Set<BENetwork> getNetworks(Direction side) {
		return getNetworkMap().get(side);
	}

	/**
	 * Replace the {@link Set<BENetwork>} associated with a {@link Direction} with the provided set.
	 *
	 * @param side The {@link Direction} of the block the networks are connected to.
	 * @param networks The {@link Set<BENetwork>} to be used as a replacement.
	 */
	default void setNetworks(Direction side, Set<BENetwork> networks) {
		getNetworkMap().put(side, networks);
	}

	/**
	 * Connect this {@link BlockEntity} to a {@link BENetwork}/Add that {@link BENetwork} to the connections on the
	 * specified side.
	 *
	 * @param side The {@link Direction} of the connected {@link BENetwork}.
	 * @param network The {@link BENetwork} this {@link BlockEntity} is connecting to.
	 */
	default void addNetwork(Direction side, BENetwork network) {
		Set<BENetwork> networks = getNetworks(side);
		if(networks == null)
			setNetworks(side, new HashSet<>());
		getNetworks(side).add(network);
	}

	/**
	 * Clear side of all network associations
	 */
	default void clearNetworks(Direction side) {
		getNetworkMap().get(side).clear();
	}

	/**
	 * Remove a network from all sides.
	 */
	default void removeNetwork(BENetwork network) {
		for(Set<BENetwork> side : getNetworkMap().values())
			side.remove(network);
	}

	/**
	 * Remove a network from a specified side.
	 */
	default void removeNetwork(Direction side, BENetwork network) {
		getNetworks(side).remove(network);
	}

	/**
	 * Replace all occurences of oldNetwork with newNetwork (on all sides)
	 */
	default void replaceNetwork(BENetwork oldNetwork, BENetwork newNetwork) {
		for(Direction direction : getNetworkMap().keySet()) {
			Set<BENetwork> side = getNetworks(direction);
			if(side.contains(oldNetwork)) { // Only replace if old network contained it.
				side.remove(oldNetwork);
				side.add(newNetwork);
			}
		}
	}

	/**
	 * Replace all occurences of oldNetwork with newNetwork (on one side)
	 */
	default void replaceNetwork(Direction direction, BENetwork oldNetwork, BENetwork newNetwork) {
		Set<BENetwork> side = getNetworks(direction);
		if(side.contains(oldNetwork)) { // Only replace if old network contained it.
			side.remove(oldNetwork);
			side.add(newNetwork);
		}
	}

	/**
	 * Called whenever a change is made to this network.
	 */
	default void networkUpdated(BENetwork network) {}

}

package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.common.benetworks.BENetwork;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Any BlockEntity which is intended to connect to a coax network should implement this. Power networks are a different type and should implement IPowerNetworkItem.
 */
public interface IBENetworkItem {

	Map<Direction, Set<BENetwork>> getNetworkMap();

	/**
	 * Get all networks associated with a side.
	 */
	default Set<BENetwork> getNetworks(Direction side) {
		return getNetworkMap().get(side);
	}

	/**
	 * Replace side's network list.
	 */
	default void setNetworks(Direction side, Set<BENetwork> networks) {
		getNetworkMap().put(side, networks);
	}

	/**
	 * Add network to side.
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

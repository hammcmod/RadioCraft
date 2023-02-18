package com.arrl.radiocraft.common.power;

import net.minecraft.core.Direction;

import java.util.Map;

public interface IPowerNetworkItem {
	 Map<Direction, PowerNetwork> getNetworks();
	 default void setNetworks(Map<Direction, PowerNetwork> network) {}

	 default ConnectionType getDefaultConnectionType() {
		 return ConnectionType.PULL;
	 }

	 default void setNetwork(Direction direction, PowerNetwork network) {
		 getNetworks().put(direction, network);
	 }

	 default void removeNetwork(PowerNetwork network) {
		 getNetworks().remove(network);
	 }

	 default void removeNetwork(Direction direction) {
		 getNetworks().remove(direction);
	 }

	 default Direction getKey(PowerNetwork network) {
		 Map<Direction, PowerNetwork> networkMap = getNetworks();
		 for(Direction key : networkMap.keySet())
			 if(networkMap.get(key) == network)
				 return key;
		 return null;
	 }

}

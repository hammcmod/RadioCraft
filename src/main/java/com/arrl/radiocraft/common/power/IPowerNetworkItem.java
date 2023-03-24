package com.arrl.radiocraft.common.power;

import net.minecraft.core.Direction;

import java.util.Map;

public interface IPowerNetworkItem {
	 Map<Direction, PowerNetwork> getNetworks();

	 default void setNetworks(Map<Direction, PowerNetwork> network) {}

	 default ConnectionType getConnectionType() {
		 return ConnectionType.PULL;
	 }

	 default PowerNetwork getNetwork(Direction direction) {
		 return getNetworks().get(direction);
	}

	 default void setNetwork(Direction direction, PowerNetwork network) {
		 getNetworks().put(direction, network);
	 }

	 default void removeNetwork(PowerNetwork network) {
		 getNetworks().remove(getKey(network));
	 }

	 default void removeNetwork(Direction direction) {
		 getNetworks().remove(direction);
	 }

	 default void replaceNetwork(PowerNetwork oldNetwork, PowerNetwork newNetwork) {
		 Direction key = getKey(oldNetwork);
		 if(key != null)
			 getNetworks().put(key, newNetwork);
	 }

	 default Direction getKey(PowerNetwork network) {
		 Map<Direction, PowerNetwork> networkMap = getNetworks();
		 for(Direction key : networkMap.keySet())
			 if(networkMap.get(key) == network)
				 return key;
		 return null;
	 }

}

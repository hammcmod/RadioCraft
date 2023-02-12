package com.arrl.radiocraft.common.power;

import java.util.List;

public interface IPowerNetworkItem {
	 List<PowerNetwork> getNetworks();
	 void setNetworks(List<PowerNetwork> network);

	 default ConnectionType getDefaultConnectionType() {
		 return ConnectionType.PULL;
	 }

	 default void addNetwork(PowerNetwork network) {
		 getNetworks().add(network);
	 }

	 default void removeNetwork(PowerNetwork network) {
		 getNetworks().remove(network);
	 }
}

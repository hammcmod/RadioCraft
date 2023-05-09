package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.common.power.ConnectionType;

/**
 * Any BlockEntity which is intended to connect to a power network should implement this. Power consumption is managed by capabilities.
 */
public interface IPowerNetworkItem extends IBENetworkItem {

	 default ConnectionType getConnectionType() {
		 return ConnectionType.PULL;
	 }

}

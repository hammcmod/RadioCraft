package com.arrl.radiocraft.common.power;

import net.minecraft.core.Direction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * PowerNetwork represents all devices connected to a given line of wires
 */
public class PowerNetwork {

	private final List<PowerNetworkEntry> connections;

	public PowerNetwork(List<PowerNetworkEntry> entries) {
		if(entries == null)
			connections = new ArrayList<>();
		else
			this.connections = entries;
	}

	public List<PowerNetworkEntry> getConnections() {
		cleanConnections();
		return connections;
	}

	public void addConnection(IPowerNetworkItem networkItem, ConnectionType type, Direction direction) {
		connections.add(new PowerNetworkEntry(networkItem, type, direction));
	}

	public void removeConnection(IPowerNetworkItem networkItem) {
		cleanConnections();
		connections.removeIf(entry -> entry.getNetworkItem() == networkItem);
	}

	/**
	 * Removes this network from all of it's devices, GC should delete it after.
	 */
	public void dissolve() {
		for(PowerNetworkEntry entry : connections)
			if(entry.getNetworkItem() != null)
				entry.getNetworkItem().removeNetwork(this);
	}

	/**
	 * Merges an array of power networks and replaces their entries on all connected devices with the new merged network.
	 */
	public static PowerNetwork merge(PowerNetwork... networks) {
		PowerNetwork newNetwork = new PowerNetwork(null);
		List<PowerNetworkEntry> newEntries = newNetwork.getConnections();

		for(PowerNetwork oldNetwork : networks) {
			for(PowerNetworkEntry connection : oldNetwork.getConnections()) {
				newEntries.add(connection); // Add this device to the new network and replace the network entry with the new network
				connection.getNetworkItem().replaceNetwork(oldNetwork, newNetwork);
			}
		}
		return newNetwork;
	}

	private void cleanConnections() { // Remove any null connections in case they still exist.
		connections.removeIf(entry -> entry.getNetworkItem() == null);
	}

	/**
	 * Represents a power consumer or provider within a network
	 */
	private static class PowerNetworkEntry {

		private final WeakReference<IPowerNetworkItem> networkItem; // Use weak reference so network items don't stay loaded if chunk unloads.
		private final ConnectionType connectionType;

		public PowerNetworkEntry(IPowerNetworkItem networkItem, ConnectionType connectionType, Direction direction) {
			this.networkItem = new WeakReference<>(networkItem);
			this.connectionType = connectionType;
		}

		public IPowerNetworkItem getNetworkItem() {
			return networkItem.get();
		}

		public ConnectionType getConnectionType() {
			return connectionType;
		}

	}
}

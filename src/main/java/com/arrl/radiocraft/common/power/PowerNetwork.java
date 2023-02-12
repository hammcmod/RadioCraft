package com.arrl.radiocraft.common.power;

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
		return connections;
	}

	public void addConnection(IPowerNetworkItem networkItem) {
		addConnection(networkItem, networkItem.getDefaultConnectionType());
	}

	public void addConnection(IPowerNetworkItem networkItem, ConnectionType type) {
		connections.add(new PowerNetworkEntry(networkItem, type));
	}

	public void removeConnection(IPowerNetworkItem networkItem) {
		connections.remove(networkItem);
	}

	/**
	 * Removes this network from all of it's devices, GC should delete it after.
	 */
	public void dissolve() {
		for(PowerNetworkEntry entry : connections) {
			entry.networkItem().removeNetwork(this);
		}
	}

	/**
	 * Merges two power networks and replaces their entries on all connected devices with the new merged network.
	 * @param first
	 * @param second
	 */
	public static void merge(PowerNetwork first, PowerNetwork second) {
		List<PowerNetworkEntry> newEntries = first.getConnections();
		newEntries.addAll(second.getConnections());
		PowerNetwork newNetwork = new PowerNetwork(newEntries);

		for(PowerNetworkEntry entry : first.getConnections()) // Remove existing networks from all devices
			entry.networkItem().removeNetwork(first);
		for(PowerNetworkEntry entry : first.getConnections())
			entry.networkItem().removeNetwork(first);

		first.getConnections().forEach(entry -> entry.networkItem().addNetwork(newNetwork)); // Add merged network to all devices
		second.getConnections().forEach(entry -> entry.networkItem().addNetwork(newNetwork));
	}

	private record PowerNetworkEntry(IPowerNetworkItem networkItem, ConnectionType connectionType) {}
}

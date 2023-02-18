package com.arrl.radiocraft.common.power;

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

	public void addConnection(IPowerNetworkItem networkItem) {
		addConnection(networkItem, networkItem.getDefaultConnectionType());
	}

	public void addConnection(IPowerNetworkItem networkItem, ConnectionType type) {
		connections.add(new PowerNetworkEntry(networkItem, type));
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
	 * Merges two power networks and replaces their entries on all connected devices with the new merged network.
	 * @param first
	 * @param second
	 */
	public static void merge(PowerNetwork first, PowerNetwork second) {
		List<PowerNetworkEntry> newEntries = first.getConnections();
		newEntries.addAll(second.getConnections());

		PowerNetwork newNetwork = new PowerNetwork(newEntries);

		// Replace merged network to on devices
		first.getConnections().forEach(entry -> {
			IPowerNetworkItem item = entry.getNetworkItem();
			entry.getNetworkItem().setNetwork(item.getKey(first), newNetwork);
		});
		second.getConnections().forEach(entry -> {
			IPowerNetworkItem item = entry.getNetworkItem();
			entry.getNetworkItem().setNetwork(item.getKey(second), newNetwork);
		});
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

		public PowerNetworkEntry(IPowerNetworkItem networkItem, ConnectionType connectionType) {
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

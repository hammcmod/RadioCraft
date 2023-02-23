package com.arrl.radiocraft.common.power;

import net.minecraft.core.Direction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
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

	public PowerNetworkEntry getConnectionByItem(IPowerNetworkItem networkItem) {
		for(PowerNetworkEntry entry : connections)
			if(entry.getNetworkItem() == networkItem)
				return entry;
		return null;
	}

	public void addConnection(IPowerNetworkItem networkItem, ConnectionType type, Direction direction) {
		addConnection(new PowerNetworkEntry(networkItem, type, direction));
	}

	public void addConnection(PowerNetworkEntry entry) {
		connections.add(entry);
	}

	public void removeConnection(IPowerNetworkItem networkItem) {
		cleanConnections();
		connections.removeIf(entry -> entry.getNetworkItem() == networkItem);
	}

	public void removeConnection(PowerNetworkEntry entry) {
		cleanConnections();
		connections.remove(entry);
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

	/**
	 * Splits the network associated with a wire block, returns a new network with the entries passed in.
	 */
	public void split(Collection<IPowerNetworkItem> itemsToSplit) {
		PowerNetwork newNetwork = new PowerNetwork(null);

		for(IPowerNetworkItem item : itemsToSplit) {
			PowerNetworkEntry entry = getConnectionByItem(item);
			removeConnection(entry); // Remove from old network
			newNetwork.addConnection(entry); // Add to new network
			item.replaceNetwork(this, newNetwork); // Replace old network with new on the item
		}
	}

	/**
	 * Removes null connection refs
	 */
	private void cleanConnections() {
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

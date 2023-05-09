package com.arrl.radiocraft.common.benetworks;

import com.arrl.radiocraft.api.benetworks.IBENetworkItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Network represents a "connected" set of BlockEntities
 */
public class BENetwork {

	protected final List<BENetworkEntry> connections;

	public BENetwork(List<BENetworkEntry> entries) {
		if(entries == null)
			connections = new ArrayList<>();
		else
			this.connections = entries;
	}

	public BENetwork() {
		this(null);
	}

	public List<BENetworkEntry> getConnections() {
		clean();
		return connections;
	}

	public BENetworkEntry getConnectionByItem(IBENetworkItem networkItem) {
		for(BENetworkEntry entry : connections)
			if(entry.getNetworkItem() == networkItem)
				return entry;
		return null;
	}

	public void addConnection(IBENetworkItem networkItem) {
		addConnection(new BENetworkEntry(networkItem));
	}

	public void addConnection(BENetworkEntry entry) {
		connections.add(entry);
	}


	public void removeConnection(IBENetworkItem networkItem) {
		clean();
		connections.removeIf(entry -> entry.getNetworkItem() == networkItem);
	}

	public void removeConnection(BENetworkEntry entry) {
		clean();
		connections.remove(entry);
	}

	/**
	 * Removes this network from all of it's devices, GC should delete it after.
	 */
	public void dissolve() {
		for(BENetworkEntry entry : connections)
			if(entry.getNetworkItem() != null)
				entry.getNetworkItem().removeNetwork(this);
	}

	/**
	 * Merges an array of networks and replaces their entries on all connected devices with the new merged network.
	 */
	public static BENetwork merge(List<BENetwork> networks, Supplier<BENetwork> fallbackSupplier) {
		if(networks.size() > 0) {
			BENetwork newNetwork = networks.get(0).createNetwork(); // Network type is taken from first instance, callers of this method are responsible for ensuring that the inputs are all the same type.
			for(BENetwork oldNetwork : networks) {
				for(BENetworkEntry connection : oldNetwork.getConnections()) {
					newNetwork.addConnection(connection); // Add this device to the new network and replace the network entry with the new network
					connection.getNetworkItem().replaceNetwork(oldNetwork, newNetwork);
				}
			}
			return newNetwork;
		}
		return fallbackSupplier.get();
	}

	/**
	 * Splits the network associated with a wire block, returns a new network with the entries passed in.
	 */
	public void split(Collection<IBENetworkItem> itemsToSplit) {
		BENetwork newNetwork = new BENetwork();

		for(IBENetworkItem item : itemsToSplit) {
			BENetworkEntry entry = getConnectionByItem(item);
			if(entry != null) {
				removeConnection(entry); // Remove from old network
				newNetwork.addConnection(entry); // Add to new network
				item.replaceNetwork(this, newNetwork); // Replace old network with new on the item
			}
		}
	}

	/**
	 * Removes null connection refs
	 */
	protected void clean() {
		connections.removeIf(entry -> entry.getNetworkItem() == null);
	}

	/**
	 * Create new empty instance of this network, for when networks use a different type (e.g. PowerNetwork)
	 */
	public BENetwork createNetwork() {
		return new BENetwork();
	}

	/**
	 * Represents a member of a BE network
	 */
	public static class BENetworkEntry {

		private final WeakReference<IBENetworkItem> networkItem; // Use weak reference so network items don't stay loaded if chunk unloads.

		public BENetworkEntry(IBENetworkItem networkItem) {
			this.networkItem = new WeakReference<>(networkItem);
		}

		public IBENetworkItem getNetworkItem() {
			return networkItem.get();
		}

	}

}
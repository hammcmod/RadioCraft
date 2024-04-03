package com.arrl.radiocraft.common.benetworks;

import com.arrl.radiocraft.api.benetworks.IBENetworkItem;
import com.arrl.radiocraft.common.benetworks.power.WireUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
			connections = entries;
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
		updateConnections();
	}

	public void removeConnection(IBENetworkItem networkItem) {
		clean();
		connections.removeIf(entry -> entry.getNetworkItem() == networkItem);
		updateConnections();
	}

	public void removeConnection(BENetworkEntry entry) {
		clean();
		connections.remove(entry);
		updateConnections();
	}

	private void updateConnections() {
		for(BENetworkEntry connection : connections) {
			IBENetworkItem item = connection.getNetworkItem();
			if(item != null)
				item.networkUpdated(this);
		}
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
		if(!networks.isEmpty()) {
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
	 * Attempts to merge a IBENetworkItem at level, pos onto surrounding networks.
	 */
	public static void tryConnectToNetworks(Level level, BlockPos pos, Predicate<BlockState> validWire, Predicate<BlockState> validConnection, Predicate<BENetwork> validNetwork, Supplier<BENetwork> fallbackSupplier) {
		for(Direction direction : Direction.values())
			WireUtils.mergeWireNetworks(level, pos.relative(direction), validWire, validConnection, validNetwork, fallbackSupplier);
	}

	/**
	 * Attempt to disconnect the BE at level, pos from all of it's networks.
	 */
	public static void tryRemoveFromNetworks(Level level, BlockPos pos) {
		if(level.getBlockEntity(pos) instanceof IBENetworkItem networkItem) {
			for(Set<BENetwork> side : networkItem.getNetworkMap().values()) {
				for(BENetwork network : side)
					network.removeConnection(networkItem);
				side.clear();
			}
		}
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
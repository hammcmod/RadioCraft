package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Represents a network of {@link BENetworkObject}s.
 */
public class BENetwork {

    /**
     * The type of network.
     */
    public static final ResourceLocation COAXIAL_TYPE = Radiocraft.id("coaxial");

    /**
     * The {@link BENetworkObject}s in this network.
     */
    protected final Set<BENetworkObject> networkObjects = new HashSet<>();

    /**
     * The UUID of this network.
     */
    protected final UUID uuid;

    /**
     * Creates a new {@link BENetwork} with a specified UUID.
     * @param uuid The UUID of the network.
     */
    public BENetwork(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Creates a new {@link BENetwork} with a random UUID.
     */
    public BENetwork() {
        this(UUID.randomUUID());
    }

    /**
     * Adds a {@link BENetworkObject} to this network.
     * @param networkObject The {@link BENetworkObject} to add.
     */
    public void add(BENetworkObject networkObject) {
        networkObjects.add(networkObject);
        networkObject.onNetworkAdd(this);
        for(BENetworkObject obj : networkObjects)
            obj.onNetworkUpdateAdd(this, networkObject);
    }

    /**
     * Removes a {@link BENetworkObject} from this network.
     * @param networkObject The {@link BENetworkObject} to remove.
     * @param updateSelf Whether to update the {@link BENetworkObject} that called this method.
     */
    public void remove(BENetworkObject networkObject, boolean updateSelf) {
        networkObjects.remove(networkObject);
        if(updateSelf)
            networkObject.onNetworkRemove(this);
        for(BENetworkObject obj : networkObjects)
            obj.onNetworkUpdateRemove(this, networkObject);

        if(getNetworkObjects().isEmpty())
            IBENetworks.removeNetwork(networkObject.level, this);
    }

    /**
     * Gets the {@link BENetworkObject}s in this network.
     * @return networkObjects
     */
    public Set<BENetworkObject> getNetworkObjects() {
        return networkObjects;
    }

    /**
     * Gets the UUID of this network.
     * @return The UUID of this network.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the type of network.
     * @return The type of network.
     */
    public ResourceLocation getType() {
        return COAXIAL_TYPE;
    }

    /**
     * Merges an array of networks and replaces their entries on all connected devices with the new merged network.
     * @param networks The networks to merge.
     * @param newSupplier A supplier for a new network.
     * @param level The level to add the new network to.
     * @return The merged network.
     */
    public static BENetwork merge(Set<BENetwork> networks, Supplier<BENetwork> newSupplier, Level level) {
        BENetwork newNetwork = newSupplier.get();

        for(BENetwork oldNetwork : networks) {
            for(BENetworkObject networkObject : oldNetwork.getNetworkObjects()) {
                newNetwork.add(networkObject);
                networkObject.replaceNetwork(oldNetwork, newNetwork);
            }
            IBENetworks.removeNetwork(level, oldNetwork);
        }

        IBENetworks.addNetwork(level, newNetwork);
        return newNetwork;
    }

}

package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class BENetwork {

    public static final ResourceLocation COAXIAL_TYPE = Radiocraft.id("coaxial");

    protected final Set<BENetworkObject> networkObjects = new HashSet<>();
    protected final UUID uuid;

    public BENetwork(UUID uuid) {
        this.uuid = uuid;
    }

    public BENetwork() {
        this(UUID.randomUUID());
    }

    public void add(BENetworkObject networkObject) {
        networkObjects.add(networkObject);
        networkObject.onNetworkAdd(this);
        for(BENetworkObject obj : networkObjects)
            obj.onNetworkUpdateAdd(this, networkObject);
    }

    public void remove(BENetworkObject networkObject, boolean updateSelf) {
        networkObjects.remove(networkObject);
        if(updateSelf)
            networkObject.onNetworkRemove(this);
        for(BENetworkObject obj : networkObjects)
            obj.onNetworkUpdateRemove(this, networkObject);

        if(getNetworkObjects().isEmpty())
            IBENetworks.removeNetwork(networkObject.level, this);
    }

    public Set<BENetworkObject> getNetworkObjects() {
        return networkObjects;
    }

    public UUID getUUID() {
        return uuid;
    }

    public ResourceLocation getType() {
        return COAXIAL_TYPE;
    }

    /**
     * Merges an array of networks and replaces their entries on all connected devices with the new merged network.
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

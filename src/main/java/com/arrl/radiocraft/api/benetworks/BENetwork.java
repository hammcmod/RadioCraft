package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BENetwork {

    public static final ResourceLocation DEFAULT_TYPE = Radiocraft.location("coaxial");

    protected final Set<BENetworkObject> networkObjects = new HashSet<>();
    protected final UUID uuid;

    public BENetwork(UUID uuid) {
        this.uuid = uuid;
    }

    public void add(BENetworkObject networkObject) {
        networkObjects.add(networkObject);
    }

    public void remove(BENetworkObject networkObject) {
        networkObjects.remove(networkObject);
    }

    public UUID getUuid() {
        return uuid;
    }

    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

}

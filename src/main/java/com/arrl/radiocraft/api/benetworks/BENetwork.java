package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BENetwork<T extends BENetworkObject> {

    public static final ResourceLocation DEFAULT_TYPE = Radiocraft.location("coaxial");

    protected final List<T> networkObjects = new ArrayList<>();
    protected final UUID uuid;

    public BENetwork(UUID uuid) {
        this.uuid = uuid;
    }

    public void add(T networkObject) {
        networkObjects.add(networkObject);
    }

    public void remove(T networkObject) {
        networkObjects.remove(networkObject);
    }

    public UUID getUuid() {
        return uuid;
    }

    public ResourceLocation getType() {
        return DEFAULT_TYPE;
    }

}

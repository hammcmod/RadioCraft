package com.arrl.radiocraft.api.benetworks;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class BENetworkRegistry {

    private static Map<ResourceLocation, Function<UUID, BENetwork<?>>> networkTypes;
    private static Map<ResourceLocation, Supplier<BENetworkObject>> objectTypes;

    public static void registerNetwork(ResourceLocation id, Function<UUID, BENetwork<?>> networkSupplier) {
        if(networkTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network type.");
        networkTypes.put(id, networkSupplier);
    }

    public static void registerObject(ResourceLocation id, Supplier<BENetworkObject> networkSupplier) {
        if(objectTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network object type.");
        objectTypes.put(id, networkSupplier);
    }

}

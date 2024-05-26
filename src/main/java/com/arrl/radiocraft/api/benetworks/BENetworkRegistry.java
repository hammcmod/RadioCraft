package com.arrl.radiocraft.api.benetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BENetworkRegistry {

    private static final Map<ResourceLocation, Function<UUID, BENetwork>> networkTypes = new HashMap<>();
    private static final Map<ResourceLocation, BiFunction<Level, BlockPos, BENetworkObject>> objectTypes = new HashMap<>();

    public static void registerNetwork(ResourceLocation id, Function<UUID, BENetwork> networkSupplier) {
        if(networkTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network type.");
        networkTypes.put(id, networkSupplier);
    }

    public static void registerObject(ResourceLocation id, BiFunction<Level, BlockPos, BENetworkObject> networkSupplier) {
        if(objectTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network object type.");
        objectTypes.put(id, networkSupplier);
    }

    public static BENetwork createNetwork(ResourceLocation id, UUID uuid) {
        return networkTypes.get(id).apply(uuid);
    }

    public static BENetworkObject createObject(ResourceLocation id, Level level, BlockPos pos) {
        return objectTypes.get(id).apply(level, pos);
    }

}

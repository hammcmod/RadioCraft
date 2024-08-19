package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
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

    public static BENetwork createNetwork(ResourceLocation id, UUID uuid, Level level) {
        BENetwork network = networkTypes.get(id).apply(uuid);

        // TODO: Something needs to be here.
        //level.getCapability(RadiocraftCapabilities.BE_NETWORKS).ifPresent(cap -> cap.addNetwork(network));
        return network;
    }

    public static BENetworkObject createObject(ResourceLocation id, Level level, BlockPos pos) {
        BENetworkObject object = objectTypes.get(id).apply(level, pos);

        RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null).setObject(pos, object);

        return object;
    }

}

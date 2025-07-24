package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
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

        // Get the BE_NETWORKS capability and add the network to it
        IBENetworks beNetworks = RadiocraftCapabilities.BE_NETWORKS.getCapability(level, BlockPos.ZERO, null, null, null);
        if (beNetworks != null) {
            beNetworks.addNetwork(network);
        } else {
            throw new IllegalStateException("Could not get BE_NETWORKS capability for level. Network creation will fail.");
        }

        return network;
    }

    public static BENetworkObject createObject(ResourceLocation id, Level level, BlockPos pos) {
        // Check if the object type exists
        if (!objectTypes.containsKey(id)) {
            throw new IllegalArgumentException("No network object type registered for id: " + id);
        }

        // Create the network object
        BENetworkObject object = objectTypes.get(id).apply(level, pos);

        // Get the BE_NETWORKS capability for this position
        IBENetworks beNetworks = RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null);
        if (beNetworks != null) {
            // Set the object in the capability
            beNetworks.setObject(pos, object);
        } else {
            throw new IllegalStateException("Could not get BE_NETWORKS capability for position " + pos + ". Make sure the block is registered for BE_NETWORKS capability.");
        }

        return object;
    }
}
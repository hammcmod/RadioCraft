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

/**
 * Registry for {@link BENetwork} and {@link BENetworkObject} types.
 */
public class BENetworkRegistry {

    /**
     * Constructor for the {@link BENetworkRegistry}.
     */
    public BENetworkRegistry() {}

    private static final Map<ResourceLocation, Function<UUID, BENetwork>> networkTypes = new HashMap<>();
    private static final Map<ResourceLocation, BiFunction<Level, BlockPos, BENetworkObject>> objectTypes = new HashMap<>();

    /**
     * Registers a new {@link BENetwork} type.
     * @param id The {@link ResourceLocation} of the type of network to register.
     * @param networkSupplier A {@link Function} that takes a {@link UUID} and returns a new {@link BENetwork}.
     */
    public static void registerNetwork(ResourceLocation id, Function<UUID, BENetwork> networkSupplier) {
        if(networkTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network type.");
        networkTypes.put(id, networkSupplier);
    }

    /**
     * Registers a new {@link BENetworkObject} type.
     * @param id The {@link ResourceLocation} of the type of object to register.
     * @param networkSupplier A {@link BiFunction} that takes a {@link Level} and {@link BlockPos} and returns a new {@link BENetworkObject}.
     */
    public static void registerObject(ResourceLocation id, BiFunction<Level, BlockPos, BENetworkObject> networkSupplier) {
        if(objectTypes.containsKey(id))
            throw new IllegalArgumentException("Tried to register a duplicate network object type.");
        objectTypes.put(id, networkSupplier);
    }

    /**
     * Creates a new {@link BENetwork} for the specified level, uuid, and type.
     * @param id The {@link ResourceLocation} of the type of network to create.
     * @param uuid The {@link UUID} of the network.
     * @param level The {@link Level} to create the network in.
     * @return The newly created {@link BENetwork}.
     */
    public static BENetwork createNetwork(ResourceLocation id, UUID uuid, Level level) {
        BENetwork network = networkTypes.get(id).apply(uuid);

        // TODO: Something needs to be here.
        //level.getCapability(RadiocraftCapabilities.BE_NETWORKS).ifPresent(cap -> cap.addNetwork(network));
        return network;
    }

    /**
     * Creates a new {@link BENetworkObject} for the specified level, pos, and type.
     * @param id The {@link ResourceLocation} of the type of object to create.
     * @param level The {@link Level} to create the object in.
     * @param pos The {@link BlockPos} to create the object at.
     * @return The newly created {@link BENetworkObject}.
     */
    public static BENetworkObject createObject(ResourceLocation id, Level level, BlockPos pos) {
        BENetworkObject object = objectTypes.get(id).apply(level, pos);

        RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null).setObject(pos, object);

        return object;
    }

}

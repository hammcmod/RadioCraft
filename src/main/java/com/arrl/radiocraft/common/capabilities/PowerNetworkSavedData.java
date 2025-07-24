
package com.arrl.radiocraft.common.data;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * SavedData for managing power networks across the level
 */
public class PowerNetworkSavedData extends SavedData {

    private static final String DATA_NAME = Radiocraft.MOD_ID + "_power_networks";

    private final Map<BlockPos, UUID> positionToNetwork = new HashMap<>();
    private final Map<UUID, PowerBENetwork> networks = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> networkPositions = new HashMap<>();

    public PowerNetworkSavedData() {}

    public static PowerNetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(PowerNetworkSavedData::new, PowerNetworkSavedData::load),
                DATA_NAME
        );
    }

    private static PowerNetworkSavedData load(CompoundTag nbt, HolderLookup.Provider provider) {
        PowerNetworkSavedData data = new PowerNetworkSavedData();

        // Load networks
        CompoundTag networksTag = nbt.getCompound("networks");
        for (String uuidStr : networksTag.getAllKeys()) {
            UUID networkId = UUID.fromString(uuidStr);
            PowerBENetwork network = new PowerBENetwork(networkId);
            data.networks.put(networkId, network);
            data.networkPositions.put(networkId, new HashSet<>());
        }

        // Load position mappings
        ListTag positionsTag = nbt.getList("positions", ListTag.TAG_COMPOUND);
        for (int i = 0; i < positionsTag.size(); i++) {
            CompoundTag posTag = positionsTag.getCompound(i);
            BlockPos pos = BlockPos.of(posTag.getLong("pos"));
            UUID networkId = posTag.getUUID("network");

            data.positionToNetwork.put(pos, networkId);
            data.networkPositions.computeIfAbsent(networkId, k -> new HashSet<>()).add(pos);
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        // Save networks
        CompoundTag networksTag = new CompoundTag();
        for (UUID networkId : networks.keySet()) {
            networksTag.putString(networkId.toString(), "power"); // Network type
        }
        nbt.put("networks", networksTag);

        // Save position mappings
        ListTag positionsTag = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : positionToNetwork.entrySet()) {
            CompoundTag posTag = new CompoundTag();
            posTag.putLong("pos", entry.getKey().asLong());
            posTag.putUUID("network", entry.getValue());
            positionsTag.add(posTag);
        }
        nbt.put("positions", positionsTag);

        return nbt;
    }

    public PowerBENetwork getNetwork(BlockPos pos) {
        UUID networkId = positionToNetwork.get(pos);
        return networkId != null ? networks.get(networkId) : null;
    }

    public PowerBENetwork createNetwork() {
        PowerBENetwork network = new PowerBENetwork();
        networks.put(network.getUUID(), network);
        networkPositions.put(network.getUUID(), new HashSet<>());
        setDirty();
        return network;
    }

    public void addToNetwork(BlockPos pos, PowerBENetwork network) {
        positionToNetwork.put(pos, network.getUUID());
        networkPositions.computeIfAbsent(network.getUUID(), k -> new HashSet<>()).add(pos);
        setDirty();
    }

    public void removeFromNetwork(BlockPos pos) {
        UUID networkId = positionToNetwork.remove(pos);
        if (networkId != null) {
            Set<BlockPos> positions = networkPositions.get(networkId);
            if (positions != null) {
                positions.remove(pos);
                // If network is empty, remove it
                if (positions.isEmpty()) {
                    networks.remove(networkId);
                    networkPositions.remove(networkId);
                }
            }
            setDirty();
        }
    }

    public void mergeNetworks(Set<PowerBENetwork> networksToMerge, PowerBENetwork targetNetwork) {
        for (PowerBENetwork networkToMerge : networksToMerge) {
            if (networkToMerge == targetNetwork) continue;

            Set<BlockPos> positions = networkPositions.get(networkToMerge.getUUID());
            if (positions != null) {
                for (BlockPos pos : positions) {
                    positionToNetwork.put(pos, targetNetwork.getUUID());
                }
                networkPositions.get(targetNetwork.getUUID()).addAll(positions);
            }

            // Remove old network
            networks.remove(networkToMerge.getUUID());
            networkPositions.remove(networkToMerge.getUUID());
        }
        setDirty();
    }

    public Set<BlockPos> getNetworkPositions(UUID networkId) {
        return networkPositions.getOrDefault(networkId, Collections.emptySet());
    }

    public Collection<PowerBENetwork> getAllNetworks() {
        return networks.values();
    }
}
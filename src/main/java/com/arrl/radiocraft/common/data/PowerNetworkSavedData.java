
package com.arrl.radiocraft.common.data;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.blocks.WireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Connects a wire block to adjacent networks, handling network merging when necessary.
     * Moved from WireBlock to centralize network management logic.
     */
    public void connectToAdjacentNetworks(ServerLevel level, BlockPos pos) {
        Set<PowerBENetwork> adjacentNetworks = new HashSet<>();

        // Find all adjacent networks
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = pos.relative(dir);
            if (WireBlock.canConnectTo(level.getBlockState(adjacentPos), true)) {
                PowerBENetwork network = getNetwork(adjacentPos);
                if (network != null) {
                    adjacentNetworks.add(network);
                }
            }
        }

        PowerBENetwork targetNetwork;
        if (adjacentNetworks.isEmpty()) {
            // Create new network
            targetNetwork = createNetwork();
        } else if (adjacentNetworks.size() == 1) {
            // Join existing network
            targetNetwork = adjacentNetworks.iterator().next();
        } else {
            // Merge multiple networks
            targetNetwork = createNetwork();
            mergeNetworks(adjacentNetworks, targetNetwork);
        }

        addToNetwork(pos, targetNetwork);
    }

    /**
     * Splits a network when a wire is removed, creating separate networks for disconnected segments.
     * This performs a breadth-first search to identify connected components after removal.
     */
    public void splitNetworkOnRemoval(ServerLevel level, BlockPos removedPos) {
        UUID originalNetworkId = positionToNetwork.get(removedPos);
        if (originalNetworkId == null) return;

        // Remove the position from the network first
        removeFromNetwork(removedPos);

        // Get all remaining positions that were in the original network
        Set<BlockPos> remainingPositions = new HashSet<>(networkPositions.getOrDefault(originalNetworkId, Collections.emptySet()));
        if (remainingPositions.isEmpty()) return;

        // Find connected components using flood fill
        List<Set<BlockPos>> connectedComponents = findConnectedComponents(level, remainingPositions);

        // If we have more than one component, we need to split
        if (connectedComponents.size() > 1) {
            // Remove the old network
            networks.remove(originalNetworkId);
            networkPositions.remove(originalNetworkId);

            // Create new networks for each component
            for (Set<BlockPos> component : connectedComponents) {
                PowerBENetwork newNetwork = createNetwork();
                for (BlockPos pos : component) {
                    positionToNetwork.put(pos, newNetwork.getUUID());
                    networkPositions.get(newNetwork.getUUID()).add(pos);
                }
            }
            setDirty();
        }
    }

    /**
     * Finds connected components in a set of positions using breadth-first search.
     */
    private List<Set<BlockPos>> findConnectedComponents(Level level, Set<BlockPos> positions) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> unvisited = new HashSet<>(positions);

        while (!unvisited.isEmpty()) {
            BlockPos start = unvisited.iterator().next();
            Set<BlockPos> component = exploreComponent(level, start, unvisited);
            components.add(component);
            unvisited.removeAll(component);
        }

        return components;
    }

    /**
     * Explores a connected component using breadth-first search.
     */
    private Set<BlockPos> exploreComponent(Level level, BlockPos start, Set<BlockPos> availablePositions) {
        Set<BlockPos> component = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.offer(start);
        component.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // Check all adjacent positions
            for (Direction dir : Direction.values()) {
                BlockPos adjacent = current.relative(dir);

                // Skip if already visited or not in available positions
                if (component.contains(adjacent) || !availablePositions.contains(adjacent)) {
                    continue;
                }

                // Check if positions can connect to each other
                if (canPositionsConnect(level, current, adjacent)) {
                    component.add(adjacent);
                    queue.offer(adjacent);
                }
            }
        }

        return component;
    }

    /**
     * Checks if two positions can connect to each other for network purposes.
     */
    private boolean canPositionsConnect(Level level, BlockPos pos1, BlockPos pos2) {
        BlockState state1 = level.getBlockState(pos1);
        BlockState state2 = level.getBlockState(pos2);

        // Both must be power-connectable
        return WireBlock.canConnectTo(state1, true) && WireBlock.canConnectTo(state2, true);
    }

    /**
     * Gets all positions adjacent to the given position that are part of power networks.
     */
    public Set<BlockPos> getAdjacentNetworkPositions(Level level, BlockPos pos) {
        Set<BlockPos> adjacent = new HashSet<>();

        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = pos.relative(dir);
            if (positionToNetwork.containsKey(adjacentPos) &&
                    WireBlock.canConnectTo(level.getBlockState(adjacentPos), true)) {
                adjacent.add(adjacentPos);
            }
        }

        return adjacent;
    }

    /**
     * Rebuilds network connectivity for a specific area. Useful for handling complex scenarios
     * where multiple blocks are placed/removed simultaneously.
     */
    public void rebuildNetworksInArea(ServerLevel level, Set<BlockPos> positions) {
        // Remove all positions from their current networks
        Set<UUID> affectedNetworks = new HashSet<>();
        for (BlockPos pos : positions) {
            UUID networkId = positionToNetwork.get(pos);
            if (networkId != null) {
                affectedNetworks.add(networkId);
                removeFromNetwork(pos);
            }
        }

        // Clean up empty networks
        affectedNetworks.removeIf(networkId -> !networks.containsKey(networkId));

        // Find all valid wire positions in the area
        Set<BlockPos> validPositions = positions.stream()
                .filter(pos -> WireBlock.canConnectTo(level.getBlockState(pos), true))
                .collect(Collectors.toSet());

        // Rebuild networks by finding connected components
        List<Set<BlockPos>> components = findConnectedComponents(level, validPositions);

        for (Set<BlockPos> component : components) {
            PowerBENetwork network = createNetwork();
            for (BlockPos pos : component) {
                addToNetwork(pos, network);
            }
        }
    }

    public Set<BlockPos> getNetworkPositions(UUID networkId) {
        return networkPositions.getOrDefault(networkId, Collections.emptySet());
    }

    public Collection<PowerBENetwork> getAllNetworks() {
        return networks.values();
    }

    /**
     * Debug method to get network statistics.
     */
    public String getNetworkStats() {
        return String.format("Networks: %d, Total positions: %d, Average network size: %.2f",
                networks.size(),
                positionToNetwork.size(),
                networks.isEmpty() ? 0.0 : (double) positionToNetwork.size() / networks.size());
    }
}

package com.arrl.radiocraft.common.data;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.blockentities.SolarPanelBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
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

    /**
     * Unified power distribution method for producers, consumers, and storage devices.
     * Handles both internal Radiocraft power sharing and external mod integration.
     *
     * @param level The server level
     * @param sourcePos The position of the block distributing power
     * @param availableEnergy The amount of energy available to distribute
     * @param maxOutput The maximum energy that can be output per tick
     * @return The amount of energy actually transferred
     */
    public int distributePowerUnified(ServerLevel level, BlockPos sourcePos, int availableEnergy, int maxOutput) {
        if (availableEnergy <= 0 || maxOutput <= 0) return 0;

        int totalTransferred = 0;
        int energyToDistribute = Math.min(availableEnergy, maxOutput);

        // Get adjacent positions in all 6 directions for external mod integration
        List<BlockPos> adjacentPositions = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            adjacentPositions.add(sourcePos.relative(dir));
        }

        // Shuffle for even distribution
        Collections.shuffle(adjacentPositions);

        // First, try to distribute to external mod blocks (adjacent positions)
        int remainingEnergy = energyToDistribute;
        int energyPerDirection = energyToDistribute / 6; // Even distribution across 6 directions
        int extraEnergy = energyToDistribute % 6; // Handle remainder

        for (int i = 0; i < adjacentPositions.size() && remainingEnergy > 0; i++) {
            BlockPos adjacentPos = adjacentPositions.get(i);
            BlockEntity adjacentBE = level.getBlockEntity(adjacentPos);

            if (adjacentBE != null) {
                // Check if it's a Radiocraft power block - if so, skip for now (handle via network)
                if (isRadiocraftPowerBlock(adjacentBE)) {
                    continue;
                }

                // Try to push to external mod blocks
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjacentPos, null);
                if (storage != null && storage.canReceive()) {
                    int energyForThisDirection = energyPerDirection + (i < extraEnergy ? 1 : 0);
                    int actualAmount = Math.min(energyForThisDirection, remainingEnergy);

                    int transferred = storage.receiveEnergy(actualAmount, false);
                    totalTransferred += transferred;
                    remainingEnergy -= transferred;
                }
            }
        }

        // Then, distribute remaining energy within the network to Radiocraft blocks
        if (remainingEnergy > 0) {
            PowerBENetwork network = getNetwork(sourcePos);
            if (network != null) {
                Set<BlockPos> networkPositions = getNetworkPositions(network.getUUID());
                List<BlockPos> consumers = new ArrayList<>();

                // Find consumers in the network (excluding self)
                for (BlockPos pos : networkPositions) {
                    if (pos.equals(sourcePos)) continue;

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                        if (storage != null && storage.canReceive()) {
                            consumers.add(pos);
                        }
                    }
                }

                if (!consumers.isEmpty()) {
                    // Shuffle consumers for even distribution
                    Collections.shuffle(consumers);

                    int energyPerConsumer = remainingEnergy / consumers.size();
                    int networkExtraEnergy = remainingEnergy % consumers.size();

                    for (int i = 0; i < consumers.size() && remainingEnergy > 0; i++) {
                        BlockPos consumerPos = consumers.get(i);
                        BlockEntity be = level.getBlockEntity(consumerPos);

                        if (be != null) {
                            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, consumerPos, null);
                            if (storage != null && storage.canReceive()) {
                                int energyForThisConsumer = energyPerConsumer + (i < networkExtraEnergy ? 1 : 0);
                                int actualAmount = Math.min(energyForThisConsumer, remainingEnergy);

                                int transferred = storage.receiveEnergy(actualAmount, false);
                                totalTransferred += transferred;
                                remainingEnergy -= transferred;
                            }
                        }
                    }
                }
            }
        }

        return totalTransferred;
    }

    /**
     * Checks if a block entity is a Radiocraft power block that should use internal power sharing.
     */
    private boolean isRadiocraftPowerBlock(BlockEntity blockEntity) {
        return blockEntity instanceof SolarPanelBlockEntity ||
                blockEntity instanceof LargeBatteryBlockEntity ||
                blockEntity.getBlockState().getBlock() == RadiocraftBlocks.WIRE.get() ||
                blockEntity.getBlockState().getBlock() == RadiocraftBlocks.WATERPROOF_WIRE.get() ||
                blockEntity.getBlockState().getBlock() == RadiocraftBlocks.CHARGE_CONTROLLER.get();
    }

    /**
     * Producer method for blocks that generate energy (like solar panels).
     *
     * @param level The server level
     * @param producerPos The position of the producer block
     * @param availableEnergy The amount of energy available to distribute
     * @param maxOutput The maximum energy output per tick
     * @return The amount of energy actually distributed
     */
    public int distributeProducerPower(ServerLevel level, BlockPos producerPos, int availableEnergy, int maxOutput) {
        return distributePowerUnified(level, producerPos, availableEnergy, maxOutput);
    }

    /**
     * Storage method for blocks that store and provide energy (like batteries).
     *
     * @param level The server level
     * @param storagePos The position of the storage block
     * @param availableEnergy The amount of energy available to distribute
     * @param maxOutput The maximum energy output per tick
     * @return The amount of energy actually distributed
     */
    public int distributeStoragePower(ServerLevel level, BlockPos storagePos, int availableEnergy, int maxOutput) {
        return distributePowerUnified(level, storagePos, availableEnergy, maxOutput);
    }

    /**
     * Consumer method for blocks that consume energy and may need to pull from nearby sources.
     *
     * @param level The server level
     * @param consumerPos The position of the consumer block
     * @param energyNeeded The amount of energy needed
     * @return The amount of energy actually received
     */
    public int pullConsumerPower(ServerLevel level, BlockPos consumerPos, int energyNeeded) {
        if (energyNeeded <= 0) return 0;

        int totalReceived = 0;
        int remainingNeeded = energyNeeded;

        // First try to pull from adjacent external mod blocks
        List<BlockPos> adjacentPositions = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            adjacentPositions.add(consumerPos.relative(dir));
        }

        // Shuffle for even distribution
        Collections.shuffle(adjacentPositions);

        int energyPerDirection = energyNeeded / 6;
        int extraEnergy = energyNeeded % 6;

        for (int i = 0; i < adjacentPositions.size() && remainingNeeded > 0; i++) {
            BlockPos adjacentPos = adjacentPositions.get(i);
            BlockEntity adjacentBE = level.getBlockEntity(adjacentPos);

            if (adjacentBE != null && !isRadiocraftPowerBlock(adjacentBE)) {
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, adjacentPos, null);
                if (storage != null && storage.canExtract()) {
                    int energyForThisDirection = energyPerDirection + (i < extraEnergy ? 1 : 0);
                    int actualAmount = Math.min(energyForThisDirection, remainingNeeded);

                    int extracted = storage.extractEnergy(actualAmount, false);
                    totalReceived += extracted;
                    remainingNeeded -= extracted;
                }
            }
        }

        // Then try to pull from network sources
        if (remainingNeeded > 0) {
            PowerBENetwork network = getNetwork(consumerPos);
            if (network != null) {
                Set<BlockPos> networkPositions = getNetworkPositions(network.getUUID());
                List<BlockPos> sources = new ArrayList<>();

                // Find energy sources in the network (excluding self)
                for (BlockPos pos : networkPositions) {
                    if (pos.equals(consumerPos)) continue;

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                        if (storage != null && storage.canExtract() && storage.getEnergyStored() > 0) {
                            sources.add(pos);
                        }
                    }
                }

                if (!sources.isEmpty()) {
                    Collections.shuffle(sources);

                    int energyPerSource = remainingNeeded / sources.size();
                    int networkExtraEnergy = remainingNeeded % sources.size();

                    for (int i = 0; i < sources.size() && remainingNeeded > 0; i++) {
                        BlockPos sourcePos = sources.get(i);
                        BlockEntity be = level.getBlockEntity(sourcePos);

                        if (be != null) {
                            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, null);
                            if (storage != null && storage.canExtract()) {
                                int energyForThisSource = energyPerSource + (i < networkExtraEnergy ? 1 : 0);
                                int actualAmount = Math.min(energyForThisSource, remainingNeeded);

                                int extracted = storage.extractEnergy(actualAmount, false);
                                totalReceived += extracted;
                                remainingNeeded -= extracted;
                            }
                        }
                    }
                }
            }
        }

        return totalReceived;
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
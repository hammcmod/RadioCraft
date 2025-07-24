package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.blocks.AbstractPowerNetworkBlock;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.HashSet;
import java.util.Set;

public enum EnhancedNetworkDebugProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Block block = accessor.getBlock();
        CompoundTag serverData = accessor.getServerData();

        // Network object info
        boolean hasNetworkObject = serverData.getBoolean("has_network_object");
        tooltip.add(Component.literal("§7Network Object: " + (hasNetworkObject ? "§aYes" : "§cNo")));

        // Show BE_NETWORKS capability status
        boolean hasCapability = serverData.getBoolean("has_capability");
        tooltip.add(Component.literal("§7BE_NETWORKS Cap: " + (hasCapability ? "§aYes" : "§cNo")));

        // Network size info
        if (serverData.contains("network_size")) {
            tooltip.add(Component.literal("§7Network Size: " + serverData.getInt("network_size")));
        }

        // Power info if available
        if (serverData.contains("power_stored")) {
            int powerStored = serverData.getInt("power_stored");
            int powerCapacity = serverData.getInt("power_capacity");
            tooltip.add(Component.literal(String.format("§7Power: §e%d/%d RF", powerStored, powerCapacity)));
        }

        // Connection validation
        tooltip.add(Component.literal("§7Power Tag: " +
                (serverData.getBoolean("power_tagged") ? "§aYes" : "§cNo")));
        tooltip.add(Component.literal("§7Coax Tag: " +
                (serverData.getBoolean("coax_tagged") ? "§aYes" : "§cNo")));

        // Debug adjacent blocks
        tooltip.add(Component.literal("§7Connected Blocks:"));
        for (Direction dir : Direction.values()) {
            String blockName = serverData.getString("adjacent_" + dir.getName());
            boolean isWire = serverData.getBoolean("is_wire_" + dir.getName());
            boolean isValidConnection = serverData.getBoolean("valid_" + dir.getName());
            boolean powerTagged = serverData.getBoolean("power_tagged_" + dir.getName());
            boolean coaxTagged = serverData.getBoolean("coax_tagged_" + dir.getName());
            boolean hasAdjacentNetworkObject = serverData.getBoolean("network_obj_" + dir.getName());
            boolean hasAdjacentCapability = serverData.getBoolean("capability_" + dir.getName());

            if (!blockName.isEmpty()) {
                String status = "";
                if (isWire) {
                    status += "§b[WIRE]";
                }
                if (isValidConnection) status += "§a[VALID]";
                if (powerTagged) status += "§b[POWER TAGGED]";
                if (coaxTagged) status += "§b[COAX TAGGED]";
                if (hasAdjacentNetworkObject) status += "§e[NET_OBJ]";
                if (hasAdjacentCapability) status += "§d[CAP]";

                if (status.isEmpty()) status = "§c[NO_CONNECT]";

                tooltip.add(Component.literal("  " + dir.getName() + ": " + blockName + " " + status));
            }
        }

        // Show network IDs if present
        Set<String> networkIds = new HashSet<>();
        for (Direction dir : Direction.values()) {
            if (serverData.contains("network_id_" + dir.getName())) {
                String networkId = serverData.getString("network_id_" + dir.getName());
                if (!networkId.isEmpty()) {
                    networkIds.add(networkId);
                }
            }
        }

        if (!networkIds.isEmpty()) {
            tooltip.add(Component.literal("§7Network IDs:"));
            for (String id : networkIds) {
                tooltip.add(Component.literal("  §e" + id));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var level = accessor.getLevel();
        var pos = accessor.getPosition();
        var state = accessor.getBlockState();

        // Check if this block is tagged properly
        boolean powerTagged = state.is(RadiocraftTags.Blocks.POWER_BLOCKS);
        data.putBoolean("power_tagged", powerTagged);
        boolean coaxTagged = state.is(RadiocraftTags.Blocks.COAX_BLOCKS);
        data.putBoolean("coax_tagged", coaxTagged);

        // Check for BE_NETWORKS capability
        boolean hasCapability = RadiocraftCapabilities.BE_NETWORKS.getCapability(level, pos, null, null, null) != null;
        data.putBoolean("has_capability", hasCapability);

        // Check for network object
        BENetworkObject networkObj = IBENetworks.getObject(level, pos);
        boolean hasNetworkObject = networkObj instanceof PowerNetworkObject;
        data.putBoolean("has_network_object", hasNetworkObject);

        // Try to get network size if possible
        if (networkObj != null) {
            int totalNetworkSize = 0;
            for (Direction dir : Direction.values()) {
                BENetwork network = networkObj.getNetwork(dir);
                if (network != null) {
                    totalNetworkSize = Math.max(totalNetworkSize, network.getNetworkObjects().size());
                    // Store network ID for this direction
                    data.putString("network_id_" + dir.getName(), network.getUUID().toString());
                }
            }
            if (totalNetworkSize > 0) {
                data.putInt("network_size", totalNetworkSize);
            }

            // Get power info if this is a power network object
            if (networkObj instanceof PowerNetworkObject powerNetObj) {
                data.putInt("power_stored", powerNetObj.getStorage().getEnergyStored());
                data.putInt("power_capacity", powerNetObj.getStorage().getMaxEnergyStored());
            }
        }

        // Analyze adjacent blocks
        for (Direction dir : Direction.values()) {
            var adjacentPos = pos.relative(dir);
            var adjacentState = level.getBlockState(adjacentPos);
            var adjacentBlock = adjacentState.getBlock();

            // Store block name
            String blockName = adjacentBlock.getDescriptionId();
            data.putString("adjacent_" + dir.getName(), blockName);

            // Check if it's a wire
            boolean isWire = adjacentBlock instanceof WireBlock && ((WireBlock) adjacentBlock).isPower;
            data.putBoolean("is_wire_" + dir.getName(), isWire);

            // Check if this block is tagged
            boolean powerTagged2 = adjacentState.is(RadiocraftTags.Blocks.POWER_BLOCKS);
            boolean coaxTagged2 = adjacentState.is(RadiocraftTags.Blocks.COAX_BLOCKS);
            data.putBoolean("power_tagged_" + dir.getName(), powerTagged2);
            data.putBoolean("coax_tagged_" + dir.getName(), coaxTagged2);

            // Check for BE_NETWORKS capability on adjacent block
            boolean hasAdjacentCapability = RadiocraftCapabilities.BE_NETWORKS.getCapability(level, adjacentPos, null, null, null) != null;
            data.putBoolean("capability_" + dir.getName(), hasAdjacentCapability);

            // Check if it has a network object
            BENetworkObject adjacentNetworkObj = IBENetworks.getObject(level, adjacentPos);
            boolean hasAdjacentNetworkObject = adjacentNetworkObj instanceof PowerNetworkObject;
            data.putBoolean("network_obj_" + dir.getName(), hasAdjacentNetworkObject);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("enhanced_network_debug");
    }
}
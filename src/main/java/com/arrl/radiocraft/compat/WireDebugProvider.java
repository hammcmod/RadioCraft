package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.blocks.WireBlock;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Set;

public enum WireDebugProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlock() instanceof WireBlock wireBlock)) return;

        CompoundTag serverData = accessor.getServerData();

        // Basic wire info
        tooltip.add(Component.literal("§6Wire Debug Info:"));
        tooltip.add(Component.literal("§7Type: " + (wireBlock.isPower ? "§cPower" : "§9Coaxial")));

        // Connection info
        tooltip.add(Component.literal("§7Connections:"));
        for (Direction dir : Direction.values()) {
            boolean connected = serverData.getBoolean("connected_" + dir.getName());
            String symbol = connected ? "§a✓" : "§c✗";
            tooltip.add(Component.literal("  " + symbol + " " + dir.getName()));
        }

        // Network info
        if (serverData.contains("network_size")) {
            tooltip.add(Component.literal("§7Network Size: " + serverData.getInt("network_size")));
        }

        if (serverData.contains("multiple_sides")) {
            boolean multipleSides = serverData.getBoolean("multiple_sides");
            tooltip.add(Component.literal("§7Multiple Sides: " + (multipleSides ? "§aYes" : "§cNo")));
        }

        // Debug adjacent blocks
        tooltip.add(Component.literal("§7Adjacent Blocks:"));
        for (Direction dir : Direction.values()) {
            String blockName = serverData.getString("adjacent_" + dir.getName());
            boolean isWire = serverData.getBoolean("is_wire_" + dir.getName());
            boolean wireMatches = serverData.getBoolean("wire_matches_" + dir.getName());
            boolean isValidConnection = serverData.getBoolean("valid_" + dir.getName());
            boolean isTagged = serverData.getBoolean("tagged_" + dir.getName());
            boolean hasNetworkObject = serverData.getBoolean("network_obj_" + dir.getName());

            if (!blockName.isEmpty()) {
                String status = "";
                if (isWire) {
                    status += wireMatches ? "§a[WIRE_MATCH]" : "§c[WIRE_MISMATCH]";
                } else {
                    if (isValidConnection) status += "§a[VALID]";
                    if (isTagged) status += "§b[TAGGED]";
                    if (hasNetworkObject) status += "§e[NET_OBJ]";
                }
                if (status.isEmpty()) status = "§c[NO_CONNECT]";

                tooltip.add(Component.literal("  " + dir.getName() + ": " + blockName + " " + status));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlock() instanceof WireBlock wireBlock)) return;

        var level = accessor.getLevel();
        var pos = accessor.getPosition();
        var state = accessor.getBlockState();

        // Get connections
        Set<Direction> connections = wireBlock.getConnections(level, pos);
        for (Direction dir : Direction.values()) {
            data.putBoolean("connected_" + dir.getName(), connections.contains(dir));
        }

        // Check multiple sides
        boolean multipleSides = wireBlock.checkMultipleSides(level, pos);
        data.putBoolean("multiple_sides", multipleSides);

        // Debug adjacent blocks
        for (Direction dir : Direction.values()) {
            var adjacentPos = pos.relative(dir);
            var adjacentState = level.getBlockState(adjacentPos);
            var adjacentBlock = adjacentState.getBlock();

            // Store block name
            String blockName = adjacentBlock.getDescriptionId();
            data.putString("adjacent_" + dir.getName(), blockName);

            // Check if it's a wire
            boolean isWire = adjacentBlock instanceof WireBlock;
            data.putBoolean("is_wire_" + dir.getName(), isWire);

            if (isWire) {
                WireBlock adjacentWire = (WireBlock) adjacentBlock;
                boolean wireMatches = adjacentWire.isPower == wireBlock.isPower;
                data.putBoolean("wire_matches_" + dir.getName(), wireMatches);
            }

            // Check if it's a valid connection using the actual method
            boolean canConnect = WireBlock.canConnectTo(adjacentState, wireBlock.isPower);
            data.putBoolean("valid_" + dir.getName(), canConnect);

            // Check if it's properly tagged
            boolean isTagged = wireBlock.isPower ?
                    adjacentState.is(RadiocraftTags.Blocks.POWER_BLOCKS) :
                    adjacentState.is(RadiocraftTags.Blocks.COAX_BLOCKS);
            data.putBoolean("tagged_" + dir.getName(), isTagged);

            // Check if it has a network object
            BENetworkObject networkObj = IBENetworks.getObject(level, adjacentPos);
            boolean hasNetworkObject = false;
            if (networkObj != null) {
                if (wireBlock.isPower) {
                    hasNetworkObject = networkObj instanceof PowerNetworkObject;
                } else {
                    hasNetworkObject = networkObj instanceof ICoaxNetworkObject;
                }
            }
            data.putBoolean("network_obj_" + dir.getName(), hasNetworkObject);
        }

        // Try to get network size if possible - check each direction for networks
        BENetworkObject networkObj = IBENetworks.getObject(level, pos);
        if (networkObj != null) {
            int totalNetworkSize = 0;
            for (Direction dir : Direction.values()) {
                var network = networkObj.getNetwork(dir);
                if (network != null) {
                    totalNetworkSize = Math.max(totalNetworkSize, network.getNetworkObjects().size());
                }
            }
            if (totalNetworkSize > 0) {
                data.putInt("network_size", totalNetworkSize);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("wire_debug");
    }
}
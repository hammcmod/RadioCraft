package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public enum RadioProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("radioPowered")) {
            tooltip.add(Component.literal("Radio: Loading..."));
            return;
        }

        tooltip.add(Component.literal("Radio: " + (data.getBoolean("radioPowered") ? "On" : "Off")));
        tooltip.add(Component.literal(String.format("Frequency: %.3f MHz", data.getFloat("frequency") / 1_000_000.0F)));
        tooltip.add(Component.literal("Mic Volume: " + Math.round(data.getFloat("micGain") * 100.0F) + "%"));
        tooltip.add(Component.literal("Speaker Volume: " + Math.round(data.getFloat("speakerGain") * 100.0F) + "%"));

        int antennaCount = data.getInt("antennaCount");
        if(antennaCount == 0) {
            tooltip.add(Component.literal("Antenna: None connected"));
            if(data.contains("nearbyAntennaBlock")) {
                tooltip.add(Component.literal("Nearby Antenna Block: " + data.getString("nearbyAntennaBlock")));
                String nearbyAntennaType = data.getString("nearbyAntennaType");
                tooltip.add(Component.literal("Nearby Antenna Match: " + (nearbyAntennaType.isBlank() ? "No valid antenna" : nearbyAntennaType)));
            }
        }
        else if(antennaCount == 1) {
            String antennaType = data.getString("antennaType");
            tooltip.add(Component.literal("Antenna: " + (antennaType.isBlank() ? "Unknown" : antennaType)));
        }
        else {
            tooltip.add(Component.literal("Antennas: " + antennaCount + " connected"));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(!(blockEntity instanceof RadioBlockEntity radio))
            return;

        data.putFloat("frequency", radio.getFrequency());
        data.putFloat("micGain", radio.getMicGain());
        data.putFloat("speakerGain", radio.getSpeakerGain());

        if(radio.getNetworkObject(level, pos) instanceof RadioNetworkObject radioNetworkObject) {
            data.putBoolean("radioPowered", radioNetworkObject.isPowered);

            List<AntennaNetworkObject> antennas = getConnectedAntennas(level, pos, radioNetworkObject);
            data.putInt("antennaCount", antennas.size());
            if(antennas.size() == 1) {
                StaticAntenna<?> antenna = antennas.getFirst().getAntenna();
                if(antenna != null)
                    data.putString("antennaType", antenna.getType().getId().getPath());
            }
        }
        else {
            data.putBoolean("radioPowered", false);
            data.putInt("antennaCount", 0);
        }

        if(data.getInt("antennaCount") == 0)
            appendNearbyAntennaData(data, level, pos);
    }

    private static List<AntennaNetworkObject> getConnectedAntennas(Level level, BlockPos pos, RadioNetworkObject radioNetworkObject) {
        Set<AntennaNetworkObject> antennas = new HashSet<>(radioNetworkObject.getAntennas());
        Set<BlockPos> connectedObjects = new HashSet<>();
        CoaxWireProvider.collectConnections(level, pos, connectedObjects);
        for(BlockPos objectPos : connectedObjects) {
            BENetworkObject object = IBENetworks.getObject(level, objectPos);
            if(object instanceof AntennaNetworkObject antenna)
                antennas.add(antenna);
        }
        return List.copyOf(antennas);
    }

    private static void appendNearbyAntennaData(CompoundTag data, Level level, BlockPos pos) {
        for(Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if(!(level.getBlockEntity(checkPos) instanceof AntennaBlockEntity))
                continue;

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(checkPos).getBlock());
            data.putString("nearbyAntennaBlock", blockId.getPath());

            StaticAntenna<?> antenna = AntennaTypes.match(level, checkPos);
            if(antenna != null)
                data.putString("nearbyAntennaType", antenna.getType().getId().getPath());
            return;
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("radio");
    }
}

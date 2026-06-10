package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.HashSet;
import java.util.Set;

public enum CoaxWireProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("coaxRadioCount"))
            return;

        int radioCount = data.getInt("coaxRadioCount");
        tooltip.add(Component.literal("Connected Radios: " + radioCount));
        for(int i = 0; i < radioCount; i++)
            tooltip.add(Component.literal(" - " + data.getString("coaxRadio_" + i)));

        int antennaCount = data.getInt("coaxAntennaCount");
        tooltip.add(Component.literal("Connected Antennas: " + antennaCount));
        for(int i = 0; i < antennaCount; i++)
            tooltip.add(Component.literal(" - " + data.getString("coaxAntenna_" + i)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        if(level.getBlockState(pos).getBlock() != RadiocraftBlocks.COAX_WIRE.get())
            return;

        Set<BlockPos> visitedObjects = new HashSet<>();
        collectConnections(level, pos, visitedObjects);

        int radioCount = 0;
        int antennaCount = 0;
        for(BlockPos objectPos : visitedObjects) {
            BENetworkObject object = IBENetworks.getObject(level, objectPos);
            if(object instanceof RadioNetworkObject) {
                data.putString("coaxRadio_" + radioCount++, describeBlock(level, objectPos));
            }
            else if(object instanceof AntennaNetworkObject antennaObject) {
                data.putString("coaxAntenna_" + antennaCount++, describeAntenna(level, antennaObject));
            }
        }

        data.putInt("coaxRadioCount", radioCount);
        data.putInt("coaxAntennaCount", antennaCount);
    }

    public static void collectConnections(Level level, BlockPos pos, Set<BlockPos> visitedObjects) {
        Set<BlockPos> visitedWires = new HashSet<>();
        if(level.getBlockState(pos).getBlock() == RadiocraftBlocks.COAX_WIRE.get()) {
            collectWireConnections(level, pos, visitedWires, visitedObjects);
            return;
        }

        BENetworkObject origin = IBENetworks.getObject(level, pos);
        if(origin instanceof ICoaxNetworkObject)
            visitedObjects.add(pos);

        for(Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if(level.getBlockState(checkPos).getBlock() == RadiocraftBlocks.COAX_WIRE.get())
                collectWireConnections(level, checkPos, visitedWires, visitedObjects);
            else if(IBENetworks.getObject(level, checkPos) instanceof ICoaxNetworkObject)
                visitedObjects.add(checkPos);
        }
    }

    private static void collectWireConnections(Level level, BlockPos pos, Set<BlockPos> visitedWires, Set<BlockPos> visitedObjects) {
        if(!visitedWires.add(pos))
            return;

        for(Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if(level.getBlockState(checkPos).getBlock() == RadiocraftBlocks.COAX_WIRE.get()) {
                collectWireConnections(level, checkPos, visitedWires, visitedObjects);
                continue;
            }

            BENetworkObject object = IBENetworks.getObject(level, checkPos);
            if(object instanceof ICoaxNetworkObject)
                visitedObjects.add(checkPos);
        }
    }

    private static String describeBlock(Level level, BlockPos pos) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return blockId.getPath() + " at " + formatPos(pos);
    }

    private static String describeAntenna(Level level, AntennaNetworkObject antennaObject) {
        StaticAntenna<?> antenna = antennaObject.getAntenna();
        String type = antenna == null ? "unmatched" : antenna.getType().getId().getPath();
        return type + " (" + describeBlock(level, antennaObject.getPos()) + ")";
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("coax_wire");
    }
}

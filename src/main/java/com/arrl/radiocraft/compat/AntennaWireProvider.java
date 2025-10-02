package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.entities.AntennaWire;
import com.arrl.radiocraft.common.entities.IAntennaWire;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum AntennaWireProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        int wireCount = data.getInt("wires");
        if (wireCount != 0) {
            iTooltip.add(Component.literal("Attached Wires: " + wireCount));
            for (int i = 0; i < wireCount; i++) {
                double length = data.getDouble("wire_" + i);
                iTooltip.add(Component.literal(String.format("%.2f", length) + "m"));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        List<IAntennaWire> wires = AntennaWire.getWires(level, pos);
        data.putInt("wires", wires.size());
        for (int i = 0; i < wires.size(); i++) {
            data.putDouble("wire_" + i, wires.get(i).getLength());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("antenna_wire") ;
    }
}

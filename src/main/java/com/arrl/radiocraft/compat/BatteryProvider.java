package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BatteryProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        int lastTickTransferredRf = blockAccessor.getServerData().getInt("transferred_power");
        int currentStorage = blockAccessor.getServerData().getInt("current_storage");
        int maxStorage = blockAccessor.getServerData().getInt("max_storage");

        if (blockAccessor.getBlockEntity() instanceof LargeBatteryBlockEntity) {
            iTooltip.add(Component.literal(String.format("Power Transferred: %.2f Watts", lastTickTransferredRf / 8.0)));
            iTooltip.add(Component.literal(String.format("Current Storage: %.2f kJ", currentStorage / 8.0 / 1000.0)));
            iTooltip.add(Component.literal(String.format("Max Storage: %.2f kJ", maxStorage / 8.0 / 1000.0)));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof LargeBatteryBlockEntity && be.getLevel() != null) {
            int lastTickTransferredRf = ((LargeBatteryBlockEntity) be).getTransferredEnergyLastTick();
            int currentStorage = ((LargeBatteryBlockEntity) be).getEnergyStorage().getEnergyStored();
            int maxStorage = ((LargeBatteryBlockEntity) be).getEnergyStorage().getMaxEnergyStored();

            data.putInt("transferred_power", lastTickTransferredRf);
            data.putInt("current_storage", currentStorage);
            data.putInt("max_storage", maxStorage);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("large_battery_provider");
    }
}

package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.SolarPanelBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SolarPanelProvider  implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        int solarOutput = blockAccessor.getServerData().getInt("solar_output");
        double solarCoefficient = blockAccessor.getServerData().getDouble("solar_coefficient");
        double rainCoefficient = blockAccessor.getServerData().getDouble("rain_coefficient");
        if (blockAccessor.getBlockEntity() instanceof SolarPanelBlockEntity) {
            iTooltip.add(Component.literal(String.format("§7Solar Output: §e%.2f Watts", solarOutput / 8.0)));
            iTooltip.add(Component.literal(String.format("§7Panel Efficiency: §e%.2f %%", solarCoefficient * rainCoefficient * 100.0)));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof SolarPanelBlockEntity && be.getLevel() != null) {
            int solarOutput = SolarPanelBlockEntity.getSolarOutput(be.getLevel(), be.getBlockPos());
            data.putInt("solar_output", solarOutput);
            double solarCoefficient = SolarPanelBlockEntity.getSolarCoefficient(be.getLevel(), be.getBlockPos());
            data.putDouble("solar_coefficient", solarCoefficient);
            double rainCoefficient = SolarPanelBlockEntity.getRainCoefficient(be.getLevel());
            data.putDouble("rain_coefficient", rainCoefficient);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("solar_panel_provider");
    }
}
package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.*;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import static com.arrl.radiocraft.compat.InfoHelpers.getAntennaInfo;

public class JadeProbe implements IBlockComponentProvider {

    private static JadeProbe INSTANCE;

    public static JadeProbe getProbe() {
        if (INSTANCE == null) {
            INSTANCE = new JadeProbe();
        }
        return INSTANCE;
    }

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        switch (blockAccessor.getBlock()) {
            case YagiAntennaBlock b -> getAntennaInfo(b, blockAccessor.getLevel(), blockAccessor.getPosition()).forEach(iTooltip::add);
            case DoubleVHFAntennaBlock b -> getAntennaInfo(b, blockAccessor.getLevel(), blockAccessor.getPosition()).forEach(iTooltip::add);
            case BalunBlock b -> getAntennaInfo(b, blockAccessor.getLevel(), blockAccessor.getPosition()).forEach(iTooltip::add);
            case VHFAntennaCenterBlock b -> getAntennaInfo(b, blockAccessor.getLevel(), blockAccessor.getPosition()).forEach(iTooltip::add);
            case AntennaCenterBlock b -> getAntennaInfo(b, blockAccessor.getLevel(), blockAccessor.getPosition()).forEach(iTooltip::add);
            default -> {}
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, Radiocraft.MOD_ID);
    }
}

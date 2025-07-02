package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.*;
import mcjty.theoneprobe.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static com.arrl.radiocraft.compat.InfoHelpers.getAntennaInfo;

public class TopProbe implements IProbeInfoProvider {

    public static TopProbe getProbe() {
        if (instance == null) {
            instance = new TopProbe();
        }
        return instance;
    }

    private static TopProbe instance;

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, Radiocraft.MOD_ID);
    }

    /**
     * Adds information to TOP. Note - runs with server side level, no client info here.
     */
    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, Player player, Level level, BlockState blockState, IProbeHitData iProbeHitData) {
        switch (blockState.getBlock()) {
            case YagiAntennaBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos()).forEach(iProbeInfo::text);
            case DoubleVHFAntennaBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos()).forEach(iProbeInfo::text);
            case BalunBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos()).forEach(iProbeInfo::text);
            case VHFAntennaCenterBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos()).forEach(iProbeInfo::text);
            case AntennaCenterBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos()).forEach(iProbeInfo::text);
            default -> {}
        }
    }
}

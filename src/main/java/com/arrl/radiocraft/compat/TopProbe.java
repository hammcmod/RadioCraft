package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blocks.*;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import mcjty.theoneprobe.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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

    private void getAntennaInfo(AntennaCenterBlock block, Level level, BlockPos pos, IProbeInfo iProbeInfo) {
        if(IBENetworks.getObject(level, pos) instanceof AntennaNetworkObject networkObject) {
            StaticAntenna<?> antenna = networkObject.getAntenna();
            if(antenna != null)
                iProbeInfo.text("Antenna Type: " + antenna.type.toString());
            else
                iProbeInfo.text("No valid antenna found");
        } else {
            iProbeInfo.text("No valid antenna network found");
        }
    }

    /**
     * Adds information to TOP. Note - runs with server side level, no client info here.
     */
    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, Player player, Level level, BlockState blockState, IProbeHitData iProbeHitData) {
        switch (blockState.getBlock()) {
            case YagiAntennaBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos(), iProbeInfo);
            case DoubleVHFAntennaBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos(), iProbeInfo);
            case BalunBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos(), iProbeInfo);
            case VHFAntennaCenterBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos(), iProbeInfo);
            case AntennaCenterBlock b -> getAntennaInfo(b, level, iProbeHitData.getPos(), iProbeInfo);
            default -> {}
        }
    }
}

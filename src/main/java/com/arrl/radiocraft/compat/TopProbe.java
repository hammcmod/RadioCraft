package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import mcjty.theoneprobe.api.*;
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

    /**
     * Adds information to TOP. Note - runs with server side level, no client info here.
     */
    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, Player player, Level level, BlockState blockState, IProbeHitData iProbeHitData) {
        // Add data here.
    }
}

package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A direction finder module on its coax. It sits on the coax as a receiver does (so the antenna hands it what it
 * hears, see {@code AntennaNetworkObject#receiveAudioPacket}) but never transmits and draws no power.
 */
public class DirectionFinderModuleNetworkObject extends RadioNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("direction_finder_module");

    public DirectionFinderModuleNetworkObject(Level level, BlockPos pos) {
        super(level, pos, 0, 0);
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        isPowered = false; // Nothing to run: the direction finder it is attached to is the one switched on.
    }

    @Override
    public boolean isIndirectConsumer() {
        return false;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.IPassThroughNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A direction finder module on its coax. It sits on the coax as a receiver does (so the antenna hands it what it
 * hears, see {@code AntennaNetworkObject#receiveAudioPacket}) but never transmits and draws no power. On the power
 * side it is a conductor: a power wire run to any module reaches the finder and the other modules it touches, so the
 * finder need not have a free face for its own wire. Its coax it keeps to itself (each module has its own antenna).
 */
public class DirectionFinderModuleNetworkObject extends RadioNetworkObject implements IPassThroughNetworkObject {

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

    /** One node for all its sides on the power wire only. */
    @Override
    public boolean passesThrough(boolean power) {
        return power;
    }

    /** Touching modules and the finder share power; coax is never shared by touching. */
    @Override
    public boolean linksDirectlyTo(BENetworkObject other, boolean power) {
        return power && (other instanceof DirectionFinderModuleNetworkObject || other instanceof DirectionFinderNetworkObject);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

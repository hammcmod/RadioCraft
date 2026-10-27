package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * An Ionospheric Processor on its coax. It sits on the coax as a receiver does, so the Ionosonde Antenna at the other
 * end shows up in {@link #getAntennas()}, but it never transmits through it and draws no power: its bulletins go out
 * through the radio it touches.
 */
public class IonosphericProcessorNetworkObject extends RadioNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("ionospheric_processor");

    public IonosphericProcessorNetworkObject(Level level, BlockPos pos) {
        super(level, pos, 0, 0);
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        isPowered = false; // It never keys its own coax.
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

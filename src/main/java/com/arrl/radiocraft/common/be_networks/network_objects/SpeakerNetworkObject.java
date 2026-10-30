package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A wired speaker (wall speaker, loudspeaker) as it was on a power wire. Since 2026-10-30 its line is data wire
 * ({@code DataWire}, {@code RadioBlockEntity#refreshSpeakers}) and power wire no longer runs into it; the object stays
 * registered so worlds that saved it still load.
 */
public class SpeakerNetworkObject extends PowerNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("speaker");

    public SpeakerNetworkObject(Level level, BlockPos pos) {
        super(level, pos, 0, 0, 0);
    }

    @Override
    public boolean isIndirectConsumer() {
        return false;
    }

    @Override
    public boolean isDirectConsumer() {
        return false;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

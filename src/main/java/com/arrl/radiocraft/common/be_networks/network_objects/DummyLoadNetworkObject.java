package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The antenna end of a dummy load. It screws straight onto a radio (joins one it touches, no coax needed) as well as
 * taking coax, and as it works on every band it sits on whichever antenna network, HF or VHF, its radio is on.
 */
public class DummyLoadNetworkObject extends BandFollowingAntennaNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("dummy_load");

    public DummyLoadNetworkObject(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Override
    public boolean linksDirectlyTo(BENetworkObject other) {
        return other instanceof RadioNetworkObject;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

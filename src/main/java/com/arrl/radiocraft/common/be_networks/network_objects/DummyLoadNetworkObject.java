package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The antenna end of a dummy load. It screws straight onto a radio (joins one it touches, no coax needed) as well as
 * taking coax, and as it works on every band it sits on whichever antenna network, HF or VHF, its radio is on.
 */
public class DummyLoadNetworkObject extends AntennaNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("dummy_load");

    public DummyLoadNetworkObject(Level level, BlockPos pos) {
        super(level, pos, AntennaNetworkManager.HF_ID);
    }

    @Override
    public boolean linksDirectlyTo(BENetworkObject other) {
        return other instanceof RadioNetworkObject;
    }

    /** Moves to the antenna network of the band its one radio is on. With no radio, or several, it stays put. */
    public void followRadioBand() {
        List<RadioNetworkObject> radios = getRadios();
        if(radios.size() != 1 || level == null || !level.isLoaded(radios.getFirst().getPos()))
            return;
        if(level.getBlockEntity(radios.getFirst().getPos()) instanceof RadioBlockEntity radio) {
            Band band = radio.getBand();
            if(band != null)
                setNetworkId(band.isVhf() ? AntennaNetworkManager.VHF_ID : AntennaNetworkManager.HF_ID);
        }
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

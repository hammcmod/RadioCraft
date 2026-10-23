package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The antenna end of an antenna that works (well or badly) on every band, such as the dummy load and the ionosonde: it
 * sits on whichever antenna network, HF or VHF, its radio is on, so an HF radio sees its HF SWR and a VHF radio its
 * VHF SWR. Its block entity calls {@link #followRadioBand} now and then, since an all-band radio can change band.
 */
public class BandFollowingAntennaNetworkObject extends AntennaNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("band_following_antenna");

    public BandFollowingAntennaNetworkObject(Level level, BlockPos pos) {
        super(level, pos, AntennaNetworkManager.HF_ID);
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

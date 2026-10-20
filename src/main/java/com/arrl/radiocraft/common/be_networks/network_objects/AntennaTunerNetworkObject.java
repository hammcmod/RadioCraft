package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The antenna tuner's presence on a coax run. It has no state of its own: {@link AntennaNetworkObject} notices a tuner
 * sharing its coax network and applies the match (see {@link AntennaNetworkObject#getSWR}), so a tuner anywhere on the
 * run between a radio and its antenna counts. The wire walk passes through it ({@link ICoaxPassThrough}), which is
 * what keeps the radio and the antenna on one network. The accessors here are for the tuner's own readout.
 */
public class AntennaTunerNetworkObject extends BENetworkObject implements ICoaxPassThrough {

    public static final ResourceLocation TYPE = Radiocraft.id("antenna_tuner");

    public AntennaTunerNetworkObject(Level level, BlockPos pos) {
        super(level, pos);
    }

    /** Every radio on a coax run this tuner is part of. */
    public List<RadioNetworkObject> getRadios() {
        List<RadioNetworkObject> radios = new ArrayList<>();
        for(BENetworkObject object : coaxObjects()) {
            if(object instanceof RadioNetworkObject radio)
                radios.add(radio);
        }
        return radios;
    }

    /** Every antenna (recognized or not) on a coax run this tuner is part of. */
    public List<AntennaNetworkObject> getAntennas() {
        List<AntennaNetworkObject> antennas = new ArrayList<>();
        for(BENetworkObject object : coaxObjects()) {
            if(object instanceof AntennaNetworkObject antenna)
                antennas.add(antenna);
        }
        return antennas;
    }

    private Set<BENetworkObject> coaxObjects() {
        Set<BENetworkObject> objects = new LinkedHashSet<>();
        for(Direction side : Direction.values()) {
            BENetwork network = getNetwork(side);
            if(network != null && network.getType() == BENetwork.COAXIAL_TYPE)
                objects.addAll(network.getNetworkObjects());
        }
        return objects;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}

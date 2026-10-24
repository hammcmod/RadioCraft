package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.radio.SWRHelper;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An antenna tuner on a coax run: the manual one ({@link AntennaTunerNetworkObject}) or the automatic one
 * ({@link AutoTunerNetworkObject}). {@link AntennaNetworkObject} asks the tuner sharing its coax what the radio sees.
 * May be called from the voice thread, so implementations read only volatile state.
 */
public interface ITuner {

    /**
     * What a radio on this run sees through the tuner.
     *
     * @param swr The SWR the radio sees.
     * @param efficiency Share of the transmit power that reaches the antenna.
     */
    record Match(double swr, double efficiency) {
        /** The tuner is not in the circuit (off, bypassed, gave up): the antenna as it is. */
        public static Match bypass(double rawSwr) {
            return new Match(rawSwr, SWRHelper.getEfficiencyMultiplier(rawSwr));
        }
    }

    /** The match for {@code antenna}, which presents {@code rawSwr} at {@code frequencyHertz}. */
    Match match(AntennaNetworkObject antenna, double rawSwr, float frequencyHertz);

    /** True when this is an automatic tuner that has given up on the antenna (the radio shows TUNE FAIL). */
    default boolean isTuneFailed() {
        return false;
    }

    /** Every radio on a coax run this tuner is part of. */
    default List<RadioNetworkObject> getRadios() {
        List<RadioNetworkObject> radios = new ArrayList<>();
        for(BENetworkObject object : coaxObjects()) {
            if(object instanceof RadioNetworkObject radio)
                radios.add(radio);
        }
        return radios;
    }

    /** Every antenna (recognized or not) on a coax run this tuner is part of. */
    default List<AntennaNetworkObject> getAntennas() {
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
            BENetwork network = ((BENetworkObject)this).getNetwork(side);
            if(network != null && network.getType() == BENetwork.COAXIAL_TYPE)
                objects.addAll(network.getNetworkObjects());
        }
        return objects;
    }

}

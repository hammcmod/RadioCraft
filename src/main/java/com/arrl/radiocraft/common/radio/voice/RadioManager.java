package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.common.radio.antenna.BERadioNetwork;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class RadioManager {

    private static final Map<Level, BERadioNetwork> NETWORKS = new HashMap<>();

    public static BERadioNetwork getNetwork(Level level) {
        if(!NETWORKS.containsKey(level))
            setNetwork(level, new BERadioNetwork());
        return NETWORKS.get(level);
    }

    public static void setNetwork(Level level, BERadioNetwork network) {
        NETWORKS.put(level, network);
    }

}
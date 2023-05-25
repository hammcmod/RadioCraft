package com.arrl.radiocraft.common.radio.voice;

import com.arrl.radiocraft.common.radio.RadioNetwork;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class RadioManager {

    private static final Map<Level, RadioNetwork> NETWORKS = new HashMap<>();

    public static RadioNetwork getNetwork(Level level) {
        if(!NETWORKS.containsKey(level))
            setNetwork(level, new RadioNetwork());
        return NETWORKS.get(level);
    }

    public static void setNetwork(Level level, RadioNetwork network) {
        NETWORKS.put(level, network);
    }

}
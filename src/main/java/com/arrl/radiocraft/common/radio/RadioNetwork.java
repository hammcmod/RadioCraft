package com.arrl.radiocraft.common.radio;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioNetwork {

    private final Map<BlockPos, Radio> radios = new ConcurrentHashMap<>(); // Concurrent map as it is read by the VoiP thread

    /**
     * Do not call this from the VoiP thread
     */
    public void putRadio(BlockPos pos, Radio radio) {
        radios.put(pos, radio);
    }

    /**
     * Do not call this from the VoiP thread
     */
    public void removeRadio(BlockPos pos) {
        radios.remove(pos);
    }

    public Radio getRadio(BlockPos pos) {
        return radios.get(pos);
    }

    public Map<BlockPos, Radio> allRadios() {
        return radios;
    }


}
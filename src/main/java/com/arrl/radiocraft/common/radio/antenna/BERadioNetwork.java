package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BERadioNetwork {

    private final Map<BlockPos, BEVoiceReceiver> radios = new ConcurrentHashMap<>(); // Concurrent map as it is read by the VoiP thread

    /**
     * Do not call this from the VoiP thread
     */
    public void putRadio(BlockPos pos, BEVoiceReceiver radio) {
        radios.put(pos, radio);
    }

    /**
     * Do not call this from the VoiP thread
     */
    public void removeRadio(BlockPos pos) {
        radios.remove(pos);
    }

    public BEVoiceReceiver getRadio(BlockPos pos) {
        return radios.get(pos);
    }

    public Map<BlockPos, BEVoiceReceiver> allRadios() {
        return radios;
    }


}
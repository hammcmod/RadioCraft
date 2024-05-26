package com.arrl.radiocraft.common.be_networks.network_objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class HFRadioNetworkObject extends RadioNetworkObject {

    public HFRadioNetworkObject(Level level, BlockPos pos, int transmitUse, int receiveUse) {
        super(level, pos, transmitUse, receiveUse);
    }

}

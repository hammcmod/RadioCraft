package com.arrl.radiocraft.common.be_networks.network_objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * {@link RepeaterNetworkObject} contains logic for receiving and repeating a signal since this needs to work while
 * the {@link BlockEntity} isn't loaded.
 */
public class RepeaterNetworkObject extends RadioNetworkObject {

    public RepeaterNetworkObject(Level level, BlockPos pos, int transmitUse, int receiveUse) {
        super(level, pos, transmitUse, receiveUse);
    }

}

package com.arrl.radiocraft.api.capabilities;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Capability attached to a block entity concerning callsign/license information for automatic transmissions
 */
public interface IBlockEntityCallsignCapability {
    PlayerCallsignData getCallsignData(Level level, BlockPos pos, BlockEntityCallsignData callsignData);
    IPlayerCallsignCapability setCallsignData(Level level, BlockPos pos, BlockEntityCallsignData callsignData);
    IPlayerCallsignCapability resetCallsign(Level level, BlockPos pos);
}

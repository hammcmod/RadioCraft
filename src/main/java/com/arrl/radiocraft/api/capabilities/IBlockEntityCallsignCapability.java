package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.GlobalPos;

import java.util.ArrayList;

/**
 * Capability attached to a block entity concerning callsign/license information for automatic transmissions
 */
public interface IBlockEntityCallsignCapability {
    ArrayList<String> getCallsigns();
    BlockEntityCallsignData getCallsignData(String callsign);
    BlockEntityCallsignData getCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData);
    IBlockEntityCallsignCapability setCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData);
    IBlockEntityCallsignCapability resetCallsign(GlobalPos pos);
}

package com.arrl.radiocraft.api.capabilities;

import net.minecraft.core.GlobalPos;

import java.util.ArrayList;

/**
 * Capability attached to a block entity concerning callsign/license information for automatic transmissions
 */
public interface IBlockEntityCallsignCapability {
    /**
     * Get all callsigns stored.
     * @return ArrayList of all callsigns stored.
     */
    ArrayList<String> getCallsigns();

    /**
     * Get the callsign data for a target callsign.
     * @param callsign The callsign to get the data for.
     * @return The callsign data associated with the target callsign.
     */
    BlockEntityCallsignData getCallsignData(String callsign);

    /**
     * Get the callsign data for a target block entity.
     * @param pos The {@link GlobalPos} of the target block entity.
     * @param callsignData The callsign data to be used.
     * @return The callsign data being used.
     */
    BlockEntityCallsignData getCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData);

    /**
     * Set the callsign data for a target block entity.
     * @param pos The {@link GlobalPos} of the target block entity.
     * @param callsignData The callsign data to be used.
     * @return The callsign data being set.
     */
    IBlockEntityCallsignCapability setCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData);

    /**
     * Remove the callsign associated with a target block entity.
     * @param pos The {@link GlobalPos} of the target block entity.
     * @return The callsign data being removed.
     */
    IBlockEntityCallsignCapability resetCallsign(GlobalPos pos);
}

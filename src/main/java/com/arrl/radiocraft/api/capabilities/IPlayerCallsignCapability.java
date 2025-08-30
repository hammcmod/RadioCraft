package com.arrl.radiocraft.api.capabilities;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Capability attached to the player concerning callsign/license information
 */
public interface IPlayerCallsignCapability {

	/**
	 * @return All callsigns stored
	 */
	ArrayList<String> getCallsigns();

	/**
	 * @param callsign Callsign belonging to the target player
	 * @return The callsign data assicated with the target player (if exists)
	 */
	PlayerCallsignData getCallsignData(String callsign);

	/**
	 * @param playerUUID {@link UUID} belonging to the target player.
	 * @return The callsign {@link String} associated with the provided {@link UUID}.
	 */
	PlayerCallsignData getCallsignData(UUID playerUUID);

	/**
	 * Set the callsign string associated with a certain player.
	 * @param playerUUID The {@link UUID} of the target player.
	 * @param playerCallsignData The callsign data to be used.
	 */
	IPlayerCallsignCapability setCallsignData(UUID playerUUID, PlayerCallsignData playerCallsignData);

	/**
	 * Remove the callsign associated with a target player.
	 * @param playerUUID The {@link UUID} of the target player.
	 */
	IPlayerCallsignCapability resetCallsign(UUID playerUUID);
}

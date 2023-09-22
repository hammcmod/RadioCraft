package com.arrl.radiocraft.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

/**
 * Capability attached to {@link Level} containing callsign data for all players. This cap will only be present on the
 * overworld (Key: {@link Level#OVERWORLD}).
 */
@AutoRegisterCapability
public interface ICallsignCapability extends INBTSerializable<CompoundTag> {

	/**
	 * @param playerUUID {@link UUID} belonging to the target player.
	 * @return The callsign {@link String} associated with the provided {@link UUID}.
	 */
	String getCallsign(UUID playerUUID);

	/**
	 * Set the callsign string associated with a certain player.
	 * @param playerUUID The {@link UUID} of the target player.
	 * @param callsign The callsign to be used.
	 */
	void setCallsign(UUID playerUUID, String callsign);

	/**
	 * Remove the callsign associated with a target player.
	 * @param playerUUID The {@link UUID} of the target player.
	 */
	void resetCallsign(UUID playerUUID);

}

package com.arrl.radiocraft.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

@AutoRegisterCapability
public interface ICallsignCapability extends INBTSerializable<CompoundTag> {
	String getCallsign(UUID playerUUID);
	void setCallsign(UUID playerUUID, String callsign);
	void resetCallsign(UUID playerUUID);
}

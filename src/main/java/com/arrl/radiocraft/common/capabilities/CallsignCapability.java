package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.api.capabilities.ICallsignCapability;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CallsignCapability implements ICallsignCapability {

	private final Map<UUID, String> callsigns = new HashMap<>();

	@Override
	public String getCallsign(UUID playerUUID) {
		return callsigns.get(playerUUID);
	}

	@Override
	public void setCallsign(UUID playerUUID, String callsign) {
		callsigns.put(playerUUID, callsign);
	}

	@Override
	public void resetCallsign(UUID playerUUID) {
		callsigns.remove(playerUUID);
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();

		for(UUID uuid : callsigns.keySet())
			nbt.putUUID(callsigns.get(uuid), uuid); // Saving them backwards as NBT can't use UUID as the key.

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		callsigns.clear();

		for(String callsign : nbt.getAllKeys()) {
			callsigns.put(nbt.getUUID(callsign), callsign);
		}
	}

}

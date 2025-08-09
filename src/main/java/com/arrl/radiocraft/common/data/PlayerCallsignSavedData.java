package com.arrl.radiocraft.common.data;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.PlayerCallsignData;
import com.arrl.radiocraft.api.capabilities.IPlayerCallsignCapability;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class PlayerCallsignSavedData extends SavedData implements IPlayerCallsignCapability {

    private static final String DATA_NAME = Radiocraft.MOD_ID + "_player_callsign_data";

    // Key: Player UUID, Value: PlayerCallsignData (Player UUID, Callsign, License Class)
    private static final HashMap<String, PlayerCallsignData> callsigns = new HashMap<>();

    public static PlayerCallsignSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(PlayerCallsignSavedData::new, PlayerCallsignSavedData::load), DATA_NAME
        );
    }

    private static PlayerCallsignSavedData load(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        CompoundTag callData = nbt.getCompound("player_callsign_data");
        PlayerCallsignSavedData data = new PlayerCallsignSavedData();
        PlayerCallsignSavedData.callsigns.clear();
        for (String uuid: callData.getAllKeys()) {
            String playerUUID = null;
            String callsign = null;
            LicenseClass licenseClass = null;
            try {
                playerUUID = callData.getString(uuid +"_uuid");
                callsign = callData.getString(uuid +"_callsign");
                licenseClass = LicenseClass.valueOf(callData.getString(uuid +"_class"));
                PlayerCallsignSavedData.callsigns.put(uuid, new PlayerCallsignData(playerUUID, callsign, licenseClass));
            } catch (IllegalArgumentException e) {
                Radiocraft.LOGGER.error("Invalid callsign data for {}", playerUUID);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        CompoundTag callData = new CompoundTag();
        for (String uuid: callsigns.keySet()) {
            PlayerCallsignData data = callsigns.get(uuid);
            callData.putString(uuid + "_uuid", data.playerUUID());
            callData.putString(uuid + "_callsign", data.callsign());
            callData.putString(uuid + "_class", data.licenseClass().name());
        }
        nbt.put("player_callsign_data", callData);
        return nbt;
    }

    @Override
    public PlayerCallsignData getCallsignData(UUID playerUUID) {
        return callsigns.get(playerUUID.toString());
    }

    @Override
    public IPlayerCallsignCapability setCallsignData(UUID playerUUID, PlayerCallsignData playerCallsignData) {
        callsigns.put(playerUUID.toString(), playerCallsignData);
        setDirty();
        return this;
    }

    @Override
    public IPlayerCallsignCapability resetCallsign(UUID playerUUID) {
        callsigns.remove(playerUUID.toString());
        setDirty();
        return this;
    }
}

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

import java.util.ArrayList;
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
        Radiocraft.LOGGER.info("Loading player callsign data");
        CompoundTag callData = nbt.getCompound("player_callsign_data");
        PlayerCallsignSavedData data = new PlayerCallsignSavedData();
        PlayerCallsignSavedData.callsigns.clear();

        for (String uuid : callData.getAllKeys()) {
            try {
                CompoundTag playerData = callData.getCompound(uuid);
                String callsign = playerData.getString("callsign");
                String playerName = "";
                if (playerData.contains("name")) {
                    playerName = playerData.getString("name");
                }
                LicenseClass licenseClass = LicenseClass.valueOf(playerData.getString("class"));
                PlayerCallsignSavedData.callsigns.put(uuid, new PlayerCallsignData(uuid, playerName, callsign, licenseClass));
                Radiocraft.LOGGER.info("Loaded callsign data for UUID: {}, Callsign: {}", uuid, callsign);
            } catch (IllegalArgumentException e) {
                Radiocraft.LOGGER.error("Invalid callsign data for UUID: {}", uuid, e);
            }
        }
        Radiocraft.LOGGER.info("Loaded {} player callsign entries", PlayerCallsignSavedData.callsigns.size());
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        Radiocraft.LOGGER.info("Saving player callsign data");
        CompoundTag callData = new CompoundTag();
        for (String uuid: callsigns.keySet()) {
            PlayerCallsignData data = callsigns.get(uuid);
            CompoundTag playerData = new CompoundTag();
            playerData.putString("callsign", data.callsign());
            playerData.putString("class", data.licenseClass().name());
            if (data.playerName() != null && !data.playerName().isEmpty()) {
                playerData.putString("name", data.playerName());
            }
            callData.put(uuid, playerData);
        }
        nbt.put("player_callsign_data", callData);
        return nbt;
    }

    @Override
    public ArrayList<String> getCallsigns() {
        return callsigns.values().stream().map(PlayerCallsignData::callsign).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public PlayerCallsignData getCallsignData(String callsign) {
        return callsigns.values().stream().filter(data -> data.callsign().equals(callsign)).findFirst().orElse(null);
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

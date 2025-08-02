
package com.arrl.radiocraft.common.data;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.BlockEntityCallsignData;
import com.arrl.radiocraft.api.capabilities.IBlockEntityCallsignCapability;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockEntityCallsignSavedData extends SavedData implements IBlockEntityCallsignCapability {

    private static final String DATA_NAME = Radiocraft.MOD_ID + "_block_entity_callsign_data";
    private static final HashMap<GlobalPos, BlockEntityCallsignData> callsigns = new HashMap<>();

    public static BlockEntityCallsignSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(BlockEntityCallsignSavedData::new, BlockEntityCallsignSavedData::load), DATA_NAME
        );
    }

    private static BlockEntityCallsignSavedData load(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        CompoundTag callData = nbt.getCompound("block_entity_callsign_data");
        BlockEntityCallsignSavedData data = new BlockEntityCallsignSavedData();
        BlockEntityCallsignSavedData.callsigns.clear();

        for (String globalPosKey : callData.getAllKeys()) {
            try {
                CompoundTag posData = callData.getCompound(globalPosKey);
                GlobalPos globalPos = GlobalPos.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), posData.get("globalPos")).result().orElse(null);

                if (globalPos != null) {
                    String callsign = posData.getString("callsign");
                    LicenseClass licenseClass = LicenseClass.valueOf(posData.getString("licenseClass"));

                    // Note: Level cannot be reconstructed from NBT, so we pass null
                    BlockEntityCallsignData blockData = new BlockEntityCallsignData(globalPos, callsign, licenseClass);
                    BlockEntityCallsignSavedData.callsigns.put(globalPos, blockData);
                }
            } catch (IllegalArgumentException e) {
                Radiocraft.LOGGER.error("Invalid block entity callsign data for position {}", globalPosKey);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        CompoundTag callData = new CompoundTag();

        int index = 0;
        for (GlobalPos globalPos : callsigns.keySet()) {
            BlockEntityCallsignData data = callsigns.get(globalPos);
            CompoundTag posData = new CompoundTag();

            // Serialize GlobalPos
            GlobalPos.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), globalPos)
                    .result()
                    .ifPresent(tag -> posData.put("globalPos", tag));

            posData.putString("callsign", data.callsign());
            posData.putString("licenseClass", data.licenseClass().name());

            callData.put("entry_" + index, posData);
            index++;
        }

        nbt.put("block_entity_callsign_data", callData);
        return nbt;
    }

    @Override
    public ArrayList<String> getCallsigns() {
        return callsigns.values().stream().map(BlockEntityCallsignData::callsign).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public BlockEntityCallsignData getCallsignData(String callsign) {
        return callsigns.values().stream().filter(data -> data.callsign().equals(callsign)).findFirst().orElse(null);
    }

    @Override
    public BlockEntityCallsignData getCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData) {
        return callsigns.get(pos);
    }

    @Override
    public IBlockEntityCallsignCapability setCallsignData(GlobalPos pos, BlockEntityCallsignData callsignData) {
        callsigns.put(pos, callsignData);
        setDirty();
        return this;
    }

    @Override
    public IBlockEntityCallsignCapability resetCallsign(GlobalPos pos) {
        callsigns.remove(pos);
        setDirty();
        return this;
    }
}
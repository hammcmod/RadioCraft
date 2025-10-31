package com.arrl.radiocraft.common.advancements;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores per-player progression for VHF transmission based advancements.
 */
public class TransmissionProgressSavedData extends SavedData {

    private static final String DATA_NAME = Radiocraft.MOD_ID + "_vhf_transmission_progress";
    private final Map<UUID, PlayerTransmissionProgress> playerData = new HashMap<>();

    public static TransmissionProgressSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(TransmissionProgressSavedData::new, TransmissionProgressSavedData::load), DATA_NAME
        );
    }

    private TransmissionProgressSavedData() {
    }

    private static TransmissionProgressSavedData load(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        TransmissionProgressSavedData data = new TransmissionProgressSavedData();
        CompoundTag playersTag = nbt.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag playerTag = playersTag.getCompound(key);
                PlayerTransmissionProgress progress = PlayerTransmissionProgress.fromTag(playerTag);
                data.playerData.put(uuid, progress);
            } catch (IllegalArgumentException ex) {
                Radiocraft.LOGGER.error("Failed to parse UUID {} in transmission progress data", key, ex);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerTransmissionProgress> entry : playerData.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().toTag());
        }
        nbt.put("players", playersTag);
        return nbt;
    }

    public PlayerTransmissionProgress getOrCreate(UUID uuid) {
        return playerData.computeIfAbsent(uuid, ignored -> {
            setDirty();
            return new PlayerTransmissionProgress();
        });
    }

    public static class PlayerTransmissionProgress {

        private static final String OVERWORLD = "overworld";
        private static final String NETHER = "nether";
        private static final String LEGACY = "legacy";
        private static final String TYPES = "types";

        private final Set<String> overworldBiomes = new HashSet<>();
        private final Set<String> netherBiomes = new HashSet<>();
        private final Set<String> legacyBiomes = new HashSet<>();
        private final Set<String> biomeTypes = new HashSet<>();

        private PlayerTransmissionProgress() {
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put(OVERWORLD, toList(overworldBiomes));
            tag.put(NETHER, toList(netherBiomes));
            tag.put(LEGACY, toList(legacyBiomes));
            tag.put(TYPES, toList(biomeTypes));
            return tag;
        }

        private static PlayerTransmissionProgress fromTag(CompoundTag tag) {
            PlayerTransmissionProgress progress = new PlayerTransmissionProgress();
            progress.overworldBiomes.addAll(fromList(tag.getList(OVERWORLD, Tag.TAG_STRING)));
            progress.netherBiomes.addAll(fromList(tag.getList(NETHER, Tag.TAG_STRING)));
            progress.legacyBiomes.addAll(fromList(tag.getList(LEGACY, Tag.TAG_STRING)));
            progress.biomeTypes.addAll(fromList(tag.getList(TYPES, Tag.TAG_STRING)));
            return progress;
        }

        private static ListTag toList(Set<String> values) {
            ListTag listTag = new ListTag();
            for (String value : values) {
                listTag.add(StringTag.valueOf(value));
            }
            return listTag;
        }

        private static Set<String> fromList(ListTag listTag) {
            Set<String> values = new HashSet<>();
            for (int i = 0; i < listTag.size(); i++) {
                values.add(listTag.getString(i));
            }
            return values;
        }

        public boolean trackOverworld(ResourceLocation biomeId) {
            return overworldBiomes.add(biomeId.toString());
        }

        public boolean trackNether(ResourceLocation biomeId) {
            return netherBiomes.add(biomeId.toString());
        }

        public boolean trackLegacy(ResourceLocation biomeId) {
            return legacyBiomes.add(biomeId.toString());
        }

        public boolean trackBiomeType(String typeKey) {
            return biomeTypes.add(typeKey);
        }

        public int overworldCount() {
            return overworldBiomes.size();
        }

        public int netherCount() {
            return netherBiomes.size();
        }

        public int legacyCount() {
            return legacyBiomes.size();
        }

        public int biomeTypeCount() {
            return biomeTypes.size();
        }
    }
}

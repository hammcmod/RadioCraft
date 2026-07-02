package com.arrl.radiocraft.common.advancements;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central place to update BOTA/SOTA advancements whenever a VHF handheld transmission occurs.
 */
public final class VhfTransmissionTracker {

    private static final int SOTA_MIN_Y = 128;

    private static final ResourceLocation ADV_ROOT = Radiocraft.id("achievements/root");
    private static final ResourceLocation ADV_SOTA = Radiocraft.id("achievements/vhf_sota");
    private static final ResourceLocation ADV_BOTA_OVERWORLD = Radiocraft.id("achievements/bota_all_overworld");
    private static final ResourceLocation ADV_BOTA_NETHER = Radiocraft.id("achievements/bota_all_nether");
    private static final ResourceLocation ADV_BOTA_TYPES = Radiocraft.id("achievements/bota_biome_types");
    private static final ResourceLocation ADV_BOTA_LEGACY = Radiocraft.id("achievements/bota_legacy");
    private static final ResourceLocation ADV_BOTA_ARID = Radiocraft.id("achievements/bota_arid");
    private static final ResourceLocation ADV_BOTA_WETLAND = Radiocraft.id("achievements/bota_wetland");
    private static final ResourceLocation ADV_BOTA_FLATLAND = Radiocraft.id("achievements/bota_flatland");
    private static final ResourceLocation ADV_BOTA_WOODLAND = Radiocraft.id("achievements/bota_woodland");
    private static final ResourceLocation ADV_BOTA_HIGHLAND = Radiocraft.id("achievements/bota_highland");
    private static final ResourceLocation ADV_BOTA_OCEAN = Radiocraft.id("achievements/bota_ocean");

    private static final TagKey<Biome> OVERWORLD_TAG = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_overworld"));
    private static final TagKey<Biome> NETHER_TAG = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_nether"));
    private static final TagKey<Biome> LEGACY_TAG = TagKey.create(Registries.BIOME, Radiocraft.id("on_the_air/legacy_biomes"));

    private static final Map<String, TagKey<Biome>> BIOME_TYPE_TAGS = new HashMap<>();
    private static final Map<String, ResourceLocation> BIOME_TYPE_ADVANCEMENTS = new HashMap<>();

    static {
        registerBiomeType("arid", ADV_BOTA_ARID);
        registerBiomeType("wetland", ADV_BOTA_WETLAND);
        registerBiomeType("flatland", ADV_BOTA_FLATLAND);
        registerBiomeType("woodland", ADV_BOTA_WOODLAND);
        registerBiomeType("highland", ADV_BOTA_HIGHLAND);
        registerBiomeType("ocean", ADV_BOTA_OCEAN);
    }

    private VhfTransmissionTracker() {
    }

    public static void handleTransmission(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Optional<ResourceKey<Biome>> biomeKeyOptional = biomeHolder.unwrapKey();

        if (biomeKeyOptional.isEmpty()) {
            return;
        }

        ResourceKey<Biome> biomeKey = biomeKeyOptional.get();
        TransmissionProgressSavedData data = TransmissionProgressSavedData.get(level);
        TransmissionProgressSavedData.PlayerTransmissionProgress progress = data.getOrCreate(player.getUUID());
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        boolean dirty = false;

        if (Radiocraft.class.getResource("/data/radiocraft/advancements/achievements/root.json") == null) {
            Radiocraft.LOGGER.warn("Advancement resource not found on classpath: data/radiocraft/advancements/achievements/root.json");
        }

        try {
            player.server.getResourceManager().getResourceOrThrow(Radiocraft.id("advancements/achievements/root.json"));
        } catch (Exception ex) {
            Radiocraft.LOGGER.error("Failed to locate advancement resource", ex);
        }
        award(player, ADV_ROOT);

        Radiocraft.LOGGER.debug("VHF handheld transmit: player={}, biome={}, dimension={}, y={} ",
                player.getScoreboardName(), biomeKey.location(), level.dimension().location(), pos.getY());

        if (level.dimension() == Level.OVERWORLD && pos.getY() >= SOTA_MIN_Y) {
            award(player, ADV_SOTA);
        }

        if (biomeHolder.is(OVERWORLD_TAG)) {
            if (progress.trackOverworld(biomeKey.location())) {
                Radiocraft.LOGGER.debug("Overworld biome recorded for {} => {} ({} total)",
                        player.getScoreboardName(), biomeKey.location(), progress.overworldCount());
                dirty = true;
            }
            int required = tagSize(biomeRegistry, OVERWORLD_TAG);
            if (required > 0 && progress.overworldCount() >= required) {
                award(player, ADV_BOTA_OVERWORLD);
            }
        }

        if (biomeHolder.is(NETHER_TAG)) {
            if (progress.trackNether(biomeKey.location())) {
                Radiocraft.LOGGER.debug("Nether biome recorded for {} => {} ({} total)",
                        player.getScoreboardName(), biomeKey.location(), progress.netherCount());
                dirty = true;
            }
            int required = tagSize(biomeRegistry, NETHER_TAG);
            if (required > 0 && progress.netherCount() >= required) {
                award(player, ADV_BOTA_NETHER);
            }
        }

        if (biomeHolder.is(LEGACY_TAG)) {
            if (progress.trackLegacy(biomeKey.location())) {
                Radiocraft.LOGGER.debug("Legacy biome recorded for {} => {} ({} total)",
                        player.getScoreboardName(), biomeKey.location(), progress.legacyCount());
                dirty = true;
            }
            int required = tagSize(biomeRegistry, LEGACY_TAG);
            if (required > 0 && progress.legacyCount() >= required) {
                award(player, ADV_BOTA_LEGACY);
            }
        }

        boolean categoryUnlocked = false;
        for (Map.Entry<String, TagKey<Biome>> entry : BIOME_TYPE_TAGS.entrySet()) {
            String categoryKey = entry.getKey();
            if (biomeHolder.is(entry.getValue()) && progress.trackBiomeType(categoryKey)) {
                dirty = true;
                categoryUnlocked = true;
                Radiocraft.LOGGER.debug("Biome category recorded for {} => {} ({} categories)",
                        player.getScoreboardName(), categoryKey, progress.biomeTypeCount());
                ResourceLocation categoryAdvancement = BIOME_TYPE_ADVANCEMENTS.get(categoryKey);
                if (categoryAdvancement != null) {
                    award(player, categoryAdvancement);
                }
            }
        }
        if (categoryUnlocked && progress.biomeTypeCount() >= BIOME_TYPE_TAGS.size()) {
            award(player, ADV_BOTA_TYPES);
        }

        if (dirty) {
            data.setDirty();
        }
    }

    private static void award(ServerPlayer player, ResourceLocation advancementId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        var advancementManager = server.getAdvancements();
        var advancementHolder = advancementManager.get(advancementId);
        if (advancementHolder == null) {
            Radiocraft.LOGGER.warn("Missing advancement {}", advancementId);
            Radiocraft.LOGGER.debug("Unable to find advancement {} in registered set.", advancementId);
            return;
        }
        AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancementHolder);
        if (advancementProgress.isDone()) {
            return;
        }
        for (String criterion : advancementProgress.getRemainingCriteria()) {
            player.getAdvancements().award(advancementHolder, criterion);
            Radiocraft.LOGGER.debug("Awarded advancement {} criterion {} to {}", advancementId, criterion, player.getScoreboardName());
        }
    }

    private static int tagSize(Registry<Biome> registry, TagKey<Biome> tagKey) {
        return registry.getTag(tagKey)
                .map(holderSet -> (int) holderSet.stream().map(Holder::unwrapKey).flatMap(Optional::stream).count())
                .orElse(0);
    }

    private static void registerBiomeType(String key, ResourceLocation advancementId) {
        TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, Radiocraft.id("on_the_air/" + key));
        BIOME_TYPE_TAGS.put(key, tagKey);
        BIOME_TYPE_ADVANCEMENTS.put(key, advancementId);
    }
}

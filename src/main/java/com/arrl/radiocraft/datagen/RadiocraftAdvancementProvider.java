package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Emits RadioCraft advancement definitions so they appear inside the generated mod datapack.
 */
public class RadiocraftAdvancementProvider implements DataProvider {

    private final PackOutput output;

    public RadiocraftAdvancementProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");

        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>();

        definitions.put(Radiocraft.id("achievements/root"), createAdvancement(
                null,
                "advancement.radiocraft.root.title",
                "advancement.radiocraft.root.description",
                "goal",
                false,
                false,
                false,
                ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                "bootstrap"));

        definitions.put(Radiocraft.id("achievements/vhf_sota"), createChild(
                Radiocraft.id("achievements/root"),
                "advancement.radiocraft.vhf_sota.title",
                "advancement.radiocraft.vhf_sota.description",
                "goal"));

        definitions.put(Radiocraft.id("achievements/bota_arid"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_arid.title",
                "advancement.radiocraft.bota_arid.description",
                "task"));
        definitions.put(Radiocraft.id("achievements/bota_wetland"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_wetland.title",
                "advancement.radiocraft.bota_wetland.description",
                "task"));
        definitions.put(Radiocraft.id("achievements/bota_flatland"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_flatland.title",
                "advancement.radiocraft.bota_flatland.description",
                "task"));
        definitions.put(Radiocraft.id("achievements/bota_woodland"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_woodland.title",
                "advancement.radiocraft.bota_woodland.description",
                "task"));
        definitions.put(Radiocraft.id("achievements/bota_highland"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_highland.title",
                "advancement.radiocraft.bota_highland.description",
                "task"));
        definitions.put(Radiocraft.id("achievements/bota_ocean"), createChild(
                Radiocraft.id("achievements/vhf_sota"),
                "advancement.radiocraft.bota_ocean.title",
                "advancement.radiocraft.bota_ocean.description",
                "task"));

        definitions.put(Radiocraft.id("achievements/bota_biome_types"), createChild(
                Radiocraft.id("achievements/bota_ocean"),
                "advancement.radiocraft.bota_biome_types.title",
                "advancement.radiocraft.bota_biome_types.description",
                "goal"));

        definitions.put(Radiocraft.id("achievements/bota_all_overworld"), createChild(
                Radiocraft.id("achievements/bota_biome_types"),
                "advancement.radiocraft.bota_all_overworld.title",
                "advancement.radiocraft.bota_all_overworld.description",
                "challenge"));

        definitions.put(Radiocraft.id("achievements/bota_legacy"), createChild(
                Radiocraft.id("achievements/bota_all_overworld"),
                "advancement.radiocraft.bota_legacy.title",
                "advancement.radiocraft.bota_legacy.description",
                "challenge"));

        definitions.put(Radiocraft.id("achievements/bota_all_nether"), createChild(
                Radiocraft.id("achievements/root"),
                "advancement.radiocraft.bota_all_nether.title",
                "advancement.radiocraft.bota_all_nether.description",
                "goal"));

        return CompletableFuture.allOf(definitions.entrySet().stream()
                .map(entry -> DataProvider.saveStable(cachedOutput, entry.getValue(), pathProvider.json(entry.getKey())))
                .toArray(CompletableFuture[]::new));
    }

    private JsonObject createChild(ResourceLocation parent, String titleKey, String descriptionKey, String frame) {
        return createAdvancement(parent, titleKey, descriptionKey, frame, true, true, false, null, "progress");
    }

    private JsonObject createAdvancement(ResourceLocation parent,
                                         String titleKey,
                                         String descriptionKey,
                                         String frame,
                                         boolean toast,
                                         boolean announce,
                                         boolean hidden,
                                         @Nullable ResourceLocation background,
                                         String criterionName) {
        JsonObject advancement = new JsonObject();
        if (parent != null) {
            advancement.addProperty("parent", parent.toString());
        }

        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();
        icon.addProperty("item", Radiocraft.id("vhf_handheld").toString());
        display.add("icon", icon);

        display.add("title", translate(titleKey));
        display.add("description", translate(descriptionKey));
        display.addProperty("frame", frame);
        display.addProperty("show_toast", toast);
        display.addProperty("announce_to_chat", announce);
        display.addProperty("hidden", hidden);
        if (background != null) {
            display.addProperty("background", background.toString());
        }
        advancement.add("display", display);

        JsonObject criteria = new JsonObject();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");
        criteria.add(criterionName, trigger);
        advancement.add("criteria", criteria);

        return advancement;
    }

    private JsonObject translate(String key) {
        JsonObject json = new JsonObject();
        json.addProperty("translate", key);
        return json;
    }

    @Override
    public String getName() {
        return "Radiocraft Advancements";
    }
}

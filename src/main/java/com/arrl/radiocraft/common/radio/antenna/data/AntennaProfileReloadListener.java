package com.arrl.radiocraft.common.radio.antenna.data;

import com.arrl.radiocraft.Radiocraft;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads antenna S-parameter profiles (converted from NanoVNA .s1p measurements) from data packs.
 */
public class AntennaProfileReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private final Map<ResourceLocation, S1pSmith> profiles = new ConcurrentHashMap<>();

    public AntennaProfileReloadListener() {
        super(GSON, "antenna_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiles.clear();
        jsonMap.forEach((id, element) -> {
            try {
                JsonObject json = element.getAsJsonObject();
                double z0 = json.has("z0_ohms") ? json.get("z0_ohms").getAsDouble() : 50.0D;

                JsonArray pointsArray = json.getAsJsonArray("points");
                if (pointsArray == null) {
                    throw new JsonParseException("Missing 'points' array");
                }

                List<S1pSmith.DataPoint> points = new ArrayList<>(pointsArray.size());
                for (JsonElement entry : pointsArray) {
                    parsePoint(id, entry, points);
                }

                S1pSmith smith = S1pSmith.from(points, z0);
                profiles.put(id, smith);
            } catch (IllegalArgumentException | JsonParseException ex) {
                Radiocraft.LOGGER.error("Failed to parse antenna profile {}: {}", id, ex.getMessage());
            }
        });

        Radiocraft.LOGGER.info("Loaded {} antenna profile(s)", profiles.size());
    }

    private static void parsePoint(ResourceLocation id, JsonElement element, List<S1pSmith.DataPoint> out) {
        if (element.isJsonArray()) {
            JsonArray triple = element.getAsJsonArray();
            if (triple.size() < 3) {
                throw new JsonParseException("Point entry must contain [freq, real, imag]");
            }
            double freq = triple.get(0).getAsDouble();
            double real = triple.get(1).getAsDouble();
            double imag = triple.get(2).getAsDouble();
            out.add(new S1pSmith.DataPoint(freq, new S1pSmith.Complex(real, imag)));
            return;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("freq_hz") || !obj.has("real") || !obj.has("imag")) {
                throw new JsonParseException("Object point entries require freq_hz, real, and imag fields");
            }
            double freq = obj.get("freq_hz").getAsDouble();
            double real = obj.get("real").getAsDouble();
            double imag = obj.get("imag").getAsDouble();
            out.add(new S1pSmith.DataPoint(freq, new S1pSmith.Complex(real, imag)));
            return;
        }

        throw new JsonParseException("Unsupported point format in " + id);
    }

    public @Nullable S1pSmith getProfile(ResourceLocation id) {
        return profiles.get(id);
    }

    public Set<ResourceLocation> getAvailableProfiles() {
        return Collections.unmodifiableSet(profiles.keySet());
    }
}

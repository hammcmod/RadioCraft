package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.antlr.v4.runtime.misc.Pair;

import java.util.HashMap;
import java.util.Map;

public class SolarEventReloadListener extends SimpleJsonResourceReloadListener {

	private static final Gson GSON = new Gson();
	private final Map<ResourceLocation, Pair<SolarEvent, Integer>> events = new HashMap<>();
	private int totalWeight = 0; // Cache total weight for optimisation

	public SolarEventReloadListener(String directory) {
		super(GSON, directory);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
		events.clear();
		jsonMap.forEach((resourceLocation, jsonElement) -> {
			try {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				float noise = jsonObject.get("noise").getAsFloat();
				int minDuration = jsonObject.get("minDuration").getAsInt();
				int maxDuration = jsonObject.get("maxDuration").getAsInt();

				int weight = jsonObject.get("weight").getAsInt();

				events.put(resourceLocation, new Pair<>(new SolarEvent(noise, minDuration, maxDuration), weight));
				totalWeight += weight;

			} catch (IllegalArgumentException | JsonParseException jsonparseexception) {
				Radiocraft.LOGGER.error("Parsing error loading solar event {}: {}", resourceLocation, jsonparseexception.getMessage());
			}
		});
	}

	public ResourceLocation getKey(SolarEvent event) {
		for(ResourceLocation key : events.keySet()) {
			if(events.get(key).a == event)
				return key;
		}
		return null;
	}

	public SolarEvent getValue(ResourceLocation key) {
		return events.get(key).a;
	}

	public SolarEvent getWeightedRandom() {
		int r = (int)(Math.random() * totalWeight);
		for(Pair<SolarEvent, Integer> entry : events.values()) {
			r -= entry.b;
			if(r <= 0)
				return entry.a;
		}
		Radiocraft.LOGGER.error("No solar events found, using default");
		return new SolarEvent(0.3F, 1, 1);
	}

}
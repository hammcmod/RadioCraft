package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BandReloadListener extends SimpleJsonResourceReloadListener {

	private static final Gson GSON = new Gson();
	private final Map<Integer, Band> bands = Collections.synchronizedMap(new HashMap<>());

	public BandReloadListener(String directory) {
		super(GSON, directory);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		synchronized (bands) {
			bands.clear();
			jsonMap.forEach((resourceLocation, jsonElement) -> {
				try {
					JsonObject jsonObject = jsonElement.getAsJsonObject();
					int wavelength = jsonObject.get("wavelength").getAsInt();
					int losRange = jsonObject.get("los").getAsInt();
					int minSkipDay = jsonObject.get("minSkipDay").getAsInt();
					int maxSkipDay = jsonObject.get("maxSkipDay").getAsInt();
					int minSkipNight = jsonObject.get("minSkipNight").getAsInt();
					int maxSkipNight = jsonObject.get("maxSkipNight").getAsInt();
					int minFrequency = jsonObject.get("minFrequency").getAsInt();
					int maxFrequency = jsonObject.get("maxFrequency").getAsInt();

					bands.put(wavelength, new Band(wavelength, losRange, minSkipDay, maxSkipDay, minSkipNight, maxSkipNight, minFrequency, maxFrequency));

				} catch (IllegalArgumentException | JsonParseException jsonparseexception) {
					Radiocraft.LOGGER.error("Parsing error radio band {}: {}", resourceLocation, jsonparseexception.getMessage());
				}
			});
		}
	}

	public Band getValue(int wavelength) {
		return bands.get(wavelength);
	}

}
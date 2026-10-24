package com.arrl.radiocraft.client.render;

import com.arrl.radiocraft.Radiocraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where each radio's screen is, read from its block model: the element the maintainer names {@code DISPLAY} (in
 * Blockbench), on the model as drawn facing north. Premium and Cheap radios share their plain radio's model shape, so
 * they look up the plain one. Read on first use and again after every resource reload, so a re-export lines up.
 */
public final class RadioDisplays {

	/**
	 * A radio's screen in model pixels (0 to 16): its left and right as seen from the front ({@code x1} west, {@code x2}
	 * east, and the front faces north, so east is on the viewer's left), bottom and top, and the depth of its front
	 * face. {@code rise} lifts the whole radio when it hangs from a ceiling, as the blockstate's ceiling model does.
	 */
	public record Display(float x1, float x2, float y1, float y2, float front, float rise) {}

	private static final Map<String, Optional<Display>> CACHE = new ConcurrentHashMap<>();

	private RadioDisplays() {}

	/** Forgets what was read, after a resource reload. */
	public static void clear() {
		CACHE.clear();
	}

	/** The screen of the radio block {@code blockPath} ("hf_radio_20m_pure"), or null if its model has none. */
	@Nullable
	public static Display get(String blockPath) {
		String model = blockPath.replaceAll("_(pure|poor)$", "");
		return CACHE.computeIfAbsent(model, RadioDisplays::read).orElse(null);
	}

	private static Optional<Display> read(String model) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Radiocraft.id("models/block/" + model + ".json"));
		if(resource.isEmpty())
			return Optional.empty();
		try(Reader reader = resource.get().openAsReader()) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if(!json.has("elements"))
				return Optional.empty();
			float top = 0.0F;
			JsonObject display = null;
			for(JsonElement element : json.getAsJsonArray("elements")) {
				JsonObject object = element.getAsJsonObject();
				top = Math.max(top, object.getAsJsonArray("to").get(1).getAsFloat());
				if(object.has("name") && object.get("name").getAsString().equalsIgnoreCase("DISPLAY"))
					display = object;
			}
			if(display == null)
				return Optional.empty();
			var from = display.getAsJsonArray("from");
			var to = display.getAsJsonArray("to");
			float rise = 16.0F - Math.min(16.0F, top); // The ceiling model is raised so its top meets the block above.
			return Optional.of(new Display(from.get(0).getAsFloat(), to.get(0).getAsFloat(), from.get(1).getAsFloat(), to.get(1).getAsFloat(),
					Math.min(from.get(2).getAsFloat(), to.get(2).getAsFloat()), rise));
		}
		catch(Exception e) {
			Radiocraft.LOGGER.warn("Cannot read the display of radio model {}: {}", model, e.toString());
			return Optional.empty();
		}
	}
}

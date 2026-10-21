package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Access to the {@link IAntennaType} registry
 */
public class AntennaTypes {

    /**
     * Constructor for the {@link AntennaTypes} class.
     */
    public AntennaTypes() {}

	private static final Map<ResourceLocation, IAntennaType<?>> REGISTRY = new LinkedHashMap<>(); // Matched in registration order.
	private static final List<IAntennaType<?>> FALLBACKS = new ArrayList<>();

	/**
	 * Register an {@link IAntennaType}.
     * @param <C> The type of {@link IAntennaType} being registered.
	 * @param type The {@link IAntennaType} to be registered
	 * @return The {@link IAntennaType} after registration.
	 */
	public static <C extends IAntennaType<?>> C registerType(C type) {
		ResourceLocation id = type.getId();
		if(REGISTRY.containsKey(id))
            Radiocraft.LOGGER.warn("Attempted to register a duplicate Antenna Type of ID {}", id.toString());
		else
			REGISTRY.put(id, type);

		return type;
	}

	/**
	 * Register an {@link IAntennaType} that is only tried when no ordinary type matches: a catch-all for builds that are
	 * not any particular antenna.
	 * @param <C> The type of {@link IAntennaType} being registered.
	 * @param type The {@link IAntennaType} to be registered
	 * @return The {@link IAntennaType} after registration.
	 */
	public static <C extends IAntennaType<?>> C registerFallbackType(C type) {
		registerType(type);
		FALLBACKS.add(type);
		return type;
	}

	/**
	 * Grab an instance of {@link IAntennaType} by it's ID.
	 * @param id The {@link ResourceLocation} representing the desired {@link IAntennaType}, it should match the location
	 *           returned by {@link IAntennaType#getId()}
	 * @return The {@link IAntennaType} registered under id, otherwise null if none were found.
	 */
	public static IAntennaType<?> getType(ResourceLocation id) {
		return REGISTRY.get(id);
	}

	/** A readable name for a type, from its id: "radiocraft:inverted_v" becomes "Inverted V". */
	public static String displayName(IAntennaType<?> type) {
		StringBuilder name = new StringBuilder();
		for(String word : type.getId().getPath().split("_")) {
			if(word.isEmpty())
				continue;
			if(!name.isEmpty())
				name.append(' ');
			name.append(word.length() <= 2 ? word.toUpperCase(Locale.ROOT) : Character.toUpperCase(word.charAt(0)) + word.substring(1));
		}
		return name.toString();
	}

	/**
	 * Attempt to check for a valid {@link StaticAntenna} at the provided location.
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} the antenna is located at.
	 * @return An {@link StaticAntenna} of the first matching type, otherwise null if there was no valid antenna.
	 */
	public static StaticAntenna<?> match(Level level, BlockPos pos) {
		for(IAntennaType<?> type : REGISTRY.values()) {
			if(FALLBACKS.contains(type))
				continue;
			StaticAntenna<?> antenna = type.match(level, pos);
			if(antenna != null)
				return antenna;
		}
		for(IAntennaType<?> type : FALLBACKS) {
			StaticAntenna<?> antenna = type.match(level, pos);
			if(antenna != null)
				return antenna;
		}
		return null;
	}

}

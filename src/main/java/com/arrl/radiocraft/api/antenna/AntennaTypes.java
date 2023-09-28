package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Access to the {@link IAntennaType} registry
 */
public class AntennaTypes {

	private static final Map<ResourceLocation, IAntennaType<?>> REGISTRY = new HashMap<>();

	/**
	 * Register an {@link IAntennaType}.
	 * @param type The {@link IAntennaType} to be registered
	 * @return The {@link IAntennaType} after registration.
	 */
	public static <C extends IAntennaType<?>> C registerType(C type) {
		ResourceLocation id = type.getId();
		if(REGISTRY.containsKey(id))
			Radiocraft.LOGGER.warn("Attempted to register a duplicate Antenna Type of ID " + id.toString());
		else
			REGISTRY.put(id, type);

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

	/**
	 * Attempt to check for a valid {@link BEAntenna} at the provided location.
	 * @param level The {@link Level} to check.
	 * @param pos The {@link BlockPos} the antenna is located at.
	 * @return An {@link BEAntenna} of the first matching type, otherwise null if there was no valid antenna.
	 */
	public static BEAntenna<?> match(Level level, BlockPos pos) {
		for(IAntennaType<?> type : REGISTRY.values()) {
			BEAntenna<?> antenna = type.match(level, pos);
			if(antenna != null)
				return antenna;
		}
		return null;
	}

}

package com.arrl.radiocraft.compat.create;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Create support, safe to call whether or not Create is installed: with Create, and without Create Crafts &amp;
 * Additions (which has its own), RadioCraft adds an alternator that turns rotation into Forge Energy for batteries and
 * the power network. Nothing here touches Create's classes; {@link CreateAlternator} does, and is only loaded when
 * this says so.
 */
public final class CreateCompat {

	public static final String CREATE = "create";
	public static final String CRAFTS_AND_ADDITIONS = "createaddition";

	private CreateCompat() {
	}

	/** True if RadioCraft's alternator exists in this game: Create is installed and Crafts &amp; Additions is not. */
	public static boolean isActive() {
		return ModList.get().isLoaded(CREATE) && !ModList.get().isLoaded(CRAFTS_AND_ADDITIONS);
	}

	/** Registers the alternator on {@code modBus}, if it should exist. Call once, from the mod's constructor. */
	public static void register(IEventBus modBus) {
		if(isActive())
			CreateAlternator.register(modBus);
	}
}

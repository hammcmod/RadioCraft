package com.arrl.radiocraft.compat.anotherweathermod;

import com.arrl.radiocraft.api.weather.WeatherSources;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Another Weather Mod support, safe to call whether or not it is installed: with it, WX reports and WEFAX charts show
 * its localized storms, and its storm winds can blow antennas down ({@link AwmWindDamage}). Nothing here touches its
 * classes; those two do, and are only loaded when the mod is.
 */
public final class AwmCompat {

	public static final String MOD_ID = "anotherweathermod";

	private AwmCompat() {
	}

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MOD_ID);
	}

	/** Registers its weather source. Call once, at common setup. */
	public static void register() {
		if(isLoaded()) {
			WeatherSources.register(new AwmWeatherSource());
			NeoForge.EVENT_BUS.addListener(AwmWindDamage::onWindDamage);
		}
	}
}

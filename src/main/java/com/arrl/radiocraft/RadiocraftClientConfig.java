package com.arrl.radiocraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings for each player's own game ({@code radiocraft-client.toml}); a server does not see or enforce them. */
public class RadiocraftClientConfig {

	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Boolean> SHOW_RADIO_SCREENS;

	static {
		SHOW_RADIO_SCREENS = BUILDER.comment(" Show the frequency, mode and S-meter on the screens of radios that are on, within a few blocks")
				.define("show_radio_screens", true);
		SPEC = BUILDER.build();
	}
}

package com.arrl.radiocraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class RadiocraftServerConfig {

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ConfigValue<Integer> FREQUENCY_STEP;
	public static final ConfigValue<Integer> ANTENNA_UPDATE_DELAY;

	static {
		BUILDER.push("Antenna Options ( * = Restart game to take effect)");
		ANTENNA_UPDATE_DELAY = BUILDER.comment("Delay in seconds of an antenna to recalculate it's state after being changed #default 5").define("antenna_update_delay", 5);
		BUILDER.pop();

		BUILDER.push("Band Options ( * = Restart game to take effect)");
		FREQUENCY_STEP = BUILDER.comment("The minimum step size for a frequency, in kHz (0.001MHz) #default 1").define("frequency_step", 1);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

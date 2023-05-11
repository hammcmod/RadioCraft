package com.arrl.radiocraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class RadiocraftConfig {

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ConfigValue<Integer> LARGE_BATTERY_CAPACITY;
	public static final ConfigValue<Integer> LARGE_BATTERY_OUTPUT;
	public static final ConfigValue<Integer> CHARGE_CONTROLLER_TICK;
	public static final ConfigValue<Integer> SOLAR_PANEL_MAX_OUTPUT;
	public static final ConfigValue<Double> SOLAR_PANEL_RAIN_MULTIPLIER;

	public static final ConfigValue<Integer> HF_RADIO_10M_RECEIVE_TICK;
	public static final ConfigValue<Integer> HF_RADIO_10M_TRANSMIT_TICK;

	public static final ConfigValue<Integer> ANTENNA_UPDATE_DELAY;
	public static final ConfigValue<Integer> ANTENNA_MAX_RADIUS;

	static {
		BUILDER.push("Power Options ( * = Restart game to take effect)");
		LARGE_BATTERY_CAPACITY = BUILDER.comment("*Energy capacity of large batteries. #default 1500000 integer").define("large_battery_capacity", 1500000);
		LARGE_BATTERY_OUTPUT = BUILDER.comment("*Max output per tick for a large battery. #default 1250").define("large_battery_output", 1250);
		CHARGE_CONTROLLER_TICK = BUILDER.comment("*Amount of power a charge controller can process each tick. #default 625 integer").define("charge_controller_tick", 625);
		SOLAR_PANEL_MAX_OUTPUT = BUILDER.comment("*Maximum output of a solar power per tick. #default 125").define("solar_panel_max_output integer", 125);
		SOLAR_PANEL_RAIN_MULTIPLIER = BUILDER.comment("*Multiplier for solar panel output while raining #default 0.5").define("solar_panel_rain_multiplier double", 0.5D);
		BUILDER.pop();

		BUILDER.push("Radio Options ( * = Restart game to take effect)");
		HF_RADIO_10M_RECEIVE_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while receiving) #default 125").define("hf_radio_10m_receive", 125);
		HF_RADIO_10M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while transmitting) #default 375").define("hf_radio_10m_transmit", 375);
		BUILDER.pop();

		BUILDER.push("Antenna Options ( * = Restart game to take effect)");
		ANTENNA_UPDATE_DELAY = BUILDER.comment("Delay in seconds of an antenna to recalculate it's state after being changed #default 10").define("antenna_update_delay", 10);
		ANTENNA_MAX_RADIUS = BUILDER.comment("Maximum radius in blocks for an antenna #default 20").define("antenna_max_radius", 20);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

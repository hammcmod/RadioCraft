package com.arrl.radiocraft;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {

	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Integer> LARGE_BATTERY_CAPACITY;
	public static final ModConfigSpec.ConfigValue<Integer> LARGE_BATTERY_OUTPUT;
	public static final ModConfigSpec.ConfigValue<Integer> SMALL_BATTERY_CAPACITY;
	public static final ModConfigSpec.ConfigValue<Integer> CHARGE_CONTROLLER_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> CHARGE_CONTROLLER_BATTERY_CHARGE;
	public static final ModConfigSpec.ConfigValue<Integer> SOLAR_PANEL_MAX_OUTPUT;
	public static final ModConfigSpec.ConfigValue<Double> SOLAR_PANEL_RAIN_MULTIPLIER;

	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_10M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_10M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_20M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_20M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_40M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_40M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_80M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_80M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_ALL_BAND_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RADIO_ALL_BAND_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> HF_RECEIVER_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_BASE_STATION_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_BASE_STATION_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_RECEIVER_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_REPEATER_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_REPEATER_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_20M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_20M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_40M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_40M_TRANSMIT_TICK;




	static {
		BUILDER.push("Power Options ( * = Restart game to take effect)");
		BUILDER.comment(" Power is Forge Energy (FE), so other mods' generators and cables can charge a large battery. Balanced",
				" (2026-10-23) so one small Create water wheel through a Create: Crafts & Additions alternator geared to 4 RPM",
				" (about 5 FE/t) just keeps up with a VHF base station transmitting 80% of the time (1 FE/t listening, 6 transmitting).");
		LARGE_BATTERY_CAPACITY = BUILDER.comment("*Energy capacity of large batteries. #default 120000 integer").define("large_battery_capacity", 120000);
		LARGE_BATTERY_OUTPUT = BUILDER.comment("*Max output per tick for a large battery. #default 40").define("large_battery_output", 40);
		SMALL_BATTERY_CAPACITY = BUILDER.comment("*Energy capacity of small batteries; a newly crafted one starts 50-70% charged. #default 30000 integer").define("small_battery_capacity", 30000);
		CHARGE_CONTROLLER_TICK = BUILDER.comment("*Amount of power a charge controller can process each tick. #default 20 integer").define("charge_controller_tick", 20);
		CHARGE_CONTROLLER_BATTERY_CHARGE = BUILDER.comment("*Amount of power a charge controller can transfer to a small battery each tick. #default 4 integer").define("charge_controller_battery_charge", 4);
		SOLAR_PANEL_MAX_OUTPUT = BUILDER.comment("*Maximum output of a solar power per tick. #default 4").define("solar_panel_max_output integer", 4);
		SOLAR_PANEL_RAIN_MULTIPLIER = BUILDER.comment("*Multiplier for solar panel output while raining #default 0.5").define("solar_panel_rain_multiplier double", 0.5D);
		BUILDER.pop();

		BUILDER.push("Radio Options ( * = Restart game to take effect)");
		HF_RADIO_10M_RECEIVE_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while receiving) #default 4").define("hf_radio_10m_receive", 4);
		HF_RADIO_10M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while transmitting) #default 12").define("hf_radio_10m_transmit", 12);
		HF_RADIO_20M_RECEIVE_TICK = BUILDER.comment("*HF Radio (20m) power consumption per tick (while receiving) #default 4").define("hf_radio_20m_receive", 4);
		HF_RADIO_20M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (20m) power consumption per tick (while transmitting) #default 12").define("hf_radio_20m_transmit", 12);
		HF_RADIO_40M_RECEIVE_TICK = BUILDER.comment("*HF Radio (40m) power consumption per tick (while receiving) #default 4").define("hf_radio_40m_receive", 4);
		HF_RADIO_40M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (40m) power consumption per tick (while transmitting) #default 12").define("hf_radio_40m_transmit", 12);
		HF_RADIO_80M_RECEIVE_TICK = BUILDER.comment("*HF Radio (80m) power consumption per tick (while receiving) #default 4").define("hf_radio_80m_receive", 4);
		HF_RADIO_80M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (80m) power consumption per tick (while transmitting) #default 12").define("hf_radio_80m_transmit", 12);
		HF_RADIO_ALL_BAND_RECEIVE_TICK = BUILDER.comment("*HF Radio (all band) power consumption per tick (while receiving) #default 4").define("hf_radio_all_band_receive", 4);
		HF_RADIO_ALL_BAND_TRANSMIT_TICK = BUILDER.comment("*HF Radio (all band) power consumption per tick (while transmitting) #default 12").define("hf_radio_all_band_transmit", 12);
		HF_RECEIVER_TICK = BUILDER.comment("*HF Receiver power consumption per tick (while receiving) #default 4").define("hf_receiver", 4);
		QRP_RADIO_20M_RECEIVE_TICK = BUILDER.comment("*QRP Radio (20m) power consumption per tick (while receiving) #default 2").define("qrp_radio_20m_receive", 2);
		QRP_RADIO_20M_TRANSMIT_TICK = BUILDER.comment("*QRP Radio (20m) power consumption per tick (while transmitting) #default 6").define("qrp_radio_20m_transmit", 6);
		QRP_RADIO_40M_RECEIVE_TICK = BUILDER.comment("*QRP Radio (40m) power consumption per tick (while receiving) #default 2").define("qrp_radio_40m_receive", 2);
		QRP_RADIO_40M_TRANSMIT_TICK = BUILDER.comment("*QRP Radio (40m) power consumption per tick (while transmitting) #default 6").define("qrp_radio_40m_transmit", 6);
		VHF_BASE_STATION_RECEIVE_TICK = BUILDER.comment("*VHF Base Station power consumption per tick (while receiving) #default 1").define("vhf_base_station_receive", 1);
		VHF_BASE_STATION_TRANSMIT_TICK = BUILDER.comment("*VHF Base Station power consumption per tick (while transmitting) #default 6").define("vhf_base_station_transmit", 6);
		VHF_RECEIVER_TICK = BUILDER.comment("*VHF Receiver power consumption per tick (while receiving) #default 1").define("vhf_receiver", 1);
		VHF_REPEATER_RECEIVE_TICK = BUILDER.comment("*VHF Repeater power consumption per tick (while listening) #default 2").define("vhf_repeater_receive", 2);
		VHF_REPEATER_TRANSMIT_TICK = BUILDER.comment("*VHF Repeater power consumption per tick (while transmitting) #default 8").define("vhf_repeater_transmit", 8);


		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

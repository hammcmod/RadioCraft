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

    public static final ModConfigSpec.ConfigValue<Integer> PTT_HELD_RELEASE_TIME_MS;
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
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_20M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_20M_TRANSMIT_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_40M_RECEIVE_TICK;
	public static final ModConfigSpec.ConfigValue<Integer> QRP_RADIO_40M_TRANSMIT_TICK;

	static {
		BUILDER.push("Power Options ( * = Restart game to take effect)");
		LARGE_BATTERY_CAPACITY = BUILDER.comment("*Energy capacity of large batteries. #default 1500000 integer").define("large_battery_capacity", 1500000);
		LARGE_BATTERY_OUTPUT = BUILDER.comment("*Max output per tick for a large battery. #default 1250").define("large_battery_output", 1250);
		SMALL_BATTERY_CAPACITY = BUILDER.comment("*Energy capacity of small batteries. #default 125000 integer").define("small_battery_capacity", 125000);
		CHARGE_CONTROLLER_TICK = BUILDER.comment("*Amount of power a charge controller can process each tick. #default 625 integer").define("charge_controller_tick", 625);
		CHARGE_CONTROLLER_BATTERY_CHARGE = BUILDER.comment("*Amount of power a charge controller can transfer to a small battery each tick. #default 10 integer").define("charge_controller_battery_charge", 10);
		SOLAR_PANEL_MAX_OUTPUT = BUILDER.comment("*Maximum output of a solar power per tick. #default 125").define("solar_panel_max_output integer", 125);
		SOLAR_PANEL_RAIN_MULTIPLIER = BUILDER.comment("*Multiplier for solar panel output while raining #default 0.5").define("solar_panel_rain_multiplier double", 0.5D);
		BUILDER.pop();

		BUILDER.push("Radio Options ( * = Restart game to take effect)");
        /*
          PTT_HELD_RELEASE_TIME_MS: Time to continue transmitting after last PTT held event received from use event
          You might find if your client has fairly low FPS that you will want to set this value higher than 200ms. 200ms should handle as low as 5 FPS. Maximum value is 1000ms or 1 FPS.
          Setting this value below 50ms will have little to no effect as the voice packets are buffered at 20ms of samples and the game tick is 50ms per.
         */
        PTT_HELD_RELEASE_TIME_MS = BUILDER.comment("*Time to continue transmitting after last PTT held event #default 1000").defineInRange("ptt_held_release_time_ms", 200, 50, 1000);
		HF_RADIO_10M_RECEIVE_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while receiving) #default 125").define("hf_radio_10m_receive", 125);
		HF_RADIO_10M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (10m) power consumption per tick (while transmitting) #default 375").define("hf_radio_10m_transmit", 375);
		HF_RADIO_20M_RECEIVE_TICK = BUILDER.comment("*HF Radio (20m) power consumption per tick (while receiving) #default 125").define("hf_radio_20m_receive", 125);
		HF_RADIO_20M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (20m) power consumption per tick (while transmitting) #default 375").define("hf_radio_20m_transmit", 375);
		HF_RADIO_40M_RECEIVE_TICK = BUILDER.comment("*HF Radio (40m) power consumption per tick (while receiving) #default 125").define("hf_radio_40m_receive", 125);
		HF_RADIO_40M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (40m) power consumption per tick (while transmitting) #default 375").define("hf_radio_40m_transmit", 375);
		HF_RADIO_80M_RECEIVE_TICK = BUILDER.comment("*HF Radio (80m) power consumption per tick (while receiving) #default 125").define("hf_radio_80m_receive", 125);
		HF_RADIO_80M_TRANSMIT_TICK = BUILDER.comment("*HF Radio (80m) power consumption per tick (while transmitting) #default 375").define("hf_radio_80m_transmit", 375);
		HF_RADIO_ALL_BAND_RECEIVE_TICK = BUILDER.comment("*HF Radio (all band) power consumption per tick (while receiving) default #125").define("hf_radio_all_band_receive", 125);
		HF_RADIO_ALL_BAND_TRANSMIT_TICK = BUILDER.comment("*HF Radio (all band) power consumption per tick (while transmitting) #default 375").define("hf_radio_all_band_transmit", 375);
		HF_RECEIVER_TICK = BUILDER.comment("*HF Receiver power consumption per tick (while receiving) #default 125").define("hf_receiver", 125);
		QRP_RADIO_20M_RECEIVE_TICK = BUILDER.comment("*QRP Radio (20m) power consumption per tick (while receiving) #default 63").define("qrp_radio_20m_receive", 63);
		QRP_RADIO_20M_TRANSMIT_TICK = BUILDER.comment("*QRP Radio (20m) power consumption per tick (while transmitting) #default 188").define("qrp_radio_20m_transmit", 188);
		QRP_RADIO_40M_RECEIVE_TICK = BUILDER.comment("*QRP Radio (40m) power consumption per tick (while receiving) #default 63").define("qrp_radio_40m_receive", 63);
		QRP_RADIO_40M_TRANSMIT_TICK = BUILDER.comment("*QRP Radio (40m) power consumption per tick (while transmitting) #default 188").define("qrp_radio_40m_transmit", 188);
		VHF_BASE_STATION_RECEIVE_TICK = BUILDER.comment("*VHF Base Station power consumption per tick (while receiving) #default 63").define("vhf_base_station_receive", 63);
		VHF_BASE_STATION_TRANSMIT_TICK = BUILDER.comment("*VHF Base Station power consumption per tick (while transmitting) #default 188").define("vhf_base_station_transmit", 188);
		VHF_RECEIVER_TICK = BUILDER.comment("*VHF Receiver power consumption per tick (while receiving) #default 125").define("vhf_receiver", 125);


		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

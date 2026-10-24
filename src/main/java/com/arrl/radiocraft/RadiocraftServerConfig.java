package com.arrl.radiocraft;

import com.arrl.radiocraft.common.radio.Band;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadiocraftServerConfig {

	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Integer> HF_FREQUENCY_STEP;
	public static final ModConfigSpec.ConfigValue<Integer> VHF_FREQUENCY_STEP;
	public static final ModConfigSpec.ConfigValue<Integer> ANTENNA_UPDATE_DELAY;

	public static final ModConfigSpec.ConfigValue<Double> HANDHELD_MAX_GAIN;
	public static final ModConfigSpec.ConfigValue<Double> HANDHELD_MAX_MIC_GAIN;

    public static final ModConfigSpec.ConfigValue<Boolean> CALLSIGN_PERMISSIONS_ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> CALLSIGN_SELF_ASSIGN;
	public static final ModConfigSpec.EnumValue<CwKeying> CW_KEYING;

	/** How CW is sent from a radio's screen (proposal). Both have the keyboard key ({@code key.radiocraft.cw_key}). */
	public enum CwKeying { TRUE_MORSE, CHAT_MORSE }
	public static final ModConfigSpec.ConfigValue<String> CALLSIGN_DIGITS;
	public static final ModConfigSpec.ConfigValue<Integer> CALLSIGN_ZONE_RADIUS;
	public static final ModConfigSpec.EnumValue<com.arrl.radiocraft.common.radio.Callsigns.RepeaterMode> REPEATER_CALLSIGN_MODE;

	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_STRENGTH;
	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_ONLY_BAD_SWR;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_BAD_SWR;
	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_TRANSMIT_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_TRANSMIT_CHANCE;

	public static final ModConfigSpec.ConfigValue<Boolean> LIGHTNING_STATIC_ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> SOLAR_EVENTS_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> LIGHTNING_STATIC_STRENGTH;
	public static final ModConfigSpec.ConfigValue<Integer> LIGHTNING_STATIC_RANGE;

	public static final ModConfigSpec.ConfigValue<Boolean> ANTENNA_TUNER_ENABLED;

	public static final ModConfigSpec.ConfigValue<Boolean> RADIO_POWER_REQUIRED;
	public static final ModConfigSpec.ConfigValue<Double> BARE_CONNECTOR_RANGE;

	public static final ModConfigSpec.ConfigValue<Double> DUMMY_LOAD_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> DUMMY_LOAD_STRENGTH;

	public static final ModConfigSpec.ConfigValue<Double> TNC_MIN_SNR;
	public static final ModConfigSpec.ConfigValue<Double> TNC_MIN_SIGNAL;
	public static final ModConfigSpec.ConfigValue<Integer> TNC_DIGIPEAT_DELAY_TICKS;
	public static final ModConfigSpec.ConfigValue<Integer> TNC_DIGIPEAT_MAX_HOPS;
	public static final ModConfigSpec.ConfigValue<Integer> TNC_BEACON_INTERVAL_SECONDS;
	public static final ModConfigSpec.ConfigValue<Integer> APRS_CONTACT_LAMP_MINUTES;
	public static final ModConfigSpec.ConfigValue<Boolean> APRS_H19_STAINED_GLASS;
	public static final ModConfigSpec.ConfigValue<Double> ANTENNA_TUNER_MAX_SWR;
	public static final ModConfigSpec.ConfigValue<Integer> AUTO_TUNER_POWER_USE;
	public static final ModConfigSpec.ConfigValue<Double> RADIO_BLOW_SWR;
	public static final ModConfigSpec.ConfigValue<Double> RADIO_BLOW_CHANCE;
	public static final ModConfigSpec.ConfigValue<Double> RADIO_EXPLODE_CHANCE;
	public static final ModConfigSpec.ConfigValue<Double> ANTENNA_TUNER_LOSS;

	public static final ModConfigSpec.ConfigValue<Boolean> VOICE_PROCESSING_ENABLED;
	public static final ModConfigSpec.ConfigValue<Integer> VOICE_BAND_LOW;
	public static final ModConfigSpec.ConfigValue<Integer> VOICE_BAND_HIGH;
	public static final ModConfigSpec.ConfigValue<Double> VOICE_DRIVE;
	public static final ModConfigSpec.ConfigValue<Double> VHF_NOISE_FLOOR;
	public static final ModConfigSpec.ConfigValue<Double> SKIP_FADING_DEPTH;

	public static final ModConfigSpec.ConfigValue<Boolean> TELETYPE_NEEDS_RADIO;
	public static final ModConfigSpec.ConfigValue<Integer> AURORA_LATITUDE;
	public static final ModConfigSpec.ConfigValue<Boolean> AURORA_AT_ANY_Z;
	public static final ModConfigSpec.ConfigValue<Integer> AURORA_VHF_MIN_RANGE;
	public static final ModConfigSpec.ConfigValue<Integer> AURORA_VHF_MAX_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> AURORA_VHF_STRENGTH;
	public static final ModConfigSpec.ConfigValue<Boolean> SPORADIC_E_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> SPORADIC_E_CHANCE_PER_DAY;
	public static final ModConfigSpec.ConfigValue<Integer> SPORADIC_E_MIN_RANGE;
	public static final ModConfigSpec.ConfigValue<Integer> SPORADIC_E_MAX_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> SPORADIC_E_STRENGTH;
	public static final ModConfigSpec.ConfigValue<Double> AURORA_STATIC;

	public static final ModConfigSpec.ConfigValue<Double> TOWER_FOOTHOLD_BREAK_CHANCE;
	public static final ModConfigSpec.ConfigValue<Integer> DESK_MIC_TIMEOUT_SECONDS;
	public static final ModConfigSpec.ConfigValue<Boolean> WIRE_HAZARDS_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> WIRE_FAULT_DRAIN;
	public static final ModConfigSpec.ConfigValue<Integer> WIRE_SHORT_BURN_SECONDS;
	public static final ModConfigSpec.ConfigValue<Double> WIRE_WATER_DAMAGE;
	public static final ModConfigSpec.ConfigValue<Integer> WIRE_WATER_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> WIRE_WATER_BATTERY_IGNITE_CHANCE;
	public static final ModConfigSpec.ConfigValue<Double> WIRE_ZAP_DAMAGE;
	public static final ModConfigSpec.ConfigValue<Integer> WIRE_ZAP_MULTIPLIER;
	public static final ModConfigSpec.ConfigValue<Boolean> BATTERY_FIRES_ENABLED;
	public static final ModConfigSpec.ConfigValue<Integer> BATTERY_BURN_SECONDS;
	public static final ModConfigSpec.ConfigValue<Integer> BATTERY_EXPLODE_GROUP;
	public static final ModConfigSpec.ConfigValue<Double> BATTERY_EXPLOSION_POWER_PER_BATTERY;
	public static final ModConfigSpec.ConfigValue<Double> BATTERY_EXPLOSION_MAX_POWER;
	public static final ModConfigSpec.ConfigValue<Integer> BATTERY_LIGHTNING_WIRE_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> ANTENNA_LIGHTNING_CHANCE;
	public static final ModConfigSpec.ConfigValue<Integer> ANTENNA_LIGHTNING_RANGE;
	public static final ModConfigSpec.ConfigValue<Boolean> LIGHTNING_ANTENNA_DAMAGE;
	public static final ModConfigSpec.ConfigValue<Double> LIGHTNING_COAX_BURN_CHANCE;
	public static final ModConfigSpec.ConfigValue<Double> LIGHTNING_BATTERY_IGNITE_CHANCE;
	public static final ModConfigSpec.ConfigValue<Boolean> GUIDE_ON_FIRST_JOIN;

	public static final ModConfigSpec.ConfigValue<Boolean> TERRAIN_ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> TERRAIN_LINE_OF_SIGHT;
	public static final ModConfigSpec.ConfigValue<Boolean> TERRAIN_SKY_VIEW;
	public static final ModConfigSpec.ConfigValue<Integer> TERRAIN_ATLAS_MAX_CHUNKS;
	public static final ModConfigSpec.ConfigValue<Integer> TERRAIN_REFRESH_CHUNKS_PER_TICK;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_RECALCULATE_SECONDS;
	public static final ModConfigSpec.ConfigValue<Integer> TERRAIN_CLIFF_RISE;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_CLIFF_LOSS_DB;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_CLIFF_MAX_LOSS_DB;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_TAKEOFF_10M;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_TAKEOFF_20M;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_TAKEOFF_40M;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_TAKEOFF_80M;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_SKY_FADE;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_INDOOR_VHF_LOSS_DB;
	public static final ModConfigSpec.ConfigValue<Double> TERRAIN_INDOOR_HF_LOSS_DB;

	public static final ModConfigSpec.ConfigValue<Double> CRYSTAL_SAND_CHANCE;
	public static final ModConfigSpec.ConfigValue<Integer> CRYSTAL_POOR_WEIGHT;
	public static final ModConfigSpec.ConfigValue<Integer> CRYSTAL_GOOD_WEIGHT;
	public static final ModConfigSpec.ConfigValue<Integer> CRYSTAL_PURE_WEIGHT;
	public static final ModConfigSpec.ConfigValue<Double> CRYSTAL_PURE_NOISE_FACTOR;
	public static final ModConfigSpec.ConfigValue<Double> CRYSTAL_POOR_EXTRA_NOISE;
	public static final ModConfigSpec.ConfigValue<Double> CRYSTAL_POOR_DRIFT_HZ;

	public record BandConfig(String name, ModConfigSpec.ConfigValue<Integer> losRange, ModConfigSpec.ConfigValue<Integer> minSkipDay, ModConfigSpec.ConfigValue<Integer> maxSkipDay, ModConfigSpec.ConfigValue<Integer> minSkipNight, ModConfigSpec.ConfigValue<Integer> maxSkipNight, ModConfigSpec.ConfigValue<Float> minFrequency, ModConfigSpec.ConfigValue<Float> maxFrequency) {
		public Band getBand() {
			return new Band(name, losRange.get(), minSkipDay.get(), maxSkipDay.get(), minSkipNight.get(), maxSkipNight.get(), minFrequency.get(), maxFrequency.get());
		}
	}

	public static final Map<String, BandConfig> BAND_CONFIGS = new ConcurrentHashMap<>();

	static {
		BUILDER.push("Antenna Options ( * = Restart game to take effect)");
		ANTENNA_UPDATE_DELAY = BUILDER.comment(" Delay in seconds of an antenna to recalculate it's state after being changed #default 5").define("antenna_update_delay", 5);
		BUILDER.pop();

		BUILDER.push("Band Options ( * = Restart game to take effect)");
		HF_FREQUENCY_STEP = BUILDER.comment(" The minimum step size for a HF frequency, in Hz #default 1000").define("hf_frequency_step", 1_000);
		VHF_FREQUENCY_STEP = BUILDER.comment(" The channel step of VHF radios (handhelds, base stations, receivers), in Hz #default 20000").define("vhf_frequency_step", 20_000);
		BUILDER.pop();

		BUILDER.comment(" While name and frequency are to measures of the same principle, when we refer to a band like say 10m, we mean a range of actual frequencies that aren't exactly 10m in name");
		BUILDER.comment(" Band names like 10m are just for human convenience (so you don't have to say radio capable of 144MHz to 146MHz");

		BAND_CONFIGS.clear();
		for(Band band : Band.getDefaults()) {
			BUILDER.push(band.name().equals("2m") ? band.name() + " band settings, also known as VHF" : band.name() + " band settings");
			BAND_CONFIGS.put(band.name(), new BandConfig(
					band.name(),
					BUILDER.comment(" Line of sight range for this band").define(band.name() + "m_los", band.losRange()),
					BUILDER.comment(" minimum range for the skip effect during the day").define(band.name() + "m_minSkipDay", band.minSkipDay()),
					BUILDER.comment(" maximum range for the skip effect during the day").define(band.name() + "m_maxSkipDay", band.maxSkipDay()),
					BUILDER.comment(" minimum range for the skip effect during the night").define(band.name() + "m_minSkipNight", band.minSkipNight()),
					BUILDER.comment(" maximum range for the skip effect during the night").define(band.name() + "m_maxSkipNight", band.maxSkipNight()),
					BUILDER.comment(" The minimum frequency for this band").define(band.name() + "m_minFrequency", band.minFrequency()),
					BUILDER.comment(" The maximum frequency for this band").define(band.name() + "m_maxFrequency", band.maxFrequency())
			));
			BUILDER.pop();
		}

		BUILDER.push("VHF Handheld Options");
		HANDHELD_MAX_GAIN = BUILDER.comment(" Maximum output gain").defineInRange("handheld_max_gain", 5.0, 1.0, 25.0);
		HANDHELD_MAX_MIC_GAIN = BUILDER.comment(" Maximum microphone gain").defineInRange("handheld_max_mic_gain", 5.0, 1.0, 25.0);
		BUILDER.pop();

        BUILDER.push("Permissions");
        CALLSIGN_PERMISSIONS_ENABLED = BUILDER.comment(" Require op for callsign configuration").define("callsign_permissions_enabled", true);
        BUILDER.pop();

		BUILDER.push("CW");
		CW_KEYING = BUILDER.comment(" TRUE_MORSE: CW is keyed by hand only, with the screen's key button or the CW key (Left Alt by default).",
				" CHAT_MORSE: as well, a text box under the radio sends typed text as Morse, in time").defineEnum("cw_keying", CwKeying.CHAT_MORSE);
		BUILDER.pop();

		BUILDER.push("Callsigns");
		CALLSIGN_SELF_ASSIGN = BUILDER.comment(" Players may choose their own callsign (asked the first time they use a radio, or /callsign claim). False:",
				" only operators hand them out").define("callsign_self_assign", true);
		CALLSIGN_DIGITS = BUILDER.comment(" Digits players may use in a callsign they choose, e.g. \"0123456789\" or \"3\"").define("callsign_digits", "0123456789");
		CALLSIGN_ZONE_RADIUS = BUILDER.comment(" Repeater callsigns (X#XX) carry a digit for how far from spawn they stand: 0 near spawn up to 9 at this many",
				" blocks (or the world border, if nearer)").defineInRange("callsign_zone_radius", 10000, 10, 30000000);
		REPEATER_CALLSIGN_MODE = BUILDER.comment(" Repeater callsigns: EITHER (made up in the X#XX form, or the owner's own callsign), AUTO (X#XX only) or OWN",
				" (the owner's callsign only). New repeaters get an X#XX one, or with OWN their placer's").defineEnum("repeater_callsign_mode", com.arrl.radiocraft.common.radio.Callsigns.RepeaterMode.EITHER);
		BUILDER.pop();

		BUILDER.push("Redstone Interference");
		BUILDER.comment(" Redstone components switching near a station's feedline or antenna add noise to that radio's static.",
				" A single switch is a small pop; a redstone clock is a steady buzz. Long HF bands hear the most, 20m half, 10m and VHF a tenth.");
		REDSTONE_INTERFERENCE_ENABLED = BUILDER.comment(" Set to false to turn redstone interference off entirely").define("redstone_interference_enabled", true);
		REDSTONE_INTERFERENCE_STRENGTH = BUILDER.comment(" Multiplier on how much noise redstone adds (1.0 = default, 0.5 = half, 2.0 = double)").defineInRange("redstone_interference_strength", 1.0, 0.0, 10.0);
		REDSTONE_INTERFERENCE_ONLY_BAD_SWR = BUILDER.comment(" If true, only stations whose antenna SWR is at or above redstone_interference_bad_swr pick up redstone noise").define("redstone_interference_only_bad_swr", false);
		REDSTONE_INTERFERENCE_BAD_SWR = BUILDER.comment(" SWR from which an antenna counts as badly matched, for the option above and for the transmit side").defineInRange("redstone_interference_bad_swr", 2.0, 1.0, 100.0);
		BUILDER.comment(" Transmit side: RF from a transmitting station induces a redstone level (4, more with a bad SWR) in dust near the antenna",
				" (2 blocks for QRP rigs, 6 for full power; with a bad SWR also 10 blocks of the radio and 5 of the coax) and can flip",
				" levers, buttons and torches there. Which blocks are hit is fixed per station until the antenna or SWR changes.");
		REDSTONE_INTERFERENCE_TRANSMIT_ENABLED = BUILDER.comment(" Set to false to stop transmitters affecting redstone").define("redstone_interference_transmit_enabled", true);
		REDSTONE_INTERFERENCE_TRANSMIT_CHANCE = BUILDER.comment(" Fraction of the redstone components in reach that a station affects").defineInRange("redstone_interference_transmit_chance", 0.35, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Lightning Static");
		BUILDER.comment(" A lightning strike puts a crash of static on every powered radio in the dimension, loudest close by and fading",
				" with distance. Long HF bands hear the most, 20m half, 10m and VHF a tenth, like redstone noise.");
		LIGHTNING_STATIC_ENABLED = BUILDER.comment(" Set to false to turn lightning crashes off").define("lightning_static_enabled", true);
		SOLAR_EVENTS_ENABLED = BUILDER.comment(" Solar weather and geomagnetic storms. Set to false to keep every dimension on average conditions (no blackouts,",
				" storms or auroras from new events)").define("solar_events_enabled", true);
		LIGHTNING_STATIC_STRENGTH = BUILDER.comment(" How much static a strike right next to the radio adds, on top of the solar noise floor (the total is capped at 1, so above 1 nearby strikes saturate and distant ones are still heard)").defineInRange("lightning_static_strength", 1.2, 0.0, 4.0);
		LIGHTNING_STATIC_RANGE = BUILDER.comment(" Distance in blocks at which a strike is no longer heard").defineInRange("lightning_static_range", 512, 16, 8192);
		BUILDER.pop();

		BUILDER.push("Power");
		BUILDER.comment(" Block radios draw power from the wire network they are on (batteries, charge controllers): the per-radio",
				" receive and transmit draw per tick is in the common config. A radio that cannot get its draw browns out:",
				" it stays silent and its panel cycles on / PWR / dark until the power comes back.");
		RADIO_POWER_REQUIRED = BUILDER.comment(" Set to false to let block radios run without any power (the pre-2026-10-21 behaviour)").define("radios_need_power", true);
		BUILDER.pop();

		BUILDER.push("No Antenna");
		BARE_CONNECTOR_RANGE = BUILDER.comment(" A radio with nothing on its coax still sends and hears a little through the bare connector: the",
				" distance in blocks at which that fades to nothing, both ways").defineInRange("bare_connector_range", 8.0D, 1.0D, 64.0D);
		BUILDER.pop();

		BUILDER.push("Dummy Load");
		DUMMY_LOAD_RANGE = BUILDER.comment(" A dummy load is a perfect match on every band but radiates almost nothing: the distance in blocks",
				" at which its signal fades to nothing, both ways. No skip, no solar weather").defineInRange("dummy_load_range", 10.0D, 1.0D, 64.0D);
		DUMMY_LOAD_STRENGTH = BUILDER.comment(" Strength a dummy load sends at right next to it, 0 to 1, falling to nothing at its range")
				.defineInRange("dummy_load_strength", 0.6D, 0.0D, 1.0D);
		BUILDER.pop();

		BUILDER.push("Digital Interface (TNC)");
		TNC_MIN_SNR = BUILDER.comment(" A packet decodes when its signal is at least this many times the receiver's noise floor; weaker ones are lost whole")
				.defineInRange("tnc_min_snr", 1.0D, 0.0D, 10.0D);
		TNC_MIN_SIGNAL = BUILDER.comment(" Weakest signal a packet decodes at even with no noise at all, 0 to 1").defineInRange("tnc_min_signal", 0.02D, 0.0D, 1.0D);
		TNC_DIGIPEAT_DELAY_TICKS = BUILDER.comment(" Ticks a digipeating radio interface waits before repeating a packet it heard")
				.defineInRange("tnc_digipeat_delay_ticks", 20, 1, 200);
		TNC_DIGIPEAT_MAX_HOPS = BUILDER.comment(" Digipeaters a packet may go through; one that has been through this many is not repeated again")
				.defineInRange("tnc_digipeat_max_hops", 8, 0, 16);
		TNC_BEACON_INTERVAL_SECONDS = BUILDER.comment(" Seconds between automatic beacons from an APRS terminal with beaconing enabled")
				.defineInRange("tnc_beacon_interval_seconds", 600, 30, 86400);
		APRS_CONTACT_LAMP_MINUTES = BUILDER.comment(" Minutes an APRS terminal's CONTACT light stays on after a new station is heard, unless someone opens it first; 0 keeps it on until opened")
				.defineInRange("aprs_contact_lamp_minutes", 10, 0, 1440);
		APRS_H19_STAINED_GLASS = BUILDER.comment(" Allow stained glass for the H19 APRS terminal's screen in its recipe, as well as tinted glass")
				.define("allow_stained_glass_for_h19_aprs", false);
		BUILDER.pop();

		BUILDER.push("Antenna Tuner");
		BUILDER.comment(" An antenna tuner anywhere on the coax between a radio and its antenna matches an antenna that is off resonance.",
				" The manual tuner's three dials each have a setting that matches the antenna at the radio's frequency; the",
				" automatic tuner finds it by itself on power. Matched, the radio sees SWR 1 and most of the power a mismatch would",
				" reflect reaches the antenna instead. The further off resonance the antenna is, the more is still lost in the tuner.");
		ANTENNA_TUNER_ENABLED = BUILDER.comment(" Set to false to make tuners decorative again").define("antenna_tuner_enabled", true);
		ANTENNA_TUNER_MAX_SWR = BUILDER.comment(" Highest antenna SWR a tuner can bring to 1:1 (a dipole 12 blocks off in total is SWR 7). Past it a manual",
				" tuner can still take this much minus 1 off (SWR 9 tunes down to 3), and the automatic tuner gives up (TUNE FAIL)").defineInRange("antenna_tuner_max_swr", 7.0, 1.0, 100.0);
		ANTENNA_TUNER_LOSS = BUILDER.comment(" Share of the mismatch loss a tuner cannot recover: 0 = perfect tuner, 1 = tuner only quiets the radio").defineInRange("antenna_tuner_loss", 0.25, 0.0, 1.0);
		AUTO_TUNER_POWER_USE = BUILDER.comment(" Power the automatic tuner draws per tick; without it, it is just a piece of coax (radios_need_power = false runs it free)")
				.defineInRange("auto_tuner_power_use", 4, 0, 1000);
		BUILDER.pop();

		BUILDER.push("Blown Radios");
		BUILDER.comment(" Transmitting into a badly matched antenna (the SWR the radio sees, after any tuner) can burn out the radio's",
				" transmitter. A blown radio still receives, but keying it only puts loud static out of its own speaker. Broken, it",
				" gives back the parts it was crafted from, except its crystal. The repeater is protected and never blows.");
		RADIO_BLOW_SWR = BUILDER.comment(" Lowest SWR that can blow a radio").defineInRange("radio_blow_swr", 3.0, 1.0, 100.0);
		RADIO_BLOW_CHANCE = BUILDER.comment(" Chance per second of transmitting into SWR 10 (more than one antenna on the coax); worse matches between",
				" radio_blow_swr and 10 follow the reflected power (about 0.4 of this at SWR 3). 0 = radios never blow").defineInRange("radio_blow_chance", 0.05, 0.0, 1.0);
		RADIO_EXPLODE_CHANCE = BUILDER.comment(" Chance that a radio with a poor crystal goes up like TNT when it blows, instead of just burning out. 0 = never")
				.defineInRange("radio_explode_chance", 0.1, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Receiver Audio");
		BUILDER.comment(" How received voice is shaped before it plays from the radio, and how it sits against the static. The",
				" receiver's AGC balances signal against noise: a strong signal ducks the static, a weak one stays under it.");
		VOICE_PROCESSING_ENABLED = BUILDER.comment(" Set to false to play received voice clean (only its level follows the signal)").define("voice_processing_enabled", true);
		VOICE_BAND_LOW = BUILDER.comment(" Lowest voice frequency the radio passes, in Hz").defineInRange("voice_band_low", 300, 20, 4000);
		VOICE_BAND_HIGH = BUILDER.comment(" Highest voice frequency the radio passes, in Hz").defineInRange("voice_band_high", 2700, 500, 20000);
		VOICE_DRIVE = BUILDER.comment(" Speaker overdrive on voice peaks: 1 is barely there, 3 is crunchy").defineInRange("voice_drive", 1.6, 0.1, 10.0);
		VHF_NOISE_FLOOR = BUILDER.comment(" Receiver noise on VHF (FM), which has no solar noise: how far a weak VHF signal is pushed down").defineInRange("vhf_noise_floor", 0.15, 0.0, 1.0);
		SKIP_FADING_DEPTH = BUILDER.comment(" How deep skip signals fade in and out (QSB): 0 steady, 1 fades to nothing").defineInRange("skip_fading_depth", 0.5, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Solar Weather Station");
		TELETYPE_NEEDS_RADIO = BUILDER.comment(" The station only reports (and drives a comparator) with a radio of any kind next to it that has a real",
				" antenna on its coax; a dummy load does not count").define("solar_weather_station_needs_radio", true);
		BUILDER.pop();

		BUILDER.push("Aurora");
		BUILDER.comment(" An aurora can follow a solar blackout (chance and timing are in the solar event data). It only reaches",
				" stations far from the equator (Z = 0): there VHF gains a long skip off the aurora, and every HF band drops one",
				" condition step (good to fair, ...) and picks up extra static.");
		AURORA_LATITUDE = BUILDER.comment(" Distance from Z = 0, north or south, from which a station is under the aurora").defineInRange("aurora_latitude", 10000, 0, 30000000);
		AURORA_AT_ANY_Z = BUILDER.comment(" Put every station under an aurora when one comes, whatever its Z, instead of only past aurora_latitude")
				.define("allow_aurora_at_any_z_coordinate", false);
		AURORA_VHF_MIN_RANGE = BUILDER.comment(" Shortest VHF path the aurora carries, in blocks").defineInRange("aurora_vhf_min_range", 500, 0, 1000000);
		AURORA_VHF_MAX_RANGE = BUILDER.comment(" Longest VHF path the aurora carries, in blocks").defineInRange("aurora_vhf_max_range", 4000, 1, 1000000);
		AURORA_VHF_STRENGTH = BUILDER.comment(" Strength (0 to 1) of a VHF signal carried by the aurora").defineInRange("aurora_vhf_strength", 0.8, 0.0, 1.0);
		SPORADIC_E_ENABLED = BUILDER.comment(" Sporadic E: in good or great solar conditions VHF now and then opens for long-range contacts, for a",
				" quarter of a day to a day, everywhere in the dimension").define("sporadic_e_enabled", true);
		SPORADIC_E_CHANCE_PER_DAY = BUILDER.comment(" Chance per in-game day of good or great conditions that sporadic E opens").defineInRange("sporadic_e_chance_per_day", 0.05, 0.0, 1.0);
		SPORADIC_E_MIN_RANGE = BUILDER.comment(" Shortest VHF path sporadic E carries, in blocks").defineInRange("sporadic_e_min_range", 600, 0, 1000000);
		SPORADIC_E_MAX_RANGE = BUILDER.comment(" Longest VHF path sporadic E carries, in blocks").defineInRange("sporadic_e_max_range", 3000, 1, 1000000);
		SPORADIC_E_STRENGTH = BUILDER.comment(" Strength (0 to 1) of a VHF signal carried by sporadic E").defineInRange("sporadic_e_strength", 0.6, 0.0, 1.0);
		AURORA_STATIC = BUILDER.comment(" Extra static on HF radios under the aurora, on top of the solar noise floor").defineInRange("aurora_static", 0.25, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Desk Microphone");
		DESK_MIC_TIMEOUT_SECONDS = BUILDER.comment(" Seconds a desk microphone keeps its radio keyed (held or locked) before it lets go by itself. 0 = never")
				.defineInRange("desk_mic_timeout_seconds", 30, 0, 3600);
		BUILDER.pop();

		BUILDER.push("Electrical Hazards");
		WIRE_HAZARDS_ENABLED = BUILDER.comment(" Bare power wire with power behind it: a wire joined back to itself shorts (smokes, catches fire, burns",
				" through), plain wire touching water electrocutes what is in the water, and an open end of plain wire in the rain",
				" sparks and zaps whatever touches it. Waterproof wire does not get wet or spark, but can short").define("wire_hazards_enabled", true);
		WIRE_FAULT_DRAIN = BUILDER.comment(" Share of the stored energy behind a short or a wet wire drained every second").defineInRange("wire_fault_drain", 0.25, 0.0, 1.0);
		WIRE_SHORT_BURN_SECONDS = BUILDER.comment(" Seconds a powered short burns before the wire burns through").defineInRange("wire_short_burn_seconds", 4, 1, 600);
		WIRE_WATER_DAMAGE = BUILDER.comment(" Damage per second to anything in the water near a wet wire (6 = 3 hearts)").defineInRange("wire_water_damage", 6.0, 0.0, 100.0);
		WIRE_WATER_RANGE = BUILDER.comment(" Blocks from a wet wire that the water is electrified").defineInRange("wire_water_range", 6, 0, 32);
		WIRE_WATER_BATTERY_IGNITE_CHANCE = BUILDER.comment(" Chance per second that a wet wire sets the batteries behind it alight (see Battery Fires)")
				.defineInRange("wire_water_battery_ignite_chance", 0.01, 0.0, 1.0);
		WIRE_ZAP_DAMAGE = BUILDER.comment(" Damage a sparking wire end does to whatever touches it (4 = 2 hearts)").defineInRange("wire_zap_damage", 4.0, 0.0, 100.0);
		WIRE_ZAP_MULTIPLIER = BUILDER.comment(" A sparking end draws as much as a transmitting 20m HF radio; a zap draws this many times that for a second")
				.defineInRange("wire_zap_multiplier", 6, 1, 100);
		BUILDER.pop();

		BUILDER.push("Battery Fires");
		BATTERY_FIRES_ENABLED = BUILDER.comment(" Large batteries catch fire from fire or lava beside them, a flint and steel, or lightning on power wire",
				" running to them, and burn until consumed: water only holds the flames down. Groups explode").define("battery_fires_enabled", true);
		BATTERY_BURN_SECONDS = BUILDER.comment(" Seconds a burning battery lasts before it is consumed").defineInRange("battery_burn_seconds", 45, 1, 3600);
		BATTERY_EXPLODE_GROUP = BUILDER.comment(" A battery consumed in a group of at least this many touching batteries explodes").defineInRange("battery_explode_group", 4, 1, 64);
		BATTERY_EXPLOSION_POWER_PER_BATTERY = BUILDER.comment(" Explosion power per battery in the group (TNT is 4)").defineInRange("battery_explosion_power_per_battery", 1.0, 0.0, 10.0);
		BATTERY_EXPLOSION_MAX_POWER = BUILDER.comment(" Largest battery explosion").defineInRange("battery_explosion_max_power", 8.0, 0.0, 20.0);
		BATTERY_LIGHTNING_WIRE_RANGE = BUILDER.comment(" Lightning within this many blocks of power wire sets the batteries on that wire alight. 0 = never")
				.defineInRange("battery_lightning_wire_range", 3, 0, 16);
		BUILDER.pop();

		BUILDER.push("Antenna Lightning");
		ANTENNA_LIGHTNING_CHANCE = BUILDER.comment(" Antennas draw lightning like a weak lightning rod: the chance a strike no lightning rod claimed lands on the",
				" highest antenna nearby instead. 0 turns it off").defineInRange("antenna_lightning_chance", 0.25, 0.0, 1.0);
		ANTENNA_LIGHTNING_RANGE = BUILDER.comment(" Blocks an antenna draws lightning from (a lightning rod: 128)").defineInRange("antenna_lightning_range", 32, 0, 128);
		LIGHTNING_ANTENNA_DAMAGE = BUILDER.comment(" A strike on an antenna surges down the coax: it blows every radio on the run and burns out coax").define("lightning_antenna_damage", true);
		LIGHTNING_COAX_BURN_CHANCE = BUILDER.comment(" Chance each coax block on a struck antenna's run burns out (the one at the antenna always does)")
				.defineInRange("lightning_coax_burn_chance", 0.3, 0.0, 1.0);
		LIGHTNING_BATTERY_IGNITE_CHANCE = BUILDER.comment(" Chance a strike on an antenna also sets the batteries its radios run on alight")
				.defineInRange("lightning_battery_ignite_chance", 0.1, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Towers");
		TOWER_FOOTHOLD_BREAK_CHANCE = BUILDER.comment(" Chance an old tower foothold (found in structures) gives way each time a player climbs onto it or steps on it.",
				" Crafted footholds never break. 0.0005 = 0.05%").defineInRange("tower_foothold_break_chance", 0.0005, 0.0, 1.0);
		BUILDER.pop();

		BUILDER.push("Guide Book");
		GUIDE_ON_FIRST_JOIN = BUILDER.comment(" Give each player the RadioCraft guide book the first time they join (needs Patchouli). /radiocraft guide",
				" (or /radio guide) gives another any time").define("guide_on_first_join", true);
		BUILDER.pop();

		BUILDER.push("Terrain");
		BUILDER.comment(" Hills, cliffs and valleys shape propagation. Line of sight: ground poking into the straight line between two",
				" antennas weakens the signal by knife-edge diffraction, which hurts VHF most and 80m least; HF also loses some at each",
				" cliff on the way. Skip: a station sees the sky over its horizon, and a band whose signal leaves low (10m, 20m) is cut",
				" by hills and valley walls in that direction, while 80m goes up nearly straight. A handheld under a roof loses some too.",
				" Terrain comes from a coarse map of every chunk ever loaded, saved with the world; terrain nobody has loaded counts as clear.");
		TERRAIN_ENABLED = BUILDER.comment(" Master switch. False turns all of it off for low-performance servers: no terrain map is kept and nothing is",
				" computed, exactly as before terrain existed").define("terrain_enabled", true);
		TERRAIN_LINE_OF_SIGHT = BUILDER.comment(" Terrain blocks line of sight (needs terrain_enabled)").define("terrain_line_of_sight", true);
		TERRAIN_SKY_VIEW = BUILDER.comment(" A station's horizon cuts skip at low angles (needs terrain_enabled)").define("terrain_sky_view", true);
		TERRAIN_ATLAS_MAX_CHUNKS = BUILDER.comment(" Most chunks the terrain map remembers per dimension, about 190 bytes each (100000 is about 19 MB). Past it the",
				" oldest are forgotten and count as clear until they load again").defineInRange("terrain_atlas_max_chunks", 100_000, 1_000, 10_000_000);
		TERRAIN_REFRESH_CHUNKS_PER_TICK = BUILDER.comment(" Loaded chunks re-read per tick, so building and digging shows up while they stay loaded").defineInRange("terrain_refresh_chunks_per_tick", 8, 0, 1000);
		TERRAIN_RECALCULATE_SECONDS = BUILDER.comment(" How long a path's terrain loss and a station's horizon are kept before they are worked out again. Moving",
				" stations are looked up by 4 block cell, so walking still updates them. 0 works it out for every voice packet")
				.defineInRange("terrain_recalculate_seconds", 2.0, 0.0, 600.0);
		TERRAIN_CLIFF_RISE = BUILDER.comment(" Height change over 4 blocks along a path that counts as a cliff for HF ground wave").defineInRange("terrain_cliff_rise", 8, 1, 512);
		TERRAIN_CLIFF_LOSS_DB = BUILDER.comment(" Loss in dB per cliff on 10m; longer bands lose less (80m about a third)").defineInRange("terrain_cliff_loss_db", 6.0, 0.0, 100.0);
		TERRAIN_CLIFF_MAX_LOSS_DB = BUILDER.comment(" Most the cliffs on one path can cost together, in dB").defineInRange("terrain_cliff_max_loss_db", 18.0, 0.0, 100.0);
		TERRAIN_TAKEOFF_10M = BUILDER.comment(" Angle in degrees above the horizon a 10m skip signal leaves at").defineInRange("terrain_takeoff_10m", 5.0, 0.0, 90.0);
		TERRAIN_TAKEOFF_20M = BUILDER.comment(" Angle in degrees above the horizon a 20m skip signal leaves at").defineInRange("terrain_takeoff_20m", 10.0, 0.0, 90.0);
		TERRAIN_TAKEOFF_40M = BUILDER.comment(" Angle in degrees above the horizon a 40m skip signal leaves at").defineInRange("terrain_takeoff_40m", 30.0, 0.0, 90.0);
		TERRAIN_TAKEOFF_80M = BUILDER.comment(" Angle in degrees above the horizon an 80m skip signal leaves at (nearly straight up: NVIS)").defineInRange("terrain_takeoff_80m", 60.0, 0.0, 90.0);
		TERRAIN_SKY_FADE = BUILDER.comment(" Degrees the horizon must rise above the takeoff angle to cut skip completely").defineInRange("terrain_sky_fade", 10.0, 0.1, 90.0);
		TERRAIN_INDOOR_VHF_LOSS_DB = BUILDER.comment(" Loss in dB for a handheld with no sky above it (a roof, a cave), on VHF").defineInRange("terrain_indoor_vhf_loss_db", 10.0, 0.0, 100.0);
		TERRAIN_INDOOR_HF_LOSS_DB = BUILDER.comment(" The same on HF line of sight; skip loses twice this").defineInRange("terrain_indoor_hf_loss_db", 3.0, 0.0, 100.0);
		BUILDER.pop();

		BUILDER.push("Crystals");
		BUILDER.comment(" Quartz crystals turn up when digging sand. Radios are built with a pure, good or poor one: good is the",
				" baseline, pure is a little quieter, poor a little noisier and drifts off frequency (QRP radios excepted).");
		CRYSTAL_SAND_CHANCE = BUILDER.comment(" Chance that digging a sand block also drops a crystal (gravel drops flint 10% of the time)").defineInRange("crystal_sand_chance", 0.0125, 0.0, 1.0);
		CRYSTAL_POOR_WEIGHT = BUILDER.comment(" How often a crystal found in sand is poor, against the two weights below").defineInRange("crystal_poor_weight", 80, 0, 10000);
		CRYSTAL_GOOD_WEIGHT = BUILDER.comment(" How often a crystal found in sand is good").defineInRange("crystal_good_weight", 18, 0, 10000);
		CRYSTAL_PURE_WEIGHT = BUILDER.comment(" How often a crystal found in sand is pure").defineInRange("crystal_pure_weight", 2, 0, 10000);
		CRYSTAL_PURE_NOISE_FACTOR = BUILDER.comment(" Multiplier on the noise a radio with a pure crystal hears (0.95 = 5% quieter)").defineInRange("crystal_pure_noise_factor", 0.95, 0.0, 1.0);
		CRYSTAL_POOR_EXTRA_NOISE = BUILDER.comment(" Noise a poor crystal adds on top of the noise floor (0 to 1)").defineInRange("crystal_poor_extra_noise", 0.05, 0.0, 1.0);
		CRYSTAL_POOR_DRIFT_HZ = BUILDER.comment(" Furthest a poor crystal drifts off frequency, in Hz, wandering over a few minutes (SSB hears 500 Hz off as on frequency)").defineInRange("crystal_poor_drift_hz", 1000.0, 0.0, 10000.0);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

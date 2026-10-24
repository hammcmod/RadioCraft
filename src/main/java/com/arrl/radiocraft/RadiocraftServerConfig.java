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

	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_STRENGTH;
	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_ONLY_BAD_SWR;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_BAD_SWR;
	public static final ModConfigSpec.ConfigValue<Boolean> REDSTONE_INTERFERENCE_TRANSMIT_ENABLED;
	public static final ModConfigSpec.ConfigValue<Double> REDSTONE_INTERFERENCE_TRANSMIT_CHANCE;

	public static final ModConfigSpec.ConfigValue<Boolean> LIGHTNING_STATIC_ENABLED;
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
	public static final ModConfigSpec.ConfigValue<Double> AURORA_STATIC;

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
		AURORA_STATIC = BUILDER.comment(" Extra static on HF radios under the aurora, on top of the solar noise floor").defineInRange("aurora_static", 0.25, 0.0, 1.0);
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

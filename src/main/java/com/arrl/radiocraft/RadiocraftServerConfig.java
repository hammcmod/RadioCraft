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
	public static final ModConfigSpec.ConfigValue<Double> ANTENNA_TUNER_MAX_SWR;
	public static final ModConfigSpec.ConfigValue<Double> ANTENNA_TUNER_LOSS;

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
		VHF_FREQUENCY_STEP = BUILDER.comment(" The minimum step size for a VHF frequency, in Hz #default 100000").define("vhf_frequency_step", 20_000);
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

		BUILDER.push("Antenna Tuner");
		BUILDER.comment(" An antenna tuner anywhere on the coax between a radio and its antenna matches an antenna that is off resonance:",
				" the radio sees SWR 1 and most of the power a mismatch would reflect reaches the antenna instead. The further",
				" off resonance the antenna is, the more is still lost in the tuner; past antenna_tuner_max_swr it cannot match at all.");
		ANTENNA_TUNER_ENABLED = BUILDER.comment(" Set to false to make tuners decorative again").define("antenna_tuner_enabled", true);
		ANTENNA_TUNER_MAX_SWR = BUILDER.comment(" Highest antenna SWR a tuner can bring to 1:1 (a dipole 10 blocks off in total is SWR 6)").defineInRange("antenna_tuner_max_swr", 6.0, 1.0, 100.0);
		ANTENNA_TUNER_LOSS = BUILDER.comment(" Share of the mismatch loss a tuner cannot recover: 0 = perfect tuner, 1 = tuner only quiets the radio").defineInRange("antenna_tuner_loss", 0.25, 0.0, 1.0);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}

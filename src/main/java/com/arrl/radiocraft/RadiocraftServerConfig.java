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

	public record BandConfig(int wavelength, ModConfigSpec.ConfigValue<Integer> losRange, ModConfigSpec.ConfigValue<Integer> minSkipDay, ModConfigSpec.ConfigValue<Integer> maxSkipDay, ModConfigSpec.ConfigValue<Integer> minSkipNight, ModConfigSpec.ConfigValue<Integer> maxSkipNight, ModConfigSpec.ConfigValue<Integer> minFrequency, ModConfigSpec.ConfigValue<Integer> maxFrequency) {
		public Band getBand() {
			return new Band(wavelength, losRange.get(), minSkipDay.get(), maxSkipDay.get(), minSkipNight.get(), maxSkipNight.get(), minFrequency.get(), maxFrequency.get());
		}
	}

	public static final Map<Integer, BandConfig> BAND_CONFIGS = new ConcurrentHashMap<>();

	static {
		BUILDER.push("Antenna Options ( * = Restart game to take effect)");
		ANTENNA_UPDATE_DELAY = BUILDER.comment(" Delay in seconds of an antenna to recalculate it's state after being changed #default 5").define("antenna_update_delay", 5);
		BUILDER.pop();

		BUILDER.push("Band Options ( * = Restart game to take effect)");
		HF_FREQUENCY_STEP = BUILDER.comment(" The minimum step size for a HF frequency, in kHz (0.001MHz) #default 1").define("hf_frequency_step", 1);
		VHF_FREQUENCY_STEP = BUILDER.comment(" The minimum step size for a VHF frequency, in kHz (0.001MHz) #default 100").define("vhf_frequency_step", 20);
		BUILDER.pop();

		BUILDER.comment(" While wavelength and frequency are to measures of the same principle, when we refer to a band like say 10m, we mean a range of actual frequencies that aren't exactly 10m in wavelength");
		BUILDER.comment(" Band names like 10m are just for human convenience (so you don't have to say radio capable of 144MHz to 146MHz");

		BAND_CONFIGS.clear();
		for(Band band : Band.getDefaults()) {
			BUILDER.push(band.wavelength() == 2 ? "2m band settings, also known as VHF" : band.wavelength() + "m band settings");
			BAND_CONFIGS.put(band.wavelength(), new BandConfig(
					band.wavelength(),
					BUILDER.comment(" Line of sight range for this band").define(band.wavelength() + "m_los", band.losRange()),
					BUILDER.comment(" minimum range for the skip effect during the day").define(band.wavelength() + "m_minSkipDay", band.minSkipDay()),
					BUILDER.comment(" maximum range for the skip effect during the day").define(band.wavelength() + "m_maxSkipDay", band.maxSkipDay()),
					BUILDER.comment(" minimum range for the skip effect during the night").define(band.wavelength() + "m_minSkipNight", band.minSkipNight()),
					BUILDER.comment(" maximum range for the skip effect during the night").define(band.wavelength() + "m_maxSkipNight", band.maxSkipNight()),
					BUILDER.comment(" The minimum frequency for this band").define(band.wavelength() + "m_minFrequency", band.minFrequency()),
					BUILDER.comment(" The maximum frequency for this band").define(band.wavelength() + "m_maxFrequency", band.maxFrequency())
			));
			BUILDER.pop();
		}


		SPEC = BUILDER.build();
	}

}

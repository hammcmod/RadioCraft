package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Datagen class for localization files.
 */
public class RadiocraftLanguageProvider extends LanguageProvider {

	private final String locale;

	// Add translations to this map-- crude way to allow for multiple language sets without a ton of classes
	private static final Map<String, Consumer<LanguageProvider>> TRANSLATION_PROVIDERS = new HashMap<>();

	static {
		// Add translation sets here.
		TRANSLATION_PROVIDERS.put("en_us", (provider) -> {
			provider.add(Radiocraft.translationKey("tabs", "main_tab"), "RadioCraft");

			provider.addItem(RadiocraftItems.RADIO_CRYSTAL, "Radio Crystal");
			provider.addItem(RadiocraftItems.RADIO_SPEAKER, "Radio Speaker");
			provider.addItem(RadiocraftItems.HAND_MICROPHONE, "Hand Microphone");
			provider.addItem(RadiocraftItems.HF_CIRCUIT_BOARD, "HF Circuit Board");
			provider.addItem(RadiocraftItems.SMALL_BATTERY, "Small Alkaline Battery");
			provider.addItem(RadiocraftItems.FERRITE_CORE, "Ferrite Core");
			provider.addItem(RadiocraftItems.COAXIAL_CORE, "Coaxial Core");
			provider.addItem(RadiocraftItems.ANTENNA_ANALYZER, "Antenna Analyzer");
			provider.addItem(RadiocraftItems.ANTENNA_WIRE, "Antenna Wire");
			provider.addItem(RadiocraftItems.VHF_HANDHELD, "VHF Handheld Radio");

			provider.addBlock(RadiocraftBlocks.WIRE, "Wire");
			provider.addBlock(RadiocraftBlocks.WATERPROOF_WIRE, "Waterproof Wire");
			provider.addBlock(RadiocraftBlocks.SOLAR_PANEL, "Solar Panel");
			provider.addBlock(RadiocraftBlocks.SOLAR_WEATHER_STATION, "Solar Weather Station");
			provider.addBlock(RadiocraftBlocks.LARGE_BATTERY, "Large Battery");
			provider.addBlock(RadiocraftBlocks.VHF_BASE_STATION, "VHF Base Station");
			provider.addBlock(RadiocraftBlocks.VHF_RECEIVER, "VHF Receiver");
			provider.addBlock(RadiocraftBlocks.VHF_REPEATER, "VHF Repeater");
			provider.addBlock(RadiocraftBlocks.HF_RADIO_10M, "HF Radio (10m)");
			provider.addBlock(RadiocraftBlocks.HF_RADIO_20M, "HF Radio (20m)");
			provider.addBlock(RadiocraftBlocks.HF_RADIO_40M, "HF Radio (40m)");
			provider.addBlock(RadiocraftBlocks.HF_RADIO_80M, "HF Radio (80m)");
			provider.addBlock(RadiocraftBlocks.HF_RECEIVER, "HF Receiver");
			provider.addBlock(RadiocraftBlocks.ALL_BAND_RADIO, "All Band Radio");
			provider.addBlock(RadiocraftBlocks.QRP_RADIO_20M, "QRP Radio (20m)");
			provider.addBlock(RadiocraftBlocks.QRP_RADIO_40M, "QRP Radio (40m)");
			provider.addBlock(RadiocraftBlocks.ANTENNA_POLE, "Antenna Pole");
			provider.addBlock(RadiocraftBlocks.DUPLEXER, "Duplexer");
			provider.addBlock(RadiocraftBlocks.ANTENNA_TUNER, "Antenna Tuner");
			provider.addBlock(RadiocraftBlocks.ANTENNA_CONNECTOR, "Antenna Connector");
			provider.addBlock(RadiocraftBlocks.BALUN_ONE_TO_ONE, "Balun (1:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_TWO_TO_ONE, "Balun (2:1)");
			provider.addBlock(RadiocraftBlocks.COAX_WIRE, "Coaxial Wire");
			provider.addBlock(RadiocraftBlocks.DIGITAL_INTERFACE, "Digital Interface (TNC)");
			provider.addBlock(RadiocraftBlocks.CHARGE_CONTROLLER, "Charge Controller");
			provider.addBlock(RadiocraftBlocks.MICROPHONE, "Microphone");
			provider.addBlock(RadiocraftBlocks.YAGI_ANTENNA, "Yagi Antenna");
			provider.addBlock(RadiocraftBlocks.J_POLE_ANTENNA, "J-Pole Antenna");
			provider.addBlock(RadiocraftBlocks.SLIM_JIM_ANTENNA, "Slim Jim Antenna");

			provider.addEntityType(RadiocraftEntityTypes.ANTENNA_WIRE, "Antenna Wire");

			provider.add(Radiocraft.translationKey("commands", "callsign.list.empty"), "No callsigns found");
			provider.add(Radiocraft.translationKey("commands", "callsign.get.success"), "%s's callsign is %s of license class %s.");
			provider.add(Radiocraft.translationKey("commands", "callsign.get.failure"), "%s does not have a callsign.");
			provider.add(Radiocraft.translationKey("commands", "callsign.get.failure.multiple"), "Cannot get the callsign of multiple targets at once.");
            provider.add(Radiocraft.translationKey("commands", "callsign.get.failure.permission"), "You must have permission to change callsigns to use this command.");
			provider.add(Radiocraft.translationKey("commands", "callsign.reset.success"), "%s's callsign has been reset.");
			provider.add(Radiocraft.translationKey("commands", "callsign.reset.failure.multiple"), "Cannot reset the callsign of multiple targets at once.");
			provider.add(Radiocraft.translationKey("commands", "callsign.set.success"), "%s's callsign has been set to %s and license class %s.");
			provider.add(Radiocraft.translationKey("commands", "callsign.set.failure.multiple"), "Cannot reset the callsign of multiple targets at once.");
			provider.add(Radiocraft.translationKey("commands", "solarweather.success"), "Event: %s\nProgress: %s\nDuration: %s\nNoise Floor: %s");

			provider.add(Radiocraft.translationKey("screen", "chargecontroller.power"), "%s FE/s");
			provider.add(Radiocraft.translationKey("screen", "radio.tx"), "Transmitting");
			provider.add(Radiocraft.translationKey("screen", "radio.rx"), "Receiving");

			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.network_found"), "Antenna network at %s has %s antennas");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.handheld_with_stats"), "You are holding a handheld radio, the antenna is %s m long and it has an SWR of %s");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.no_handheld"), "You are not holding a handheld radio");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.unknown_handheld_antenna"), "You are holding a handheld radio, but it's using an antenna we know nothing about.");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.no_networks_at"), "No antenna networks found at %s.");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.development_summary"), "There are %s networks in the world: %s");

            provider.add(Radiocraft.translationKey("tooltip", "antenna_analyzer"), "Used for analyzing antenna networks and handheld radio antennas");

            provider.add(Radiocraft.translationKey("tooltip", "not_implemented"), "§7Not Implemented (Decorative Only)§r");
            provider.add(Radiocraft.translationKey("tooltip", "not_implemented_crafting_only"), "§7Not Implemented (Crafting Ingredient Only)§r");
            provider.add(Radiocraft.translationKey("tooltip", "energy_stored"), "Energy: %s / %s FE");
            provider.add(Radiocraft.translationKey("tooltip", "energy_stored_joules"), "Energy: %s / %s J");
            provider.add(Radiocraft.translationKey("tooltip", "battery_percentage"), "Charge: %s%%");
			provider.add(Radiocraft.translationKey("tooltip", "small_battery"), "A common small battery, typically starts with 50-70% charge");
			provider.add(Radiocraft.translationKey("tooltip", "vhf_handheld_battery_swap"), "Click battery on VHF Handheld to swap batteries");

			provider.add(Radiocraft.translationKey("message", "battery_swapped"), "Battery swapped");
            provider.add(Radiocraft.translationKey("message", "radio_battery_empty"), "Radio battery depleted");

			// Advancements – RadioCraft progression
			provider.add("advancement.radiocraft.root.title", "RadioCraft Adventures");
			provider.add("advancement.radiocraft.root.description", "Embark on on-air operations with RadioCraft gear");
			// SOTA / BOTA branches
			provider.add("advancement.radiocraft.vhf_sota.title", "Summits On The Air");
			provider.add("advancement.radiocraft.vhf_sota.description", "Transmit with a VHF handheld from Y128 or higher");
			provider.add("advancement.radiocraft.bota_all_overworld.title", "Overworld BOTA Award");
			provider.add("advancement.radiocraft.bota_all_overworld.description", "Operate your VHF handheld from every overworld biome");
			provider.add("advancement.radiocraft.bota_all_nether.title", "Nether BOTA Award");
			provider.add("advancement.radiocraft.bota_all_nether.description", "Operate your VHF handheld from every Nether biome");
			provider.add("advancement.radiocraft.bota_biome_types.title", "Biome Explorer");
			provider.add("advancement.radiocraft.bota_biome_types.description", "Transmit from an arid, wetland, flatland, woodland, highland, and ocean biome");
			provider.add("advancement.radiocraft.bota_legacy.title", "Legacy Biomes Award");
			provider.add("advancement.radiocraft.bota_legacy.description", "Complete VHF operations from every legacy 1.7 era overworld biome");
			provider.add("advancement.radiocraft.bota_arid.title", "Arid Activator");
			provider.add("advancement.radiocraft.bota_arid.description", "Log a VHF handheld transmission from an arid biome");
			provider.add("advancement.radiocraft.bota_wetland.title", "Wetland Watch");
			provider.add("advancement.radiocraft.bota_wetland.description", "Log a VHF handheld transmission while knee-deep in a wetland");
			provider.add("advancement.radiocraft.bota_flatland.title", "Flatland Frequency");
			provider.add("advancement.radiocraft.bota_flatland.description", "Log a VHF handheld transmission from the open plains");
			provider.add("advancement.radiocraft.bota_woodland.title", "Woodland Whisper");
			provider.add("advancement.radiocraft.bota_woodland.description", "Log a VHF handheld transmission under the forest canopy");
			provider.add("advancement.radiocraft.bota_highland.title", "Highland Howl");
			provider.add("advancement.radiocraft.bota_highland.description", "Log a VHF handheld transmission from a windswept highland");
			provider.add("advancement.radiocraft.bota_ocean.title", "Ocean Operator");
			provider.add("advancement.radiocraft.bota_ocean.description", "Log a VHF handheld transmission over coastal waters");

			// Jade configuration translations
			provider.add("config.jade.plugin_radiocraft.radiocraft", "RadioCraft");
			provider.add("config.jade.plugin_radiocraft.antenna_wire", "Antenna Wire");

			Arrays.stream(LicenseClass.values()).forEach((licenseClass) -> {
                // I'm not going to bother pulling in the Apache Commons Text library, because I really don't want to shadow it in
                @SuppressWarnings("deprecation")
				String friendlyClassName = WordUtils.capitalizeFully(licenseClass.name().replace("_", " "));
				provider.add(Radiocraft.translationKey("license_class", licenseClass.name()), friendlyClassName);
			});
		});
	}

	public RadiocraftLanguageProvider(PackOutput output, String locale) {
		super(output, Radiocraft.MOD_ID, locale);
		this.locale = locale;
	}

	@Override
	protected void addTranslations() {
		// No error checking, I want a hard crash here if datagen has something bad as it's all hardcoded and should not run if erroring.
		// Only runs with datagen run config
		TRANSLATION_PROVIDERS.get(locale).accept(this);
	}

}

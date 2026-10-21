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

			provider.addItem(RadiocraftItems.PASSTHROUGH, "Wire Passthrough");
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
			provider.addBlock(RadiocraftBlocks.BALUN_FOUR_TO_ONE, "Balun (4:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_TWO_TO_ONE, "Balun (2:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_NINE_TO_ONE, "Balun (9:1)");
			provider.addBlock(RadiocraftBlocks.COAX_WIRE, "Coaxial Wire");
			provider.addBlock(RadiocraftBlocks.DIGITAL_INTERFACE, "Digital Interface (TNC)");
			provider.addBlock(RadiocraftBlocks.PARROT_CONTROLLER, "Simplex Repeat Controller");
			provider.addBlock(RadiocraftBlocks.CHARGE_CONTROLLER, "Charge Controller");
			provider.addBlock(RadiocraftBlocks.MICROPHONE, "Microphone");
			provider.addBlock(RadiocraftBlocks.YAGI_ANTENNA, "Yagi Antenna");
			provider.addBlock(RadiocraftBlocks.J_POLE_ANTENNA, "J-Pole Antenna");
			provider.addBlock(RadiocraftBlocks.SLIM_JIM_ANTENNA, "Slim Jim Antenna");
			provider.addBlock(RadiocraftBlocks.SATELLITE_DISH, "Satellite Dish");
			provider.addBlock(RadiocraftBlocks.DESK_CHARGER, "Desk Charger");

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
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.failure.multiple"), "Cannot generate callsigns for multiple targets at once.");
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.success"), "%s now has callsign %s and license class %s.");
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.existing"), "%s already has callsign %s and license class %s.");
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.collision"), "%s could not be assigned callsign %s because it is already used by %s. Please assign a unique callsign manually (e.g., %s).");
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.online.empty"), "No players are currently online.");
			provider.add(Radiocraft.translationKey("commands", "callsign.generate.online.none"), "All online players already have callsigns.");
			provider.add(Radiocraft.translationKey("commands", "solarweather.success"), "Event: %s\nProgress: %s\nDuration: %s\nNoise Floor: %s");

			provider.add(Radiocraft.translationKey("screen", "chargecontroller.power"), "%s FE/s");
			provider.add(Radiocraft.translationKey("screen", "radio.tx"), "Transmitting");
			provider.add(Radiocraft.translationKey("screen", "radio.rx"), "Receiving");

			provider.add(Radiocraft.translationKey("screen", "desk_charger"), "Desk Charger");
			
			// Container titles for new GUIs
			provider.add("container.radiocraft.digital_interface", "Digital Interface (TNC)");
			provider.add("container.radiocraft.duplexer", "Duplexer");
			provider.add("container.radiocraft.antenna_tuner", "Antenna Tuner");

			// Antenna tuner readout (indices are AntennaTunerBlockEntity.STATUS_*)
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.0"), "No radio on this coax");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.1"), "Radio is off");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.2"), "No antenna recognized");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.3"), "Matched");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.4"), "Out of range: no match");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.5"), "Overdraw: too many radios or antennas");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.6"), "Tuners disabled on this server");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.antenna_swr"), "Antenna SWR: %s");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.radio_swr"), "Radio sees: %s");
			provider.add(Radiocraft.translationKey("tooltip", "antenna_tuner"), "Put it anywhere on the coax between a radio and its antenna. Matches an antenna that is a little off resonance so the radio sees SWR 1; the further off, the more power is lost in the tuner.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_one_to_one"), "Feed point for dipoles, inverted Vs, end-feds and quarter wave verticals.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_four_to_one"), "Feed point for off-centre-fed dipoles: short arm a third to two thirds of the long one, matched on the even harmonics too.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_two_to_one"), "Feed point for quad loops.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_nine_to_one"), "Feed point for a random wire: one wire of any shape, at least half a wavelength long. Needs an antenna tuner.");
			provider.add("container.radiocraft.vhf_repeater", "VHF Repeater");
			
			// Digital Interface tab labels
			provider.add(Radiocraft.translationKey("gui", "tab.arps"), "ARPS");
			provider.add(Radiocraft.translationKey("gui", "tab.msg"), "MSG");
			provider.add(Radiocraft.translationKey("gui", "tab.rtty"), "RTTY");
			provider.add(Radiocraft.translationKey("gui", "tab.files"), "FILES");

			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.network_found"), "Antenna network at %s has %s antennas");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.handheld_with_stats"), "You are holding a handheld radio; the antenna is %s m long. SWR at %s MHz is %s");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.no_handheld"), "You are not holding a handheld radio");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.unknown_handheld_antenna"), "You are holding a handheld radio, but it's using an antenna we know nothing about.");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.no_networks_at"), "No antenna networks found at %s.");
			provider.add(Radiocraft.translationKey("message", "antenna_analyzer.development_summary"), "There are %s networks in the world: %s");

            provider.add(Radiocraft.translationKey("tooltip", "antenna_analyzer"), "Used for analyzing antenna networks and handheld radio antennas");
            provider.add(Radiocraft.translationKey("tooltip", "parrot_controller"), "Repeats received messages back out the connected radio. Affectionately called a \"parrot\" repeater.");
            provider.add(Radiocraft.translationKey("message", "parrot_controller.delay"), "Repeat delay: %ss");

            provider.add(Radiocraft.translationKey("tooltip", "solar_weather_station"), "Right click for the current solar conditions. A comparator reads 1 (blackout) to 6 (great).");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.report"), "Solar conditions: %s (noise floor %s%%). %s band: %s");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.no_data"), "No solar weather data yet");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.open"), "open");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.closed"), "closed");
            provider.add(Radiocraft.translationKey("solar_event", "none"), "None");
            provider.add(Radiocraft.translationKey("solar_event", "great"), "Great");
            provider.add(Radiocraft.translationKey("solar_event", "good"), "Good");
            provider.add(Radiocraft.translationKey("solar_event", "average"), "Average");
            provider.add(Radiocraft.translationKey("solar_event", "fair"), "Fair");
            provider.add(Radiocraft.translationKey("solar_event", "poor"), "Poor");
            provider.add(Radiocraft.translationKey("solar_event", "blackout"), "Blackout");

            provider.add(Radiocraft.translationKey("tooltip", "not_implemented"), "§7Not Implemented (Decorative Only)§r");
            provider.add(Radiocraft.translationKey("tooltip", "not_implemented_crafting_only"), "§7Not Implemented (Crafting Ingredient Only)§r");
            provider.add(Radiocraft.translationKey("tooltip", "passthrough"), "Use on a plain solid block: it keeps its look but lets wire and coax run straight through it. Break it to get both back.");
            provider.add(Radiocraft.translationKey("tooltip", "wire"), "Carries power between solar panels, charge controllers, batteries and radios. Runs along the ground, up walls and over steps.");
            provider.add(Radiocraft.translationKey("tooltip", "waterproof_wire"), "Power wire that can be laid under water. Runs along the ground, up walls and over steps.");
            provider.add(Radiocraft.translationKey("tooltip", "coax_wire"), "Carries the signal between a radio and its antenna's balun. Runs along the ground, up walls and over steps.");
            provider.add(Radiocraft.translationKey("tooltip", "antenna_pole"), "Holds the end of an antenna wire. Use antenna wire on this and another holder to string a wire between them.");
            provider.add(Radiocraft.translationKey("tooltip", "antenna_connector"), "Joins antenna wires and coax: a wire holder you can also run coax into.");
            provider.add(Radiocraft.translationKey("tooltip", "j_pole_antenna"), "VHF antenna for 2m base stations and repeaters. Connect it with coax.");
            provider.add(Radiocraft.translationKey("tooltip", "slim_jim_antenna"), "VHF antenna for 2m base stations and repeaters. Connect it with coax.");
            provider.add(Radiocraft.translationKey("tooltip", "yagi_antenna"), "Directional VHF antenna: strongest in the direction it points. Connect it with coax.");
            provider.add(Radiocraft.translationKey("tooltip", "solar_panel"), "Makes power in daylight. Wire it to a charge controller.");
            provider.add(Radiocraft.translationKey("tooltip", "charge_controller"), "Sits between solar panels and batteries and manages the charge. Wire panels in and batteries out.");
            provider.add(Radiocraft.translationKey("tooltip", "large_battery"), "Stores power from the charge controller. Radios do not draw power yet, so a station runs without one for now.");
            provider.add(Radiocraft.translationKey("tooltip", "hf_radio_10m"), "HF transceiver for the 10m band (28 MHz). SSB voice and CW. Needs a 10m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "hf_radio_20m"), "HF transceiver for the 20m band (14 MHz). SSB voice and CW. Needs a 20m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "hf_radio_40m"), "HF transceiver for the 40m band (7 MHz). SSB voice and CW. Needs a 40m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "hf_radio_80m"), "HF transceiver for the 80m band (3.5 MHz). SSB voice and CW. Needs an 80m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "qrp_radio_20m"), "Low power 20m transceiver: shorter range, lighter to build. Needs a 20m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "qrp_radio_40m"), "Low power 40m transceiver: shorter range, lighter to build. Needs a 40m antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "hf_receiver"), "Listens on the HF bands. Receive only.");
            provider.add(Radiocraft.translationKey("tooltip", "vhf_base_station"), "2m FM transceiver for local contacts and repeaters. Needs a VHF antenna on coax.");
            provider.add(Radiocraft.translationKey("tooltip", "vhf_receiver"), "Listens on 2m FM. Receive only.");
            provider.add(Radiocraft.translationKey("tooltip", "energy_stored"), "Energy: %s / %s FE");
            provider.add(Radiocraft.translationKey("tooltip", "energy_stored_joules"), "Energy: %s / %s J");
            provider.add(Radiocraft.translationKey("tooltip", "battery_percentage"), "Charge: %s%%");
			provider.add(Radiocraft.translationKey("tooltip", "small_battery"), "A common small battery, typically starts with 50-70% charge");
			provider.add(Radiocraft.translationKey("tooltip", "vhf_handheld_battery_swap"), "Click battery on VHF Handheld to swap batteries");

			provider.add(Radiocraft.translationKey("message", "battery_swapped"), "Battery swapped");
            provider.add(Radiocraft.translationKey("message", "radio_battery_empty"), "Radio battery depleted");

			// Jade configuration translations
			provider.add("config.jade.plugin_radiocraft.radiocraft", "RadioCraft");
			provider.add("config.jade.plugin_radiocraft.antenna_wire", "Antenna Wire");
			provider.add("config.jade.plugin_radiocraft.antenna_info", "Antenna Info");
			provider.add("config.jade.plugin_radiocraft.radio", "Radio");
			provider.add("config.jade.plugin_radiocraft.coax_wire", "Coax Wire");
			provider.add("config.jade.plugin_radiocraft.parrot_controller", "Simplex Repeat Controller");
			provider.add("config.jade.plugin_radiocraft.solar_weather_station", "Solar Weather Station");
			provider.add("config.jade.plugin_radiocraft.battery", "Battery Charge");
			provider.add("config.jade.plugin_radiocraft.solar_panel", "Solar Panel Output");

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

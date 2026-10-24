package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.LicenseClass;
import com.arrl.radiocraft.common.init.CrystalVariants;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftEntityTypes;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalBand;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
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
			for(CrystalBand band : CrystalBand.values())
				for(CrystalQuality quality : CrystalQuality.values()) {
					provider.addItem(RadiocraftItems.CRYSTALS.get(band).get(quality), quality.getLabel() + " " + band.getLabel() + " Crystal");
					provider.addItem(RadiocraftItems.ENCASED_CRYSTALS.get(band).get(quality), "Encased " + quality.getLabel() + " " + band.getLabel() + " Crystal");
				}
			provider.addItem(RadiocraftItems.RADIO_SPEAKER, "Radio Speaker");
			provider.addItem(RadiocraftItems.HAND_MICROPHONE, "Hand Microphone");
			provider.addItem(RadiocraftItems.CORDED_MICROPHONE, "Hand Microphone");
			provider.add(Radiocraft.translationKey("tooltip", "corded_microphone"), "On its radio's cord: hold right click to talk, right click the radio to hang it up. The cord reaches 5 blocks.");
			provider.add(Radiocraft.translationKey("message", "mic_in_use"), "Someone else has this radio's microphone");
			provider.add(Radiocraft.translationKey("message", "mic_hand_full"), "Empty your hand to take the microphone");
			provider.addItem(RadiocraftItems.HF_CIRCUIT_BOARD, "HF Circuit Board");
			provider.addItem(RadiocraftItems.ALL_BAND_CIRCUIT_BOARD, "All-Band Circuit Board");
			provider.addItem(RadiocraftItems.SMALL_BATTERY, "Small Alkaline Battery");
			provider.addItem(RadiocraftItems.FERRITE_CORE, "Ferrite Core");
			provider.addItem(RadiocraftItems.COAXIAL_CORE, "Coaxial Core");
			provider.addItem(RadiocraftItems.INSULATION, "Insulation");
			provider.addItem(RadiocraftItems.MICROPHONE_ELEMENT, "Microphone Element");
			provider.addItem(RadiocraftItems.ANTENNA_ANALYZER, "Antenna Analyzer");
			provider.addItem(RadiocraftItems.ANTENNA_WIRE, "Antenna Wire");
			addCrystalRadio(provider, RadiocraftItems.VHF_HANDHELD_VARIANTS, "VHF Handheld Radio");

			provider.addBlock(RadiocraftBlocks.WIRE, "Wire");
			provider.addBlock(RadiocraftBlocks.WATERPROOF_WIRE, "Waterproof Wire");
			provider.addBlock(RadiocraftBlocks.SOLAR_PANEL, "Solar Panel");
			provider.addBlock(RadiocraftBlocks.TELETYPE, "Teletype Machine");
			provider.addBlock(RadiocraftBlocks.LARGE_BATTERY, "Large Battery");
			addCrystalRadio(provider, RadiocraftItems.VHF_BASE_STATION_VARIANTS, "VHF Base Station");
			provider.addBlock(RadiocraftBlocks.VHF_RECEIVER, "VHF Receiver");
			provider.addBlock(RadiocraftBlocks.VHF_REPEATER, "VHF Repeater");
			addCrystalRadio(provider, RadiocraftItems.HF_RADIO_10M_VARIANTS, "10m Radio");
			addCrystalRadio(provider, RadiocraftItems.HF_RADIO_20M_VARIANTS, "20m Radio");
			addCrystalRadio(provider, RadiocraftItems.HF_RADIO_40M_VARIANTS, "40m Radio");
			addCrystalRadio(provider, RadiocraftItems.HF_RADIO_80M_VARIANTS, "80m Radio");
			provider.addBlock(RadiocraftBlocks.HF_RECEIVER, "HF Receiver");
			provider.addBlock(RadiocraftBlocks.ALL_BAND_RADIO, "All Band Radio");
			provider.addBlock(RadiocraftBlocks.QRP_RADIO_20M, "QRP Radio (20m)");
			provider.addBlock(RadiocraftBlocks.QRP_RADIO_40M, "QRP Radio (40m)");
			provider.addBlock(RadiocraftBlocks.ANTENNA_POLE, "Antenna Pole");
			provider.addBlock(RadiocraftBlocks.TOWER_FOOTHOLD, "Tower Foothold");
			provider.addBlock(RadiocraftBlocks.OLD_TOWER_FOOTHOLD, "Old Tower Foothold");
			provider.addBlock(RadiocraftBlocks.DUPLEXER, "Duplexer");
			provider.addBlock(RadiocraftBlocks.ANTENNA_TUNER, "Antenna Tuner");
			provider.addBlock(RadiocraftBlocks.AUTO_TUNER, "Automatic Antenna Tuner");
			provider.addBlock(RadiocraftBlocks.ANTENNA_CONNECTOR, "Antenna Connector");
			provider.addBlock(RadiocraftBlocks.BALUN_ONE_TO_ONE, "Balun (1:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_FOUR_TO_ONE, "Balun (4:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_TWO_TO_ONE, "Balun (2:1)");
			provider.addBlock(RadiocraftBlocks.BALUN_NINE_TO_ONE, "Balun (9:1)");
			provider.addBlock(RadiocraftBlocks.COAX_WIRE, "Coaxial Wire");
			provider.addBlock(RadiocraftBlocks.RADIO_INTERFACE, "Radio Interface");
			provider.addBlock(RadiocraftBlocks.APRS_TERMINAL, "H19 APRS Terminal");
			provider.addBlock(RadiocraftBlocks.TRANSFER_INTERFACE, "Digital Transfer Interface");
			provider.addBlock(RadiocraftBlocks.PARROT_CONTROLLER, "Simplex Repeat Controller");
			provider.addBlock(RadiocraftBlocks.CHARGE_CONTROLLER, "Charge Controller");
			provider.addBlock(RadiocraftBlocks.MICROPHONE, "Microphone");
			provider.addBlock(RadiocraftBlocks.YAGI_ANTENNA, "Yagi Antenna");
			provider.addBlock(RadiocraftBlocks.J_POLE_ANTENNA, "J-Pole Antenna");
			provider.addBlock(RadiocraftBlocks.DUMMY_LOAD, "Dummy Load");
			provider.add("tooltip.radiocraft.dummy_load", "A perfect match on every band that hardly radiates: about 10 blocks. Fits on a radio or its coax.");
			provider.addBlock(RadiocraftBlocks.SLIM_JIM_ANTENNA, "Slim Jim Antenna");
			provider.addBlock(RadiocraftBlocks.SATELLITE_DISH, "Satellite Dish");
			provider.addBlock(RadiocraftBlocks.IONOSONDE, "Ionosonde Antenna");
			provider.addBlock(RadiocraftBlocks.FIVE_BTV_ANTENNA, "5-BTV Multiband HF Antenna");
			provider.addBlock(RadiocraftBlocks.IONOSPHERIC_PROCESSOR, "Ionospheric Processor");
			provider.addBlock(RadiocraftBlocks.WEFAX_RECEIVER, "WEFAX Receiver");
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
			provider.add("container.radiocraft.radio_interface", "Radio Interface");
			provider.add("container.radiocraft.aprs_terminal", "H19 APRS Terminal");
			provider.add("container.radiocraft.teletype", "Teletype Machine");
			provider.add(Radiocraft.translationKey("screen", "teletype.set_callsign_first"), "Set the teletype's callsign first");
			provider.add(Radiocraft.translationKey("screen", "teletype.no_radio"), "No radio switched on next to the teletype");
			provider.add(Radiocraft.translationKey("screen", "teletype.send_failed"), "Nothing sent: the radio is busy or cannot take RTTY");
			provider.add(Radiocraft.translationKey("screen", "teletype.paper"), "PAPER: %s LINES");
			provider.add(Radiocraft.translationKey("screen", "teletype.paper_out"), "PAPER OUT");
			provider.add(Radiocraft.translationKey("screen", "teletype.edit_callsign"), "TELETYPE CALLSIGN: ENTER saves, empty claims yours");
			provider.add(Radiocraft.translationKey("screen", "teletype.tip.solar"), "SOLAR WX: print the solar weather (needs a radio with an antenna next to it)");
			provider.add(Radiocraft.translationKey("screen", "teletype.tip.callsign"), "Callsign: click to change (empty claims your own)");
			provider.add(Radiocraft.translationKey("screen", "teletype.tip.set_by"), "%s, set by %s");
			provider.add(Radiocraft.translationKey("screen", "teletype.tip.paper_out"), "Out of paper: right click the teletype with paper, or feed it with a hopper");
			provider.add("item.radiocraft.aprs_map", "APRS Map");
			provider.add("item.radiocraft.aprs_map.from", "From the H19 at %s, %s, %s");
			provider.add("item.radiocraft.aprs_map.wall", "APRS Map (%s x %s wall)");
			provider.add("item.radiocraft.aprs_map.tile", "Row %s, column %s of %s x %s (top left is north west)");
			provider.add("container.radiocraft.transfer_interface", "Digital Transfer Interface");
			provider.add("container.radiocraft.duplexer", "Duplexer");
			provider.add("container.radiocraft.antenna_tuner", "Antenna Tuner");

			// Antenna tuner readout (indices are AntennaTunerBlockEntity.STATUS_*)
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.0"), "No radio on this coax");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.1"), "Radio is off");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.2"), "No antenna recognized");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.3"), "Tuned");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.4"), "Antenna too far off to match: tuned as close as it goes");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.5"), "Overdraw: too many radios or antennas");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.6"), "Tuners disabled on this server");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.antenna_swr"), "Antenna SWR: %s");
			provider.add(Radiocraft.translationKey("screen", "antenna_tuner.status.7"), "Turn the dials: green is a match");
			provider.add(Radiocraft.translationKey("tooltip", "antenna_tuner"), "Put it anywhere on the coax between a radio and its antenna. Turn its three dials until the lamp is solid green: red blinks faster as you get closer, yellow-green below SWR 3. The further off resonance the antenna, the more power is lost in the tuner.");
			provider.add(Radiocraft.translationKey("tooltip", "auto_tuner"), "Put it on the coax between a radio and its antenna, with power. Tunes itself whenever the radio changes band or moves 1 MHz. Shows TUNE FAIL on the radio when the antenna is too far off.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_one_to_one"), "Feed point for dipoles, inverted Vs, end-feds, quarter wave verticals and Moxons.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_four_to_one"), "Feed point for off-centre-fed dipoles (short arm a third to two thirds of the long one, matched on the even harmonics too) and delta loops.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_two_to_one"), "Feed point for quad loops.");
			provider.add(Radiocraft.translationKey("tooltip", "balun_nine_to_one"), "Feed point for a random wire: one wire of any shape, at least half a wavelength long. Needs an antenna tuner.");
			provider.add("container.radiocraft.vhf_repeater", "VHF Repeater");
			provider.add(Radiocraft.translationKey("screen", "repeater.rx_frequency"), "Input (receive) frequency, MHz");
			provider.add(Radiocraft.translationKey("screen", "repeater.tx_frequency"), "Output (transmit) frequency, MHz");
			provider.add(Radiocraft.translationKey("screen", "repeater.frequency"), "Frequency");
			provider.add(Radiocraft.translationKey("screen", "repeater.callsign"), "Callsign, sent in Morse");
			provider.add(Radiocraft.translationKey("screen", "repeater.rx_tone"), "Access tone (CTCSS RX)");
			provider.add(Radiocraft.translationKey("screen", "repeater.tx_tone"), "Output tone (CTCSS TX)");
			provider.add(Radiocraft.translationKey("screen", "repeater.id_off"), "ID: off");
			provider.add(Radiocraft.translationKey("screen", "repeater.id_every"), "ID every %s min");
			provider.add(Radiocraft.translationKey("screen", "repeater.hang"), "Hang time: %s s");
			provider.add(Radiocraft.translationKey("screen", "repeater.timeout"), "Timeout: %s");
			provider.add(Radiocraft.translationKey("screen", "repeater.timeout_off"), "Timeout: off");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.power"), "Power");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.prt_rx"), "Input frequency: %s MHz");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.prt_tx"), "Output frequency: %s MHz");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.cw_id"), "Callsign ID: %s");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.ctc_rx"), "Access tone: %s");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.ctc_tx"), "Output tone: %s");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.mic"), "Mic: talk out through the repeater from here");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.tx"), "Transmitter: repeat what comes in");
			provider.add(Radiocraft.translationKey("screen", "repeater.tip.speaker"), "Speaker: listen to the input here");

			// Radio interface, APRS terminal and transfer interface
			provider.add(Radiocraft.translationKey("screen", "station.no_interface"), "Not joined to a Radio Interface: place it touching one, or touching a terminal that is");
			provider.add(Radiocraft.translationKey("screen", "station.set_callsign_first"), "Set a callsign on the Radio Interface first");
			provider.add(Radiocraft.translationKey("screen", "station.no_radio"), "No radio switched on next to the Radio Interface");
			provider.add(Radiocraft.translationKey("screen", "station.send_failed"), "Nothing sent: check the message, and that the Radio Interface is set to TX");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.no_callsign"), "You have no callsign to claim it with");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.callsign"), "CALLSIGN");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.callsign_hint"), "Enter saves; leave it empty to claim your own");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.set_by"), "Set by %s");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.tx_on"), "TX ON");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.tx_off"), "TX OFF: LISTEN ONLY");
			provider.add(Radiocraft.translationKey("screen", "radio_interface.tip.tx"), "Switch off to only listen: nothing goes out from this station, digipeats included");
			provider.add(Radiocraft.translationKey("screen", "aprs.no_beacons"), "No beacons heard");
			provider.add(Radiocraft.translationKey("screen", "aprs.no_messages"), "No messages");
			provider.add(Radiocraft.translationKey("screen", "aprs.edit_comment"), "BEACON COMMENT");
			provider.add(Radiocraft.translationKey("screen", "aprs.edit_keys"), "ENTER saves, ESC cancels");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.beacon"), "Beacon now: send this station's position");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.unread"), "New message! Open MSG to read it");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.no_unread"), "No new messages");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.callsign"), "Station callsign: set on the Radio Interface");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.no_interface"), "Not joined to a Radio Interface");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.comment"), "Beacon comment, up to 30 characters: click to change");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.icon"), "Map icon: click for the next one");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.beacon_enable"), "Beacon by itself every few minutes (a redstone pulse beacons too)");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.digipeat_on"), "Digipeating ON: the Radio Interface repeats packets heard (click to stop)");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.digipeat_off"), "Digipeating OFF: click to have the Radio Interface repeat packets heard, up to 8 hops");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.rpt"), "RPT: lit while repeating a packet");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.heard"), "Heard %s min ago, direct");
			provider.add(Radiocraft.translationKey("screen", "aprs.tip.heard_via"), "Heard %s min ago, via %s digipeater(s)");
			provider.add(Radiocraft.translationKey("screen", "transfer_interface.busy"), "A file is already going out or coming in");
			provider.add(Radiocraft.translationKey("screen", "transfer_interface.nothing_to_send"), "Put a book, paper or map in the SEND slot");
			provider.add(Radiocraft.translationKey("commands", "aprs.sent"), "APRS packet delivered to %s APRS terminal(s)");
			provider.add(Radiocraft.translationKey("commands", "aprs.bad_frequency"), "Frequency must be in MHz (such as 144.390) or \"all\"");
			
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
            provider.add(Radiocraft.translationKey("message", "parrot_controller.linked"), "Linked to the radio to the %s");
            for(String side : new String[] {"north", "east", "south", "west"})
                provider.add(Radiocraft.translationKey("message", "side." + side), side);

            provider.add(Radiocraft.translationKey("tooltip", "teletype"), "RTTY through the radios it touches, printed on its own paper. SOLAR WX prints the solar conditions, which it reads through a radio with an antenna (not a dummy load) next to it. A comparator reads 1 (blackout) to 6 (great).");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.report"), "Solar conditions: %s (noise floor %s%%). %s band: %s");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.no_data"), "No solar weather data yet");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.no_radio"), "No signal: the station needs a radio next to it with an antenna (not a dummy load)");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.good"), "Good");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.fair"), "Fair");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.poor"), "Poor");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.closed"), "Closed");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.normal"), "Normal");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.aurora"), "Aurora");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.aurora_active"), "Aurora: active beyond Z ±%s");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.aurora_expected"), "Aurora expected: storm on the way, watch beyond Z ±%s");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.header"), "Solar Conditions:");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.overall"), "Overall: %s");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.noise"), "Noise Floor: %s%%");
            provider.add(Radiocraft.translationKey("message", "solar_weather_station.band"), "%s Band: %s");
            provider.add("subtitles.radiocraft.teletype", "Teletype printing");
            provider.add("subtitles.radiocraft.teletype_ding", "Teletype bell");
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
            provider.add("key.categories.radiocraft", "RadioCraft");
            provider.add("key.radiocraft.worn_ptt", "Talk on worn radio");
            provider.add(Radiocraft.translationKey("tooltip", "microphone"), "Stands beside a radio. Hold right click on it to talk; sneak and right click to lock talking on. Speak close: it gets quieter as you step away. Lets go after a timeout.");
            provider.add(Radiocraft.translationKey("message", "desk_mic.locked"), "Desk mic locked on (lets go after %ss)");
            provider.add(Radiocraft.translationKey("message", "desk_mic.unlocked"), "Desk mic released");
            provider.add(Radiocraft.translationKey("tooltip", "tower_foothold"), "A rung for climbing a tower, or standing on. Place it against a wall like a ladder.");
            provider.add(Radiocraft.translationKey("tooltip", "old_tower_foothold"), "Weathered. Now and then one gives way under a climber.");
            provider.add(Radiocraft.translationKey("commands", "guide.given"), "Gave %s the RadioCraft guide");
            provider.add(Radiocraft.translationKey("commands", "guide.no_patchouli"), "The guide book needs the Patchouli mod, which is not installed");
            provider.add(Radiocraft.translationKey("tooltip", "antenna_pole"), "Holds the end of an antenna wire. Use antenna wire on this and another holder to string a wire between them.");
            provider.add(Radiocraft.translationKey("tooltip", "antenna_connector"), "Joins antenna wires and coax: a wire holder you can also run coax into.");
            provider.add(Radiocraft.translationKey("tooltip", "j_pole_antenna"), "VHF antenna for 2m base stations and repeaters. Connect it with coax.");
            provider.add(Radiocraft.translationKey("tooltip", "five_btv_antenna"), "Trap vertical, seven blocks tall: matched on every HF band and VHF. On HF it sends its power low, so it reaches less by line of sight and further by skip. Coax goes to the base.");
            provider.add(Radiocraft.translationKey("tooltip", "ionosonde"), "Sounding antenna for the Ionospheric Processor. On a radio it hears VHF well but matches poorly everywhere, worst on HF.");
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
			provider.add(Radiocraft.translationKey("tooltip", "crystal_pure"), "Pure crystal: a little less noise.");
			provider.add(Radiocraft.translationKey("tooltip", "crystal_poor"), "Poor crystal: a little more noise, and it drifts off frequency.");

			provider.add(Radiocraft.translationKey("message", "battery_swapped"), "Battery swapped");
            provider.add(Radiocraft.translationKey("message", "radio_battery_empty"), "Radio battery depleted");

			// Jade configuration translations
			provider.add("config.jade.plugin_radiocraft.radiocraft", "RadioCraft");
			provider.add("config.jade.plugin_radiocraft.antenna_wire", "Antenna Wire");
			provider.add("config.jade.plugin_radiocraft.antenna_info", "Antenna Info");
			provider.add("config.jade.plugin_radiocraft.radio", "Radio");
			provider.add("config.jade.plugin_radiocraft.coax_wire", "Coax Wire");
			provider.add("config.jade.plugin_radiocraft.parrot_controller", "Simplex Repeat Controller");
			provider.add("config.jade.plugin_radiocraft.teletype", "Teletype Machine");
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

	/** Names a radio's crystal versions: "Premium 40m Radio", "40m Radio", "Cheap 40m Radio". */
	private static void addCrystalRadio(LanguageProvider provider, CrystalVariants<Item, ? extends Item> radio, String name) {
		for(CrystalQuality quality : CrystalQuality.values())
			provider.addItem(radio.get(quality), quality.radioPrefix() + name);
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

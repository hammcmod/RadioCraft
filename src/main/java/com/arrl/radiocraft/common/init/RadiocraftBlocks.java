package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.*;
import com.arrl.radiocraft.common.blocks.antennas.*;
import com.arrl.radiocraft.common.blocks.power.ChargeControllerBlock;
import com.arrl.radiocraft.common.blocks.power.LargeBatteryBlock;
import com.arrl.radiocraft.common.blocks.power.SolarPanelBlock;
import com.arrl.radiocraft.common.blocks.radios.*;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry class for Radiocraft's blocks
 */
public class RadiocraftBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(Radiocraft.MOD_ID);

	// Copy common properties here to avoid using copy method a ton.
	private static final Properties PROPERTIES_STONE = Properties.ofFullCopy(Blocks.STONE);
	private static final Properties PROPERTIES_STONE_NO_OCCLUDE = Properties.ofFullCopy(Blocks.STONE).noCollission();
	private static final Properties PROPERTIES_RADIO = Properties.ofFullCopy(Blocks.STONE).noOcclusion();
	private static final Properties PROPERTIES_IRON_BARS = Properties.ofFullCopy(Blocks.IRON_BARS);
	private static final Properties PROPERTIES_FOOTHOLD = Properties.ofFullCopy(Blocks.IRON_BARS).strength(3.0F, 6.0F);
	private static final Properties PROPERTIES_WIRES = Properties.of().sound(SoundType.STONE).instabreak().noCollission().noOcclusion().mapColor(MapColor.COLOR_LIGHT_GRAY); // No tool needed to pick wire back up.

	// Power related blocks
	public static final DeferredHolder<Block, WireBlock> WIRE = BLOCKS.register("wire", () -> new WireBlock(PROPERTIES_WIRES, true, false));
	public static final Supplier<Block> WATERPROOF_WIRE = BLOCKS.register("waterproof_wire", () -> new WireBlock(PROPERTIES_WIRES, true, true));
	/** Links radios, accessories, wired speakers and microwave stations that do not touch (see {@code DataWire}). */
	public static final DeferredHolder<Block, WireBlock> DATA_WIRE = BLOCKS.register("data_wire", () -> WireBlock.data(PROPERTIES_WIRES));
	/** Dyed data wire, one block per colour: it joins only its own colour, so parallel runs stay apart (as dyed coax). */
	public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> COLOURED_DATA_WIRE = colouredDataWire();

	private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> colouredDataWire() {
		java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> out = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);
		for(net.minecraft.world.item.DyeColor colour : net.minecraft.world.item.DyeColor.values())
			out.put(colour, BLOCKS.register("data_wire_" + colour.getSerializedName(), () -> WireBlock.data(PROPERTIES_WIRES, colour)));
		return out;
	}
	/** Dyed power wire, one block per colour: it joins only its own colour and plain wire, so parallel runs stay apart (as dyed coax). */
	public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> COLOURED_WIRE = colouredWire();

	private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> colouredWire() {
		java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> out = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);
		for(net.minecraft.world.item.DyeColor colour : net.minecraft.world.item.DyeColor.values())
			out.put(colour, BLOCKS.register("wire_" + colour.getSerializedName(), () -> new WireBlock(PROPERTIES_WIRES, true, false, colour)));
		return out;
	}
	public static final DeferredHolder<Block, SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new SolarPanelBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, LargeBatteryBlock> LARGE_BATTERY = BLOCKS.register("large_battery", () -> new LargeBatteryBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, ChargeControllerBlock> CHARGE_CONTROLLER = BLOCKS.register("charge_controller", () -> new ChargeControllerBlock(PROPERTIES_STONE));

	// Radios/receivers/repeaters
	public static final CrystalVariants<Block, VHFBaseStationBlock> VHF_BASE_STATION_VARIANTS = CrystalVariants.register(BLOCKS, "vhf_base_station", crystal -> new VHFBaseStationBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, VHFBaseStationBlock> VHF_BASE_STATION = VHF_BASE_STATION_VARIANTS.good();
	public static final DeferredHolder<Block, VHFReceiverBlock> VHF_RECEIVER = BLOCKS.register("vhf_receiver", () -> new VHFReceiverBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, VHFRepeaterBlock> VHF_REPEATER = BLOCKS.register("vhf_repeater", () -> new VHFRepeaterBlock(PROPERTIES_RADIO));
	/** Plays tapes out loud and into the radios it touches, and records on blank ones. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.TapePlayerBlock> TAPE_PLAYER = BLOCKS.register("tape_player",
			() -> new com.arrl.radiocraft.common.blocks.TapePlayerBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion()));
	/** Not craftable: found in structures (numbers stations, beacons, dead repeaters). Breaks into a rare part. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.CommercialRadioModuleBlock> COMMERCIAL_RADIO_MODULE = BLOCKS.register("commercial_radio_module",
			() -> new com.arrl.radiocraft.common.blocks.CommercialRadioModuleBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).noOcclusion()));
	/** Not craftable: found at repeater sites. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.radios.CommercialRepeaterBlock> COMMERCIAL_REPEATER = BLOCKS.register("commercial_repeater",
			() -> new com.arrl.radiocraft.common.blocks.radios.CommercialRepeaterBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F, 8.0F).noOcclusion()));

	public static final CrystalVariants<Block, HFRadio10mBlock> HF_RADIO_10M_VARIANTS = CrystalVariants.register(BLOCKS, "hf_radio_10m", crystal -> new HFRadio10mBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, HFRadio10mBlock> HF_RADIO_10M = HF_RADIO_10M_VARIANTS.good();
	public static final CrystalVariants<Block, HFRadio20mBlock> HF_RADIO_20M_VARIANTS = CrystalVariants.register(BLOCKS, "hf_radio_20m", crystal -> new HFRadio20mBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, HFRadio20mBlock> HF_RADIO_20M = HF_RADIO_20M_VARIANTS.good();
	public static final CrystalVariants<Block, HFRadio40mBlock> HF_RADIO_40M_VARIANTS = CrystalVariants.register(BLOCKS, "hf_radio_40m", crystal -> new HFRadio40mBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, HFRadio40mBlock> HF_RADIO_40M = HF_RADIO_40M_VARIANTS.good();
	public static final CrystalVariants<Block, HFRadio80mBlock> HF_RADIO_80M_VARIANTS = CrystalVariants.register(BLOCKS, "hf_radio_80m", crystal -> new HFRadio80mBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, HFRadio80mBlock> HF_RADIO_80M = HF_RADIO_80M_VARIANTS.good();
	public static final DeferredHolder<Block, HFReceiverBlock> HF_RECEIVER = BLOCKS.register("hf_receiver", () -> new HFReceiverBlock(PROPERTIES_RADIO));

	public static final DeferredHolder<Block, HFRadioAllBandBlock> ALL_BAND_RADIO = BLOCKS.register("all_band_radio", () -> new HFRadioAllBandBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, QRPRadio20mBlock> QRP_RADIO_20M = BLOCKS.register("qrp_radio_20m", () -> new QRPRadio20mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, QRPRadio40mBlock> QRP_RADIO_40M = BLOCKS.register("qrp_radio_40m", () -> new QRPRadio40mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, RadioInterfaceBlock> RADIO_INTERFACE = BLOCKS.register("radio_interface", () -> new RadioInterfaceBlock(PROPERTIES_STONE));
	public static final DeferredHolder<Block, AprsTerminalBlock> APRS_TERMINAL = BLOCKS.register("aprs_h19", () -> new AprsTerminalBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, ParrotControllerBlock> PARROT_CONTROLLER = BLOCKS.register("parrot_controller", () -> new ParrotControllerBlock(PROPERTIES_RADIO));

	// Antenna blocks
	/** Joins power wire of any colour (colours otherwise keep apart). */
	public static final DeferredHolder<Block, WireJunctionBlock> WIRE_JUNCTION = BLOCKS.register("wire_junction",
			() -> new WireJunctionBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()));
	public static final DeferredHolder<Block, DuplexerBlock> DUPLEXER = BLOCKS.register("duplexer", () -> new DuplexerBlock(PROPERTIES_STONE_NO_OCCLUDE));
	/** One radio to one of three antennas, chosen by a knob. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.antennas.AntennaSwitchBlock> ANTENNA_SWITCH = BLOCKS.register("antenna_switch",
			() -> new com.arrl.radiocraft.common.blocks.antennas.AntennaSwitchBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()));
	/** One antenna to several receivers, receive only. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.antennas.AntennaSplitterBlock> ANTENNA_SPLITTER = BLOCKS.register("antenna_splitter",
			() -> new com.arrl.radiocraft.common.blocks.antennas.AntennaSplitterBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()));
	public static final DeferredHolder<Block, AntennaTunerBlock> ANTENNA_TUNER = BLOCKS.register("antenna_tuner", () -> new AntennaTunerBlock(PROPERTIES_STONE));
	public static final DeferredHolder<Block, AutoTunerBlock> AUTO_TUNER = BLOCKS.register("auto_tuner", () -> new AutoTunerBlock(PROPERTIES_STONE_NO_OCCLUDE));

	public static final DeferredHolder<Block, WireBlock> COAX_WIRE = BLOCKS.register("coax_wire", () -> new WireBlock(PROPERTIES_WIRES, false, false));
	/** Dyed coax, one block per colour: it joins only its own colour and plain coax, so parallel runs stay apart. */
	public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> COLOURED_COAX = colouredCoax();

	private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> colouredCoax() {
		java.util.Map<net.minecraft.world.item.DyeColor, DeferredHolder<Block, WireBlock>> out = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);
		for(net.minecraft.world.item.DyeColor colour : net.minecraft.world.item.DyeColor.values())
			out.put(colour, BLOCKS.register("coax_wire_" + colour.getSerializedName(), () -> new WireBlock(PROPERTIES_WIRES, false, false, colour)));
		return out;
	}
	/** A wall block wire runs through; made with the passthrough item, looks like the block it replaced. */
	public static final DeferredHolder<Block, PassthroughBlock> PASSTHROUGH = BLOCKS.register("passthrough", () -> new PassthroughBlock(Properties.ofFullCopy(Blocks.STONE)));
	/** Several wires in one block (see WireBundleBlock); no item, made by using a wire item on a wire of another kind. */
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.WireBundleBlock> WIRE_BUNDLE = BLOCKS.register("wire_bundle", () -> new com.arrl.radiocraft.common.blocks.WireBundleBlock(PROPERTIES_WIRES));
	/** Rungs on a tower: the old ones (structures, creative) may give way under a player; crafted ones never do. */
	public static final DeferredHolder<Block, TowerFootholdBlock> TOWER_FOOTHOLD = BLOCKS.register("tower_foothold", () -> new TowerFootholdBlock(PROPERTIES_FOOTHOLD, false));
	public static final DeferredHolder<Block, TowerFootholdBlock> OLD_TOWER_FOOTHOLD = BLOCKS.register("old_tower_foothold", () -> new TowerFootholdBlock(PROPERTIES_FOOTHOLD, true));
	public static final DeferredHolder<Block, AntennaPoleBlock> ANTENNA_POLE = BLOCKS.register("antenna_pole", () -> new AntennaPoleBlock(PROPERTIES_IRON_BARS));
	public static final DeferredHolder<Block, AntennaConnectorBlock> ANTENNA_CONNECTOR = BLOCKS.register("antenna_connector", () -> new AntennaConnectorBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, BalunBlock> BALUN_ONE_TO_ONE = BLOCKS.register("balun_one_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE, "balun_one_to_one"));
	public static final DeferredHolder<Block, BalunBlock> BALUN_FOUR_TO_ONE = BLOCKS.register("balun_four_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE, "balun_four_to_one"));
	public static final DeferredHolder<Block, BalunBlock> BALUN_TWO_TO_ONE = BLOCKS.register("balun_two_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE, "balun_two_to_one"));
	public static final DeferredHolder<Block, BalunBlock> BALUN_NINE_TO_ONE = BLOCKS.register("balun_nine_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE, "balun_nine_to_one"));
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.antennas.DummyLoadBlock> DUMMY_LOAD = BLOCKS.register("dummy_load", () -> new com.arrl.radiocraft.common.blocks.antennas.DummyLoadBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, JPoleAntennaBlock> J_POLE_ANTENNA = BLOCKS.register("j_pole_antenna", () -> new JPoleAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.antennas.RingoAntennaBlock> RINGO_ANTENNA = BLOCKS.register("ringo_antenna", () -> new com.arrl.radiocraft.common.blocks.antennas.RingoAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.antennas.FoldedDipoleAntennaBlock> FOLDED_DIPOLE_ANTENNA = BLOCKS.register("folded_dipole_antenna", () -> new com.arrl.radiocraft.common.blocks.antennas.FoldedDipoleAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, IonosphericProcessorBlock> IONOSPHERIC_PROCESSOR = BLOCKS.register("ionospheric_processor", () -> new IonosphericProcessorBlock(Properties.ofFullCopy(Blocks.STONE).noOcclusion()
			.lightLevel(state -> state.getValue(IonosphericProcessorBlock.HALF) == DoubleBlockHalf.UPPER ? IonosphericProcessorBlock.VALVE_LIGHT : 0)));
	public static final DeferredHolder<Block, FiveBtvAntennaBlock> FIVE_BTV_ANTENNA = BLOCKS.register("5btv_antenna", () -> new FiveBtvAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, IonosondeBlock> IONOSONDE = BLOCKS.register("ionosonde", () -> new IonosondeBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, SlimJimAntennaBlock> SLIM_JIM_ANTENNA = BLOCKS.register("slim_jim_antenna", () -> new SlimJimAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, YagiAntennaBlock> YAGI_ANTENNA = BLOCKS.register("yagi_antenna", () -> new YagiAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, SatelliteDishBlock> SATELLITE_DISH = BLOCKS.register("satellite_dish", () -> new SatelliteDishBlock(PROPERTIES_STONE_NO_OCCLUDE));

	public static final DeferredHolder<Block, TeletypeBlock> TELETYPE = BLOCKS.register("teletype", () -> new TeletypeBlock(PROPERTIES_STONE));

	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.WefaxReceiverBlock> WEFAX_RECEIVER = BLOCKS.register("wefax_receiver", () -> new com.arrl.radiocraft.common.blocks.WefaxReceiverBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, MicrophoneBlock> MICROPHONE = BLOCKS.register("microphone", () -> new MicrophoneBlock(PROPERTIES_STONE));
	public static final DeferredHolder<Block, DeskChargerBlock> DESK_CHARGER = BLOCKS.register("desk_charger", () -> new DeskChargerBlock(Properties.of().mapColor(DyeColor.GRAY).strength(0.0F, 5.0F).sound(SoundType.METAL).noOcclusion()));

	// Tower legs: lattice beams that join every beam touching them (see TowerBeamBlock).
	public static final DeferredHolder<Block, TowerBeamBlock> TOWER_BEAM_THIN = BLOCKS.register("tower_beam_thin", () -> new TowerBeamBlock(PROPERTIES_IRON_BARS, 4));
	/** A thin beam that flashes a tower's obstruction lights: redstone 2 s on, 2 s off. Creative and structures only. */
	/** A tower's red obstruction light: lit by redstone, light 11. */
	public static final DeferredHolder<Block, TowerBeaconBlock> TOWER_BEACON = BLOCKS.register("tower_beacon",
			() -> new TowerBeaconBlock(Properties.ofFullCopy(Blocks.REDSTONE_LAMP).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
	public static final DeferredHolder<Block, TowerBeaconControllerBlock> TOWER_BEACON_CONTROLLER = BLOCKS.register("tower_beacon_controller", () -> new TowerBeaconControllerBlock(PROPERTIES_IRON_BARS));
	public static final DeferredHolder<Block, TowerBeamBlock> TOWER_BEAM = BLOCKS.register("tower_beam", () -> new TowerBeamBlock(PROPERTIES_IRON_BARS, 6));
	public static final DeferredHolder<Block, TowerBeamBlock> TOWER_BEAM_THICK = BLOCKS.register("tower_beam_thick", () -> new TowerBeamBlock(PROPERTIES_IRON_BARS, 8));
	/** The AT&T Long Lines horn-reflector, two by two blocks and four tall. Decorative for now. */
	public static final DeferredHolder<Block, MicrowaveHornBlock> MICROWAVE_HORN = BLOCKS.register("microwave_horn", () -> new MicrowaveHornBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).noOcclusion()));

	// Speakers (see SpeakerKind): ones that sit next to a radio, and wired ones on its power wire.
	public static final String[] SPEAKER_WOODS = { "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped" };
	private static final Properties PROPERTIES_SPEAKER_METAL = Properties.of().mapColor(MapColor.METAL).strength(1.5F, 3.0F).sound(SoundType.METAL).noOcclusion();
	public static final java.util.Map<String, DeferredHolder<Block, SpeakerBlock>> DESK_SPEAKERS = deskSpeakers();
	/** The radio fax, one per log, the same woods as the desk speakers (after SPEAKER_WOODS: it reads it). */
	public static final java.util.Map<String, DeferredHolder<Block, RadioFaxBlock>> RADIO_FAXES = radioFaxes();
	public static final DeferredHolder<Block, SpeakerBlock> LARGE_SPEAKER = BLOCKS.register("large_speaker", () -> new SpeakerBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion(),
			com.arrl.radiocraft.common.radio.voice.SpeakerKind.STANDARD, 1.0D, 0.0D, 0.25D, 15.0D, 16.0D, 15.0D));
	public static final DeferredHolder<Block, SpeakerBlock> PREMIUM_SPEAKER_MARBLE = premiumSpeaker("marble");
	public static final DeferredHolder<Block, SpeakerBlock> PREMIUM_SPEAKER_IRON = premiumSpeaker("iron");
	public static final DeferredHolder<Block, SpeakerBlock> PREMIUM_SPEAKER_GOLD = premiumSpeaker("gold");
	public static final DeferredHolder<Block, WiredSpeakerBlock> WALL_SPEAKER = BLOCKS.register("wall_speaker", () -> new WiredSpeakerBlock(PROPERTIES_SPEAKER_METAL,
			com.arrl.radiocraft.common.radio.voice.SpeakerKind.WALL, 0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D));
	public static final DeferredHolder<Block, WiredSpeakerBlock> LOUDSPEAKER = BLOCKS.register("loudspeaker", () -> new WiredSpeakerBlock(PROPERTIES_SPEAKER_METAL,
			com.arrl.radiocraft.common.radio.voice.SpeakerKind.LOUDSPEAKER, 1.0D, 0.0D, 0.0D, 15.0D, 16.0D, 15.0D));

	// Direction finding: the terminal (plain, or with the world map module), and the antenna modules stacked on it.
	public static final DeferredHolder<Block, DirectionFinderBlock> DIRECTION_FINDER = BLOCKS.register("direction_finder", () -> new DirectionFinderBlock(PROPERTIES_RADIO, false));
	public static final DeferredHolder<Block, DirectionFinderBlock> MAP_DIRECTION_FINDER = BLOCKS.register("map_direction_finder", () -> new DirectionFinderBlock(PROPERTIES_RADIO, true));
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.PabxBlock> PABX = BLOCKS.register("pabx", () -> new com.arrl.radiocraft.common.blocks.PabxBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, DirectionFinderModuleBlock> DIRECTION_FINDER_MODULE = BLOCKS.register("direction_finder_module", () -> new DirectionFinderModuleBlock(PROPERTIES_RADIO));

	// Microwave links and what rides them.
	public static final DeferredHolder<Block, MicrowaveRadioBlock> MICROWAVE_RADIO = BLOCKS.register("microwave_radio", () -> new MicrowaveRadioBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, TelephoneBlock> TELEPHONE = BLOCKS.register("telephone", () -> new TelephoneBlock(Properties.ofFullCopy(Blocks.STONE).strength(1.0F).noOcclusion()));

	// Weather: a mast-top station whose readings H19s nearby beacon.
	public static final DeferredHolder<Block, com.arrl.radiocraft.common.blocks.WeatherStationBlock> WEATHER_STATION = BLOCKS.register("weather_station",
			() -> new com.arrl.radiocraft.common.blocks.WeatherStationBlock(PROPERTIES_SPEAKER_METAL));

	private static java.util.Map<String, DeferredHolder<Block, RadioFaxBlock>> radioFaxes() {
		java.util.Map<String, DeferredHolder<Block, RadioFaxBlock>> out = new java.util.LinkedHashMap<>();
		for(String wood : SPEAKER_WOODS)
			out.put(wood, BLOCKS.register("radio_fax_" + wood, () -> new RadioFaxBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion())));
		return out;
	}

	private static java.util.Map<String, DeferredHolder<Block, SpeakerBlock>> deskSpeakers() {
		java.util.Map<String, DeferredHolder<Block, SpeakerBlock>> out = new java.util.LinkedHashMap<>();
		for(String wood : SPEAKER_WOODS)
			out.put(wood, BLOCKS.register("desk_speaker_" + wood, () -> new SpeakerBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.0F).noOcclusion(),
					com.arrl.radiocraft.common.radio.voice.SpeakerKind.STANDARD, 5.0D, 0.0D, 5.5D, 11.0D, 5.0D, 10.5D)));
		return out;
	}

	private static DeferredHolder<Block, SpeakerBlock> premiumSpeaker(String colour) {
		return BLOCKS.register("premium_speaker_" + colour, () -> new SpeakerBlock(PROPERTIES_SPEAKER_METAL,
				com.arrl.radiocraft.common.radio.voice.SpeakerKind.PREMIUM, 4.0D, 0.0D, 4.5D, 12.0D, 6.5D, 11.0D));
	}

	public static Supplier<Block> simpleBlock(String name, Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}

}

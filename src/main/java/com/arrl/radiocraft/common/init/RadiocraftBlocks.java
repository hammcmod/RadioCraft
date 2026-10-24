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
	public static final DeferredHolder<Block, SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new SolarPanelBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, LargeBatteryBlock> LARGE_BATTERY = BLOCKS.register("large_battery", () -> new LargeBatteryBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, ChargeControllerBlock> CHARGE_CONTROLLER = BLOCKS.register("charge_controller", () -> new ChargeControllerBlock(PROPERTIES_STONE));

	// Radios/receivers/repeaters
	public static final CrystalVariants<Block, VHFBaseStationBlock> VHF_BASE_STATION_VARIANTS = CrystalVariants.register(BLOCKS, "vhf_base_station", crystal -> new VHFBaseStationBlock(PROPERTIES_RADIO, crystal));
	public static final DeferredHolder<Block, VHFBaseStationBlock> VHF_BASE_STATION = VHF_BASE_STATION_VARIANTS.good();
	public static final DeferredHolder<Block, VHFReceiverBlock> VHF_RECEIVER = BLOCKS.register("vhf_receiver", () -> new VHFReceiverBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, VHFRepeaterBlock> VHF_REPEATER = BLOCKS.register("vhf_repeater", () -> new VHFRepeaterBlock(PROPERTIES_RADIO));

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
	public static final DeferredHolder<Block, TransferInterfaceBlock> TRANSFER_INTERFACE = BLOCKS.register("transfer_interface", () -> new TransferInterfaceBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, ParrotControllerBlock> PARROT_CONTROLLER = BLOCKS.register("parrot_controller", () -> new ParrotControllerBlock(PROPERTIES_RADIO));

	// Antenna blocks
	public static final DeferredHolder<Block, DuplexerBlock> DUPLEXER = BLOCKS.register("duplexer", () -> new DuplexerBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, AntennaTunerBlock> ANTENNA_TUNER = BLOCKS.register("antenna_tuner", () -> new AntennaTunerBlock(PROPERTIES_STONE));
	public static final DeferredHolder<Block, AutoTunerBlock> AUTO_TUNER = BLOCKS.register("auto_tuner", () -> new AutoTunerBlock(PROPERTIES_STONE_NO_OCCLUDE));

	public static final DeferredHolder<Block, WireBlock> COAX_WIRE = BLOCKS.register("coax_wire", () -> new WireBlock(PROPERTIES_WIRES, false, false));
	/** A wall block wire runs through; made with the passthrough item, looks like the block it replaced. */
	public static final DeferredHolder<Block, PassthroughBlock> PASSTHROUGH = BLOCKS.register("passthrough", () -> new PassthroughBlock(Properties.ofFullCopy(Blocks.STONE)));
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

	public static Supplier<Block> simpleBlock(String name, Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}

}

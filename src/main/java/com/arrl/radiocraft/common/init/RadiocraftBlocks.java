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
	private static final Properties PROPERTIES_WIRES = Properties.ofFullCopy(Blocks.STONE).instabreak().noCollission().noOcclusion().mapColor(MapColor.COLOR_LIGHT_GRAY);

	// Power related blocks
	public static final DeferredHolder<Block, WireBlock> WIRE = BLOCKS.register("wire", () -> new WireBlock(PROPERTIES_WIRES, true, false));
	public static final Supplier<Block> WATERPROOF_WIRE = BLOCKS.register("waterproof_wire", () -> new WireBlock(PROPERTIES_WIRES, true, true));
	public static final DeferredHolder<Block, SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new SolarPanelBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, LargeBatteryBlock> LARGE_BATTERY = BLOCKS.register("large_battery", () -> new LargeBatteryBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, ChargeControllerBlock> CHARGE_CONTROLLER = BLOCKS.register("charge_controller", () -> new ChargeControllerBlock(PROPERTIES_STONE));

	// Radios/receivers/repeaters
	public static final DeferredHolder<Block, VHFBaseStationBlock> VHF_BASE_STATION = BLOCKS.register("vhf_base_station", () -> new VHFBaseStationBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, VHFReceiverBlock> VHF_RECEIVER = BLOCKS.register("vhf_receiver", () -> new VHFReceiverBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, VHFRepeaterBlock> VHF_REPEATER = BLOCKS.register("vhf_repeater", () -> new VHFRepeaterBlock(PROPERTIES_RADIO));

	public static final DeferredHolder<Block, HFRadio10mBlock> HF_RADIO_10M = BLOCKS.register("hf_radio_10m", () -> new HFRadio10mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, HFRadio20mBlock> HF_RADIO_20M = BLOCKS.register("hf_radio_20m", () -> new HFRadio20mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, HFRadio40mBlock> HF_RADIO_40M = BLOCKS.register("hf_radio_40m", () -> new HFRadio40mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, HFRadio80mBlock> HF_RADIO_80M = BLOCKS.register("hf_radio_80m", () -> new HFRadio80mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, HFReceiverBlock> HF_RECEIVER = BLOCKS.register("hf_receiver", () -> new HFReceiverBlock(PROPERTIES_RADIO));

	public static final DeferredHolder<Block, HFRadioAllBandBlock> ALL_BAND_RADIO = BLOCKS.register("all_band_radio", () -> new HFRadioAllBandBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, QRPRadio20mBlock> QRP_RADIO_20M = BLOCKS.register("qrp_radio_20m", () -> new QRPRadio20mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, QRPRadio40mBlock> QRP_RADIO_40M = BLOCKS.register("qrp_radio_40m", () -> new QRPRadio40mBlock(PROPERTIES_RADIO));
	public static final DeferredHolder<Block, DigitalInterfaceBlock> DIGITAL_INTERFACE = BLOCKS.register("digital_interface", () -> new DigitalInterfaceBlock(PROPERTIES_STONE));

	// Antenna blocks
	public static final DeferredHolder<Block, DuplexerBlock> DUPLEXER = BLOCKS.register("duplexer", () -> new DuplexerBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, AntennaTunerBlock> ANTENNA_TUNER = BLOCKS.register("antenna_tuner", () -> new AntennaTunerBlock(PROPERTIES_STONE));

	public static final DeferredHolder<Block, WireBlock> COAX_WIRE = BLOCKS.register("coax_wire", () -> new WireBlock(PROPERTIES_WIRES, false, false));
	public static final DeferredHolder<Block, AntennaPoleBlock> ANTENNA_POLE = BLOCKS.register("antenna_pole", () -> new AntennaPoleBlock(PROPERTIES_IRON_BARS));
	public static final DeferredHolder<Block, AntennaConnectorBlock> ANTENNA_CONNECTOR = BLOCKS.register("antenna_connector", () -> new AntennaConnectorBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, BalunBlock> BALUN_ONE_TO_ONE = BLOCKS.register("balun_one_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, BalunBlock> BALUN_TWO_TO_ONE = BLOCKS.register("balun_two_to_one", () -> new BalunBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, JPoleAntennaBlock> J_POLE_ANTENNA = BLOCKS.register("j_pole_antenna", () -> new JPoleAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, SlimJimAntennaBlock> SLIM_JIM_ANTENNA = BLOCKS.register("slim_jim_antenna", () -> new SlimJimAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));
	public static final DeferredHolder<Block, YagiAntennaBlock> YAGI_ANTENNA = BLOCKS.register("yagi_antenna", () -> new YagiAntennaBlock(PROPERTIES_STONE_NO_OCCLUDE));

	public static final DeferredHolder<Block, SolarWeatherStationBlock> SOLAR_WEATHER_STATION = BLOCKS.register("solar_weather_station", () -> new SolarWeatherStationBlock(PROPERTIES_STONE));

	public static final DeferredHolder<Block, MicrophoneBlock> MICROPHONE = BLOCKS.register("microphone", () -> new MicrophoneBlock(PROPERTIES_STONE));
	public static final DeferredHolder<Block, DeskChargerBlock> DESK_CHARGER = BLOCKS.register("desk_charger", () -> new DeskChargerBlock(Properties.of().mapColor(DyeColor.GRAY).strength(0.0F, 5.0F).sound(SoundType.METAL).noOcclusion()));

	public static Supplier<Block> simpleBlock(String name, Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}

}

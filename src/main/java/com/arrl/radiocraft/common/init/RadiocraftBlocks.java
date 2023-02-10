package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.LargeBatteryBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry class for Radiocraft's blocks
 */
public class RadiocraftBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Radiocraft.MOD_ID);

	// Copy common properties here to avoid using copy method a ton.
	private static final Properties PROPERTIES_STONE = Properties.copy(Blocks.STONE);
	private static final Properties PROPERTIES_IRON_BARS = Properties.copy(Blocks.IRON_BARS);
	private static final Properties PROPERTIES_WOOL = Properties.copy(Blocks.WHITE_WOOL);

	// Power related blocks
	public static final RegistryObject<Block> WIRE = simpleBlock("wire", PROPERTIES_WOOL);
	public static final RegistryObject<Block> WATERPROOF_WIRE = simpleBlock("waterproof_wire", PROPERTIES_WOOL);
	public static final RegistryObject<Block> SOLAR_PANEL = simpleBlock("solar_panel", Properties.copy(Blocks.DAYLIGHT_DETECTOR));
	public static final RegistryObject<Block> LARGE_BATTERY = BLOCKS.register("large_battery", () -> new LargeBatteryBlock(PROPERTIES_STONE));

	// Radios/receivers/repeaters
	public static final RegistryObject<Block> VHF_BASE_STATION = simpleBlock("vhf_base_station", PROPERTIES_STONE);
	public static final RegistryObject<Block> VHF_RECEIVER = simpleBlock("vhf_receiver", PROPERTIES_STONE);
	public static final RegistryObject<Block> VHF_REPEATER = simpleBlock("vhf_repeater", PROPERTIES_STONE);

	public static final RegistryObject<Block> HF_RADIO_10M = simpleBlock("hf_radio_10m", PROPERTIES_STONE);
	public static final RegistryObject<Block> HF_RADIO_20M = simpleBlock("hf_radio_20m", PROPERTIES_STONE);
	public static final RegistryObject<Block> HF_RADIO_40M = simpleBlock("hf_radio_40m", PROPERTIES_STONE);
	public static final RegistryObject<Block> HF_RADIO_80M = simpleBlock("hf_radio_80m", PROPERTIES_STONE);
	public static final RegistryObject<Block> HF_RECEIVER = simpleBlock("hf_receiver", PROPERTIES_STONE);

	public static final RegistryObject<Block> ALL_BAND_RADIO = simpleBlock("all_band_radio", PROPERTIES_STONE);
	public static final RegistryObject<Block> QRP_RADIO_20M = simpleBlock("qrp_radio_20m", PROPERTIES_STONE);
	public static final RegistryObject<Block> QRP_RADIO_40M = simpleBlock("qrp_radio_40m", PROPERTIES_STONE);

	public static final RegistryObject<Block> DIGITAL_INTERFACE = simpleBlock("digital_interface", PROPERTIES_STONE);

	// Antenna blocks
	public static final RegistryObject<Block> ANTENNA_POLE = simpleBlock("antenna_pole", PROPERTIES_IRON_BARS);
	public static final RegistryObject<Block> DUPLEXER = simpleBlock("duplexer", PROPERTIES_IRON_BARS);
	public static final RegistryObject<Block> ANTENNA_TUNER = simpleBlock("antenna_tuner", PROPERTIES_STONE);
	public static final RegistryObject<Block> ANTENNA_WIRE = simpleBlock("antenna_wire", PROPERTIES_IRON_BARS);
	public static final RegistryObject<Block> ANTENNA_CONNECTOR = simpleBlock("antenna_connector", PROPERTIES_IRON_BARS);
	public static final RegistryObject<Block> COAX_WIRE = simpleBlock("coax_wire", PROPERTIES_WOOL);

	public static final RegistryObject<Block> SOLAR_WEATHER_STATION = simpleBlock("solar_weather_station", PROPERTIES_STONE);


	public static RegistryObject<Block> simpleBlock(String name, Properties properties) {
		return BLOCKS.register(name, () -> new Block(properties));
	}

}

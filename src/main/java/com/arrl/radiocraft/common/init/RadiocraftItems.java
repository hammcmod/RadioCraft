package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.datacomponents.HandheldRadioState;
import com.arrl.radiocraft.common.items.AntennaPoleItem;
import com.arrl.radiocraft.common.items.AntennaWireItem;
import com.arrl.radiocraft.common.items.SmallBatteryItem;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry class for Radiocraft's items
 */
public class RadiocraftItems {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(Radiocraft.MOD_ID);

	// Regular Items
	public static final Supplier<Item> RADIO_CRYSTAL = simpleItem("radio_crystal");
	public static final Supplier<Item> RADIO_SPEAKER = simpleItem("radio_speaker");
	public static final Supplier<Item> HAND_MICROPHONE = simpleItem("hand_microphone");
	public static final Supplier<Item> HF_CIRCUIT_BOARD = simpleItem("hf_circuit_board");

	public static final DeferredHolder<Item, SmallBatteryItem> SMALL_BATTERY = ITEMS.register("small_battery", () -> new SmallBatteryItem(new Properties().stacksTo(1))); //.component(DataComponent.ENERGY_DATA_COMPONENT, new DataComponent.EnergyRecord(1000.0))));
	public static final Supplier<Item> FERRITE_CORE = simpleItem("ferrite_core");
	public static final Supplier<Item> COAXIAL_CORE = simpleItem("coaxial_core");
	public static final Supplier<Item> ANTENNA_ANALYZER = simpleItem("antenna_analyzer");
	public static final DeferredHolder<Item, VHFHandheldItem> VHF_HANDHELD = ITEMS.register("vhf_handheld", () -> new VHFHandheldItem(new Properties().stacksTo(1).component(RadiocraftDataComponent.HANDHELD_RADIO_STATE_COMPONENT.value(), HandheldRadioState.getDefault())));
	public static final DeferredHolder<Item, AntennaWireItem> ANTENNA_WIRE = ITEMS.register("antenna_wire", () -> new AntennaWireItem(new Properties()));

	// Block Items
	public static final Supplier<BlockItem> WIRE = simpleBlockItem("wire", RadiocraftBlocks.WIRE);
	public static final Supplier<BlockItem> WATERPROOF_WIRE = simpleBlockItem("waterproof_wire", RadiocraftBlocks.WATERPROOF_WIRE);
	public static final Supplier<BlockItem> SOLAR_PANEL = simpleBlockItem("solar_panel", RadiocraftBlocks.SOLAR_PANEL);
	public static final Supplier<BlockItem> LARGE_BATTERY = simpleBlockItem("large_battery", RadiocraftBlocks.LARGE_BATTERY);
	public static final Supplier<BlockItem> CHARGE_CONTROLLER = simpleBlockItem("charge_controller", RadiocraftBlocks.CHARGE_CONTROLLER);

	public static final Supplier<BlockItem> VHF_BASE_STATION = simpleBlockItem("vhf_base_station", RadiocraftBlocks.VHF_BASE_STATION);
	public static final Supplier<BlockItem> VHF_RECEIVER = simpleBlockItem("vhf_receiver", RadiocraftBlocks.VHF_RECEIVER);
	public static final Supplier<BlockItem> VHF_REPEATER = simpleBlockItem("vhf_repeater", RadiocraftBlocks.VHF_REPEATER);

	public static final Supplier<BlockItem> HF_RADIO_10M = simpleBlockItem("hf_radio_10m", RadiocraftBlocks.HF_RADIO_10M);
	public static final Supplier<BlockItem> HF_RADIO_20M = simpleBlockItem("hf_radio_20m", RadiocraftBlocks.HF_RADIO_20M);
	public static final Supplier<BlockItem> HF_RADIO_40M = simpleBlockItem("hf_radio_40m", RadiocraftBlocks.HF_RADIO_40M);
	public static final Supplier<BlockItem> HF_RADIO_80M = simpleBlockItem("hf_radio_80m", RadiocraftBlocks.HF_RADIO_80M);
	public static final Supplier<BlockItem> HF_RECEIVER = simpleBlockItem("hf_receiver", RadiocraftBlocks.HF_RECEIVER);

	public static final Supplier<BlockItem> ALL_BAND_RADIO = simpleBlockItem("all_band_radio", RadiocraftBlocks.ALL_BAND_RADIO);
	public static final Supplier<BlockItem> QRP_RADIO_20M = simpleBlockItem("qrp_radio_20m", RadiocraftBlocks.QRP_RADIO_20M);
	public static final Supplier<BlockItem> QRP_RADIO_40M = simpleBlockItem("qrp_radio_40m", RadiocraftBlocks.QRP_RADIO_40M);

	public static final Supplier<BlockItem> DIGITAL_INTERFACE = simpleBlockItem("digital_interface", RadiocraftBlocks.DIGITAL_INTERFACE);

	// TODO: This may be supposed to be a BlockItem, but this compiles so meh?
	public static final DeferredHolder<Item, AntennaPoleItem> ANTENNA_POLE = ITEMS.register("antenna_pole", () -> new AntennaPoleItem(RadiocraftBlocks.ANTENNA_POLE.get(), new Item.Properties()));
	public static final Supplier<BlockItem> DUPLEXER = simpleBlockItem("duplexer", RadiocraftBlocks.DUPLEXER);
	public static final Supplier<BlockItem> ANTENNA_TUNER = simpleBlockItem("antenna_tuner", RadiocraftBlocks.ANTENNA_TUNER);
	public static final Supplier<BlockItem> ANTENNA_CONNECTOR = simpleBlockItem("antenna_connector", RadiocraftBlocks.ANTENNA_CONNECTOR);
	public static final Supplier<BlockItem> BALUN_ONE_TO_ONE = simpleBlockItem("balun_one_to_one", RadiocraftBlocks.BALUN_ONE_TO_ONE);
	public static final Supplier<BlockItem> BALUN_TWO_TO_ONE = simpleBlockItem("balun_two_to_one", RadiocraftBlocks.BALUN_TWO_TO_ONE);
	public static final Supplier<BlockItem> COAX_WIRE = simpleBlockItem("coax_wire", RadiocraftBlocks.COAX_WIRE);
	public static final Supplier<BlockItem> YAGI_ANTENNA = simpleBlockItem("yagi_antenna", RadiocraftBlocks.YAGI_ANTENNA);
	public static final Supplier<BlockItem> J_POLE_ANTENNA = simpleBlockItem("j_pole_antenna", RadiocraftBlocks.J_POLE_ANTENNA);
	public static final Supplier<BlockItem> SLIM_JIM_ANTENNA = simpleBlockItem("slim_jim_antenna", RadiocraftBlocks.SLIM_JIM_ANTENNA);

	public static final Supplier<BlockItem> SOLAR_WEATHER_STATION = simpleBlockItem("solar_weather_station", RadiocraftBlocks.SOLAR_WEATHER_STATION);
	public static final Supplier<BlockItem> MICROPHONE = simpleBlockItem("microphone", RadiocraftBlocks.MICROPHONE);


	// Helper methods to cut down on boilerplate
	private static Supplier<Item> simpleItem(String name) {
		return ITEMS.register(name, () -> new Item(new Properties()));
	}

	private static Supplier<BlockItem> simpleBlockItem(String name, Supplier<? extends Block> block) {
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Properties()));
	}

}

package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registry class for Radiocraft's items
 */
public class RadiocraftItems {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Radiocraft.MOD_ID);

	// Regular Items
	public static final RegistryObject<Item> RADIO_CRYSTAL = simpleItem("radio_crystal");
	public static final RegistryObject<Item> RADIO_SPEAKER = simpleItem("radio_speaker");
	public static final RegistryObject<Item> MICROPHONE = simpleItem("microphone");
	public static final RegistryObject<Item> HAND_MICROPHONE = simpleItem("hand_microphone");
	public static final RegistryObject<Item> HF_CIRCUIT_BOARD = simpleItem("hf_circuit_board");
	public static final RegistryObject<Item> SMALL_BATTERY = simpleItem("small_battery");
	public static final RegistryObject<Item> FERRITE_CORE = simpleItem("ferrite_core");
	public static final RegistryObject<Item> COAXIAL_CORE = simpleItem("coaxial_core");
	public static final RegistryObject<Item> ANTENNA_ANALYZER = simpleItem("antenna_analyzer");
	public static final RegistryObject<Item> VHF_HANDHELD = simpleItem("vhf_handheld");

	// Block Items
	public static final RegistryObject<BlockItem> WIRE = simpleBlockItem("wire", RadiocraftBlocks.WIRE);
	public static final RegistryObject<BlockItem> WATERPROOF_WIRE = simpleBlockItem("waterproof_wire", RadiocraftBlocks.WATERPROOF_WIRE);


	// Helper methods to cut down on boilerplate
	private static RegistryObject<Item> simpleItem(String name) {
		return ITEMS.register(name, () -> new Item(new Properties()));
	}

	private static RegistryObject<BlockItem> simpleBlockItem(String name, Supplier<Block> block) {
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Properties()));
	}

}

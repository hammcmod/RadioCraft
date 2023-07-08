package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry class for radiocraft BlockEntities
 */
public class RadiocraftBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Radiocraft.MOD_ID);

	public static final RegistryObject<BlockEntityType<?>> LARGE_BATTERY = BLOCK_ENTITY_TYPES.register("large_battery",
			() -> BlockEntityType.Builder.of(LargeBatteryBlockEntity::new, RadiocraftBlocks.LARGE_BATTERY.get()).build(null));

	public static final RegistryObject<BlockEntityType<?>> SOLAR_PANEL = BLOCK_ENTITY_TYPES.register("solar_panel",
			() -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, RadiocraftBlocks.SOLAR_PANEL.get()).build(null));

	public static final RegistryObject<BlockEntityType<?>> CHARGE_CONTROLLER = BLOCK_ENTITY_TYPES.register("charge_controller",
			() -> BlockEntityType.Builder.of(ChargeControllerBlockEntity::new, RadiocraftBlocks.CHARGE_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<?>> HF_RADIO_10M = BLOCK_ENTITY_TYPES.register("hf_radio_10m",
			() -> BlockEntityType.Builder.of(HFRadio10mBlockEntity::new, RadiocraftBlocks.HF_RADIO_10M.get()).build(null));
	public static final RegistryObject<BlockEntityType<?>> HF_RADIO_20M = BLOCK_ENTITY_TYPES.register("hf_radio_20m",
			() -> BlockEntityType.Builder.of(HFRadio20mBlockEntity::new, RadiocraftBlocks.HF_RADIO_20M.get()).build(null));
	public static final RegistryObject<BlockEntityType<?>> HF_RADIO_40M = BLOCK_ENTITY_TYPES.register("hf_radio_40m",
			() -> BlockEntityType.Builder.of(HFRadio40mBlockEntity::new, RadiocraftBlocks.HF_RADIO_40M.get()).build(null));
	public static final RegistryObject<BlockEntityType<?>> HF_RADIO_80M = BLOCK_ENTITY_TYPES.register("hf_radio_80m",
			() -> BlockEntityType.Builder.of(HFRadio80mBlockEntity::new, RadiocraftBlocks.HF_RADIO_80M.get()).build(null));

	public static final RegistryObject<BlockEntityType<?>> ANTENNA = BLOCK_ENTITY_TYPES.register("antenna",
			() -> BlockEntityType.Builder.of(AntennaBlockEntity::new, RadiocraftBlocks.BALUN_ONE_TO_ONE.get()).build(null));

}

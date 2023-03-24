package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.ChargeControllerBlockEntity;
import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.blockentities.SolarPanelBlockEntity;
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

}

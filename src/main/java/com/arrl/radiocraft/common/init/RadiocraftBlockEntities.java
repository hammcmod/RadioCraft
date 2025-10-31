package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.*;
import com.arrl.radiocraft.common.blockentities.radio.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry class for radiocraft BlockEntities
 */
public class RadiocraftBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Radiocraft.MOD_ID);

	public static final Supplier<BlockEntityType<?>> LARGE_BATTERY = BLOCK_ENTITY_TYPES.register("large_battery",
			() -> BlockEntityType.Builder.of(LargeBatteryBlockEntity::new, RadiocraftBlocks.LARGE_BATTERY.get()).build(null));
	public static final Supplier<BlockEntityType<?>> SOLAR_PANEL = BLOCK_ENTITY_TYPES.register("solar_panel",
			() -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, RadiocraftBlocks.SOLAR_PANEL.get()).build(null));
	public static final Supplier<BlockEntityType<?>> CHARGE_CONTROLLER = BLOCK_ENTITY_TYPES.register("charge_controller",
			() -> BlockEntityType.Builder.of(ChargeControllerBlockEntity::new, RadiocraftBlocks.CHARGE_CONTROLLER.get()).build(null));
	public static final Supplier<BlockEntityType<HFRadio10mBlockEntity>> HF_RADIO_10M = BLOCK_ENTITY_TYPES.register("hf_radio_10m",
			() -> BlockEntityType.Builder.of(HFRadio10mBlockEntity::new, RadiocraftBlocks.HF_RADIO_10M.get()).build(null));
	public static final Supplier<BlockEntityType<HFRadio20mBlockEntity>> HF_RADIO_20M = BLOCK_ENTITY_TYPES.register("hf_radio_20m",
			() -> BlockEntityType.Builder.of(HFRadio20mBlockEntity::new, RadiocraftBlocks.HF_RADIO_20M.get()).build(null));
	public static final Supplier<BlockEntityType<HFRadio40mBlockEntity>> HF_RADIO_40M = BLOCK_ENTITY_TYPES.register("hf_radio_40m",
			() -> BlockEntityType.Builder.of(HFRadio40mBlockEntity::new, RadiocraftBlocks.HF_RADIO_40M.get()).build(null));
	public static final Supplier<BlockEntityType<HFRadio80mBlockEntity>> HF_RADIO_80M = BLOCK_ENTITY_TYPES.register("hf_radio_80m",
			() -> BlockEntityType.Builder.of(HFRadio80mBlockEntity::new, RadiocraftBlocks.HF_RADIO_80M.get()).build(null));
	public static final Supplier<BlockEntityType<HFRadioAllBandBlockEntity>> HF_RADIO_ALL_BAND = BLOCK_ENTITY_TYPES.register("all_band_radio",
			() -> BlockEntityType.Builder.of(HFRadioAllBandBlockEntity::new, RadiocraftBlocks.ALL_BAND_RADIO.get()).build(null));
	public static final Supplier<BlockEntityType<HFReceiverBlockEntity>> HF_RECEIVER = BLOCK_ENTITY_TYPES.register("hf_receiver",
			() -> BlockEntityType.Builder.of(HFReceiverBlockEntity::new, RadiocraftBlocks.HF_RECEIVER.get()).build(null));
	public static final Supplier<BlockEntityType<QRPRadio20mBlockEntity>> QRP_RADIO_20M = BLOCK_ENTITY_TYPES.register("qrp_radio_20m",
			() -> BlockEntityType.Builder.of(QRPRadio20mBlockEntity::new, RadiocraftBlocks.QRP_RADIO_20M.get()).build(null));
	public static final Supplier<BlockEntityType<QRPRadio40mBlockEntity>> QRP_RADIO_40M = BLOCK_ENTITY_TYPES.register("qrp_radio_40m",
			() -> BlockEntityType.Builder.of(QRPRadio40mBlockEntity::new, RadiocraftBlocks.QRP_RADIO_40M.get()).build(null));
	public static final Supplier<BlockEntityType<VHFBaseStationBlockEntity>> VHF_BASE_STATION = BLOCK_ENTITY_TYPES.register("vhf_base_station",
			() -> BlockEntityType.Builder.of(VHFBaseStationBlockEntity::new, RadiocraftBlocks.VHF_BASE_STATION.get()).build(null));
	public static final Supplier<BlockEntityType<VHFReceiverBlockEntity>> VHF_RECEIVER = BLOCK_ENTITY_TYPES.register("vhf_receiver",
			() -> BlockEntityType.Builder.of(VHFReceiverBlockEntity::new, RadiocraftBlocks.VHF_RECEIVER.get()).build(null));

	public static final Supplier<BlockEntityType<?>> DIGITAL_INTERFACE = BLOCK_ENTITY_TYPES.register("digital_interface",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.DigitalInterfaceBlockEntity::new, RadiocraftBlocks.DIGITAL_INTERFACE.get()).build(null));
	public static final Supplier<BlockEntityType<?>> DUPLEXER = BLOCK_ENTITY_TYPES.register("duplexer",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.DuplexerBlockEntity::new, RadiocraftBlocks.DUPLEXER.get()).build(null));
	public static final Supplier<BlockEntityType<?>> ANTENNA_TUNER = BLOCK_ENTITY_TYPES.register("antenna_tuner",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity::new, RadiocraftBlocks.ANTENNA_TUNER.get()).build(null));
	public static final Supplier<BlockEntityType<?>> VHF_REPEATER = BLOCK_ENTITY_TYPES.register("vhf_repeater",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity::new, RadiocraftBlocks.VHF_REPEATER.get()).build(null));

	public static final Supplier<BlockEntityType<?>> ANTENNA = BLOCK_ENTITY_TYPES.register("antenna",
			() -> BlockEntityType.Builder.of(AntennaBlockEntity::new,
					RadiocraftBlocks.BALUN_ONE_TO_ONE.get(),
					RadiocraftBlocks.BALUN_TWO_TO_ONE.get(),
					RadiocraftBlocks.SLIM_JIM_ANTENNA.get(),
					RadiocraftBlocks.J_POLE_ANTENNA.get(),
					RadiocraftBlocks.YAGI_ANTENNA.get()
			).build(null));

	public static final Supplier<BlockEntityType<?>> DESK_CHARGER = BLOCK_ENTITY_TYPES.register("desk_charger",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity::new, RadiocraftBlocks.DESK_CHARGER.get()).build(null));

	public static final Supplier<BlockEntityType<?>> SATELLITE_DISH = BLOCK_ENTITY_TYPES.register("satellite_dish",
			() -> BlockEntityType.Builder.of(com.arrl.radiocraft.common.blockentities.SatelliteDishBlockEntity::new, RadiocraftBlocks.SATELLITE_DISH.get()).build(null));

}
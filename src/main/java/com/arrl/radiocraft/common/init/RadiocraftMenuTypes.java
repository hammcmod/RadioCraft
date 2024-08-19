package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RadiocraftMenuTypes {

	public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, Radiocraft.MOD_ID);

	public static final Supplier<MenuType<LargeBatteryMenu>> LARGE_BATTERY = MENU_TYPES.register("large_battery", () -> IMenuTypeExtension.create(LargeBatteryMenu::new));
	public static final Supplier<MenuType<SolarPanelMenu>> SOLAR_PANEL = MENU_TYPES.register("solar_panel", () -> IMenuTypeExtension.create(SolarPanelMenu::new));
	public static final Supplier<MenuType<ChargeControllerMenu>> CHARGE_CONTROLLER = MENU_TYPES.register("charge_controller", () -> IMenuTypeExtension.create(ChargeControllerMenu::new));

	public static final Supplier<MenuType<HFRadio10mMenu>> HF_RADIO_10M = MENU_TYPES.register("hf_radio_10m", () -> IMenuTypeExtension.create(HFRadio10mMenu::new));
	public static final Supplier<MenuType<HFRadio20mMenu>> HF_RADIO_20M = MENU_TYPES.register("hf_radio_20m", () -> IMenuTypeExtension.create(HFRadio20mMenu::new));
	public static final Supplier<MenuType<HFRadio40mMenu>> HF_RADIO_40M = MENU_TYPES.register("hf_radio_40m", () -> IMenuTypeExtension.create(HFRadio40mMenu::new));
	public static final Supplier<MenuType<HFRadio80mMenu>> HF_RADIO_80M = MENU_TYPES.register("hf_radio_80m", () -> IMenuTypeExtension.create(HFRadio80mMenu::new));
	public static final Supplier<MenuType<HFReceiverMenu>> HF_RECEIVER = MENU_TYPES.register("hf_receiver", () -> IMenuTypeExtension.create(HFReceiverMenu::new));
	public static final Supplier<MenuType<QRPRadio20mMenu>> QRP_RADIO_20M = MENU_TYPES.register("qrp_radio_20m", () -> IMenuTypeExtension.create(QRPRadio20mMenu::new));
	public static final Supplier<MenuType<QRPRadio40mMenu>> QRP_RADIO_40M = MENU_TYPES.register("qrp_radio_40m", () -> IMenuTypeExtension.create(QRPRadio40mMenu::new));

	public static final Supplier<MenuType<VHFBaseStationMenu>> VHF_BASE_STATION = MENU_TYPES.register("vhf_base_station", () -> IMenuTypeExtension.create(VHFBaseStationMenu::new));

	public static final Supplier<MenuType<VHFReceiverMenu>> VHF_RECEIVER = MENU_TYPES.register("vhf_receiver", () -> IMenuTypeExtension.create(VHFReceiverMenu::new));

}

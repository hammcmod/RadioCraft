package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.menus.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RadiocraftMenuTypes {

	public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Radiocraft.MOD_ID);

	public static final RegistryObject<MenuType<LargeBatteryMenu>> LARGE_BATTERY = MENU_TYPES.register("large_battery", () -> IForgeMenuType.create(LargeBatteryMenu::new));
	public static final RegistryObject<MenuType<SolarPanelMenu>> SOLAR_PANEL = MENU_TYPES.register("solar_panel", () -> IForgeMenuType.create(SolarPanelMenu::new));
	public static final RegistryObject<MenuType<ChargeControllerMenu>> CHARGE_CONTROLLER = MENU_TYPES.register("charge_controller", () -> IForgeMenuType.create(ChargeControllerMenu::new));

	public static final RegistryObject<MenuType<HFRadio10mMenu>> HF_RADIO_10M = MENU_TYPES.register("hf_radio_10m", () -> IForgeMenuType.create(HFRadio10mMenu::new));
	public static final RegistryObject<MenuType<HFRadio20mMenu>> HF_RADIO_20M = MENU_TYPES.register("hf_radio_20m", () -> IForgeMenuType.create(HFRadio20mMenu::new));
	public static final RegistryObject<MenuType<HFRadio40mMenu>> HF_RADIO_40M = MENU_TYPES.register("hf_radio_40m", () -> IForgeMenuType.create(HFRadio40mMenu::new));
	public static final RegistryObject<MenuType<HFRadio80mMenu>> HF_RADIO_80M = MENU_TYPES.register("hf_radio_80m", () -> IForgeMenuType.create(HFRadio80mMenu::new));
	public static final RegistryObject<MenuType<HFReceiverMenu>> HF_RECEIVER = MENU_TYPES.register("hf_receiver", () -> IForgeMenuType.create(HFReceiverMenu::new));
	public static final RegistryObject<MenuType<QRPRadio20mMenu>> QRP_RADIO_20M = MENU_TYPES.register("qrp_radio_20m", () -> IForgeMenuType.create(QRPRadio20mMenu::new));
	public static final RegistryObject<MenuType<QRPRadio40mMenu>> QRP_RADIO_40M = MENU_TYPES.register("qrp_radio_40m", () -> IForgeMenuType.create(QRPRadio40mMenu::new));

	public static final RegistryObject<MenuType<VHFBaseStationMenu>> VHF_BASE_STATION = MENU_TYPES.register("vhf_base_station", () -> IForgeMenuType.create(VHFBaseStationMenu::new));

	public static final RegistryObject<MenuType<VHFReceiverMenu>> VHF_RECEIVER = MENU_TYPES.register("vhf_receiver", () -> IForgeMenuType.create(VHFReceiverMenu::new));

}

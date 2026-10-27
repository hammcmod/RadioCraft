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
	public static final Supplier<MenuType<HFRadioAllBandMenu>> HF_RADIO_ALL_BAND = MENU_TYPES.register("hf_radio_all_band", () -> IMenuTypeExtension.create(HFRadioAllBandMenu::new));
	public static final Supplier<MenuType<HFReceiverMenu>> HF_RECEIVER = MENU_TYPES.register("hf_receiver", () -> IMenuTypeExtension.create(HFReceiverMenu::new));
	public static final Supplier<MenuType<QRPRadio20mMenu>> QRP_RADIO_20M = MENU_TYPES.register("qrp_radio_20m", () -> IMenuTypeExtension.create(QRPRadio20mMenu::new));
	public static final Supplier<MenuType<QRPRadio40mMenu>> QRP_RADIO_40M = MENU_TYPES.register("qrp_radio_40m", () -> IMenuTypeExtension.create(QRPRadio40mMenu::new));

	public static final Supplier<MenuType<VHFBaseStationMenu>> VHF_BASE_STATION = MENU_TYPES.register("vhf_base_station", () -> IMenuTypeExtension.create(VHFBaseStationMenu::new));

	public static final Supplier<MenuType<VHFReceiverMenu>> VHF_RECEIVER = MENU_TYPES.register("vhf_receiver", () -> IMenuTypeExtension.create(VHFReceiverMenu::new));
	public static final Supplier<MenuType<DeskChargerMenu>> DESK_CHARGER = MENU_TYPES.register("desk_charger", () -> IMenuTypeExtension.create(DeskChargerMenu::new));
	public static final Supplier<MenuType<TeletypeMenu>> TELETYPE = MENU_TYPES.register("teletype", () -> IMenuTypeExtension.create(TeletypeMenu::new));
	public static final Supplier<MenuType<RadioInterfaceMenu>> RADIO_INTERFACE = MENU_TYPES.register("radio_interface", () -> IMenuTypeExtension.create(RadioInterfaceMenu::new));
	public static final Supplier<MenuType<AprsTerminalMenu>> APRS_TERMINAL = MENU_TYPES.register("aprs_h19", () -> IMenuTypeExtension.create(AprsTerminalMenu::new));
	public static final Supplier<MenuType<RadioFaxMenu>> RADIO_FAX = MENU_TYPES.register("radio_fax", () -> IMenuTypeExtension.create(RadioFaxMenu::new));
	public static final Supplier<MenuType<DuplexerMenu>> DUPLEXER = MENU_TYPES.register("duplexer", () -> IMenuTypeExtension.create(DuplexerMenu::new));
	public static final Supplier<MenuType<AntennaTunerMenu>> ANTENNA_TUNER = MENU_TYPES.register("antenna_tuner", () -> IMenuTypeExtension.create(AntennaTunerMenu::new));
	public static final Supplier<MenuType<DirectionFinderMenu>> DIRECTION_FINDER = MENU_TYPES.register("direction_finder", () -> IMenuTypeExtension.create(DirectionFinderMenu::new));
	public static final Supplier<MenuType<MicrowaveRadioMenu>> MICROWAVE_RADIO = MENU_TYPES.register("microwave_radio", () -> IMenuTypeExtension.create(MicrowaveRadioMenu::new));
	public static final Supplier<MenuType<com.arrl.radiocraft.common.menus.TapePlayerMenu>> TAPE_PLAYER = MENU_TYPES.register("tape_player", () -> IMenuTypeExtension.create(com.arrl.radiocraft.common.menus.TapePlayerMenu::new));
	public static final Supplier<MenuType<com.arrl.radiocraft.common.menus.CommercialRadioModuleMenu>> COMMERCIAL_RADIO_MODULE = MENU_TYPES.register("commercial_radio_module", () -> IMenuTypeExtension.create(com.arrl.radiocraft.common.menus.CommercialRadioModuleMenu::new));
	public static final Supplier<MenuType<com.arrl.radiocraft.common.menus.WefaxReceiverMenu>> WEFAX_RECEIVER = MENU_TYPES.register("wefax_receiver", () -> IMenuTypeExtension.create(com.arrl.radiocraft.common.menus.WefaxReceiverMenu::new));
	public static final Supplier<MenuType<TelephoneMenu>> TELEPHONE = MENU_TYPES.register("telephone", () -> IMenuTypeExtension.create(TelephoneMenu::new));
	public static final Supplier<MenuType<com.arrl.radiocraft.common.menus.IonosphericProcessorMenu>> IONOSPHERIC_PROCESSOR = MENU_TYPES.register("ionospheric_processor",
			() -> IMenuTypeExtension.create(com.arrl.radiocraft.common.menus.IonosphericProcessorMenu::new));
	public static final Supplier<MenuType<VHFRepeaterMenu>> VHF_REPEATER = MENU_TYPES.register("vhf_repeater", () -> IMenuTypeExtension.create(VHFRepeaterMenu::new));

}

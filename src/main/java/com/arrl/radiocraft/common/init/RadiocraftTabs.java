package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.crystal.CrystalBand;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registry class for creative mode tabs
 */
public class RadiocraftTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Radiocraft.MOD_ID);

    public static CreativeModeTab TAB = CreativeModeTab.builder().icon(() -> new ItemStack(RadiocraftItems.HF_RADIO_10M.get())).title(Component.translatable(Radiocraft.translationKey("tabs", "main_tab"))).displayItems((itemDisplayParameters, output) -> {

        // In the development environment, we load everything.
        // In the release version, we only enabled working items/blocks. So put all stuff not ready for release here.
        if (Radiocraft.IS_DEVELOPMENT_ENV) {
            output.accept(RadiocraftItems.MICROPHONE.get());
            output.accept(RadiocraftItems.SMALL_BATTERY.get());
            output.accept(RadiocraftItems.WATERPROOF_WIRE.get());
            output.accept(RadiocraftItems.SOLAR_PANEL.get());
            output.accept(RadiocraftItems.LARGE_BATTERY.get());
            output.accept(RadiocraftItems.CHARGE_CONTROLLER.get());
            output.accept(RadiocraftItems.VHF_REPEATER.get());
            output.accept(RadiocraftItems.ALL_BAND_RADIO.get());
            output.accept(RadiocraftItems.DUPLEXER.get());
            output.accept(RadiocraftItems.DUMMY_LOAD.get());
            output.accept(RadiocraftItems.RADIO_INTERFACE.get());
            output.accept(RadiocraftItems.APRS_TERMINAL.get());
            output.accept(RadiocraftItems.TRANSFER_INTERFACE.get());
			output.accept(RadiocraftItems.SATELLITE_DISH.get());
			output.accept(RadiocraftItems.IONOSONDE.get());
			output.accept(RadiocraftItems.FIVE_BTV_ANTENNA.get());
			output.accept(RadiocraftItems.IONOSPHERIC_PROCESSOR.get());
			output.accept(RadiocraftItems.WEFAX_RECEIVER.get());
		}

		// Anything below here should be in the release creative menu
        // The HF station and its antennas (working since the 2026-10 rework)
        RadiocraftItems.VHF_BASE_STATION_VARIANTS.values().forEach(output::accept);
        output.accept(RadiocraftItems.VHF_RECEIVER.get());
        RadiocraftItems.HF_RADIO_10M_VARIANTS.values().forEach(output::accept);
        RadiocraftItems.HF_RADIO_20M_VARIANTS.values().forEach(output::accept);
        RadiocraftItems.HF_RADIO_40M_VARIANTS.values().forEach(output::accept);
        RadiocraftItems.HF_RADIO_80M_VARIANTS.values().forEach(output::accept);
        output.accept(RadiocraftItems.HF_RECEIVER.get());
        output.accept(RadiocraftItems.QRP_RADIO_20M.get());
        output.accept(RadiocraftItems.QRP_RADIO_40M.get());
        output.accept(RadiocraftItems.ANTENNA_POLE.get());
        output.accept(RadiocraftItems.ANTENNA_TUNER.get());
        output.accept(RadiocraftItems.ANTENNA_WIRE.get());
        output.accept(RadiocraftItems.ANTENNA_CONNECTOR.get());
        output.accept(RadiocraftItems.BALUN_ONE_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_FOUR_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_TWO_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_NINE_TO_ONE.get());
        output.accept(RadiocraftItems.COAX_WIRE.get());
        output.accept(RadiocraftItems.PASSTHROUGH.get());
        output.accept(RadiocraftItems.PARROT_CONTROLLER.get());
        output.accept(RadiocraftItems.YAGI_ANTENNA.get());
        output.accept(RadiocraftItems.J_POLE_ANTENNA.get());
        output.accept(RadiocraftItems.SLIM_JIM_ANTENNA.get());
        output.accept(RadiocraftItems.SOLAR_WEATHER_STATION.get());

        output.accept(RadiocraftItems.DESK_CHARGER.get());

        // General crafting-only ingredients (Not used yet, but they are "finished")
        output.accept(RadiocraftItems.HF_CIRCUIT_BOARD.get());
        output.accept(RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get());
        output.accept(RadiocraftItems.FERRITE_CORE.get());
        output.accept(RadiocraftItems.COAXIAL_CORE.get());
        output.accept(RadiocraftItems.INSULATION.get());

        // The antenna analyzer
        output.accept(RadiocraftItems.ANTENNA_ANALYZER.get());

        // Crafting ingredients for the VHF Handheld
        for(CrystalBand band : CrystalBand.values()) // Raw crystals, then the encased ones, pure to poor.
            for(CrystalQuality quality : List.of(CrystalQuality.PURE, CrystalQuality.GOOD, CrystalQuality.POOR))
                output.accept(RadiocraftItems.CRYSTALS.get(band).get(quality).get());
        for(CrystalBand band : CrystalBand.values())
            for(CrystalQuality quality : List.of(CrystalQuality.PURE, CrystalQuality.GOOD, CrystalQuality.POOR))
                output.accept(RadiocraftItems.ENCASED_CRYSTALS.get(band).get(quality).get());
        output.accept(RadiocraftItems.RADIO_SPEAKER.get());
        output.accept(RadiocraftItems.HAND_MICROPHONE.get());
        output.accept(RadiocraftItems.WIRE.get());

        RadiocraftItems.VHF_HANDHELD_VARIANTS.values().forEach(output::accept);

    }).build();

    public static Supplier<CreativeModeTab> RADIOCRAFT_TAB = CREATIVE_TABS.register("radiocraft_tabs", () -> TAB);
}

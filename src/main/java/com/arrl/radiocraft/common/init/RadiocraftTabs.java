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
			output.accept(RadiocraftItems.SATELLITE_DISH.get()); // The microwave dish.
			output.accept(RadiocraftItems.IONOSPHERIC_PROCESSOR.get()); // Built and glowing, does not read the sky yet.
			// Registered 2026-10-26, not yet playtested and without recipes (bar the map direction finder's).
			output.accept(RadiocraftItems.TOWER_BEAM_THIN.get());
			output.accept(RadiocraftItems.TOWER_BEAM.get());
			output.accept(RadiocraftItems.TOWER_BEAM_THICK.get());
			output.accept(RadiocraftItems.MICROWAVE_HORN.get());
			RadiocraftItems.DESK_SPEAKERS.values().forEach(item -> output.accept(item.get()));
			output.accept(RadiocraftItems.LARGE_SPEAKER.get());
			output.accept(RadiocraftItems.PREMIUM_SPEAKER_MARBLE.get());
			output.accept(RadiocraftItems.PREMIUM_SPEAKER_IRON.get());
			output.accept(RadiocraftItems.PREMIUM_SPEAKER_GOLD.get());
			output.accept(RadiocraftItems.WALL_SPEAKER.get());
			output.accept(RadiocraftItems.LOUDSPEAKER.get());
			output.accept(RadiocraftItems.DIRECTION_FINDER.get());
			output.accept(RadiocraftItems.MAP_DIRECTION_FINDER.get());
			output.accept(RadiocraftItems.DIRECTION_FINDER_MODULE.get());
			output.accept(RadiocraftItems.WORLD_MAP_MODULE.get());
			output.accept(RadiocraftItems.HANDHELD_DIRECTION_FINDER.get());
			output.accept(RadiocraftItems.MICROWAVE_RADIO.get());
			output.accept(RadiocraftItems.TELEPHONE.get());
			output.accept(RadiocraftItems.HF_MANPACK.get());
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
        output.accept(RadiocraftItems.TOWER_FOOTHOLD.get());
        output.accept(RadiocraftItems.OLD_TOWER_FOOTHOLD.get());
        output.accept(RadiocraftItems.ANTENNA_TUNER.get());
        output.accept(RadiocraftItems.ANTENNA_WIRE.get());
        output.accept(RadiocraftItems.ANTENNA_CONNECTOR.get());
        output.accept(RadiocraftItems.BALUN_ONE_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_FOUR_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_TWO_TO_ONE.get());
        output.accept(RadiocraftItems.BALUN_NINE_TO_ONE.get());
        output.accept(RadiocraftItems.COAX_WIRE.get());
        RadiocraftItems.COLOURED_COAX.values().forEach(coax -> output.accept(coax.get()));
        output.accept(RadiocraftItems.PASSTHROUGH.get());
        output.accept(RadiocraftItems.PARROT_CONTROLLER.get());
        output.accept(RadiocraftItems.YAGI_ANTENNA.get());
        output.accept(RadiocraftItems.J_POLE_ANTENNA.get());
        output.accept(RadiocraftItems.SLIM_JIM_ANTENNA.get());
        output.accept(RadiocraftItems.RINGO_ANTENNA.get());
        output.accept(RadiocraftItems.FOLDED_DIPOLE_ANTENNA.get());
        output.accept(RadiocraftItems.TELETYPE.get());
        output.accept(RadiocraftItems.ALL_BAND_RADIO.get());
        output.accept(RadiocraftItems.MICROPHONE.get());
        output.accept(RadiocraftItems.AUTO_TUNER.get());
        output.accept(RadiocraftItems.DUMMY_LOAD.get());
        output.accept(RadiocraftItems.FIVE_BTV_ANTENNA.get());
        output.accept(RadiocraftItems.IONOSONDE.get());

        // Repeaters
        output.accept(RadiocraftItems.VHF_REPEATER.get());
        output.accept(RadiocraftItems.COMMERCIAL_REPEATER.get());
        output.accept(RadiocraftItems.COMMERCIAL_RADIO_MODULE.get());
        output.accept(RadiocraftItems.DUPLEXER.get());

        // APRS and packet
        output.accept(RadiocraftItems.RADIO_INTERFACE.get());
        output.accept(RadiocraftItems.APRS_TERMINAL.get());
        RadiocraftItems.RADIO_FAXES.values().forEach(item -> output.accept(item.get()));
        output.accept(RadiocraftItems.TELETYPE_ROLL.get());
        output.accept(RadiocraftItems.WEATHER_STATION.get());
        output.accept(RadiocraftItems.WEFAX_RECEIVER.get());
        output.accept(RadiocraftItems.TAPE_PLAYER.get());
        for(int reel = 0; reel < com.arrl.radiocraft.common.datacomponents.TapeContents.REEL_COLOURS; reel++) {
            net.minecraft.world.item.ItemStack tape = new net.minecraft.world.item.ItemStack(RadiocraftItems.TAPE_REEL.get());
            tape.set(com.arrl.radiocraft.common.init.RadiocraftDataComponent.TAPE_CONTENTS.get(),
                    com.arrl.radiocraft.common.datacomponents.TapeContents.BLANK.withColorway(reel * com.arrl.radiocraft.common.datacomponents.TapeContents.LABEL_COLOURS));
            output.accept(tape);
        }

        // Power: radios need it by default (radios_need_power)
        output.accept(RadiocraftItems.SOLAR_PANEL.get());
        output.accept(RadiocraftItems.CHARGE_CONTROLLER.get());
        output.accept(RadiocraftItems.LARGE_BATTERY.get());
        output.accept(RadiocraftItems.SMALL_BATTERY.get());
        output.accept(RadiocraftItems.WATERPROOF_WIRE.get());

        output.accept(RadiocraftItems.DESK_CHARGER.get());

        // General crafting-only ingredients (Not used yet, but they are "finished")
        output.accept(RadiocraftItems.HF_CIRCUIT_BOARD.get());
        output.accept(RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get());
        output.accept(RadiocraftItems.FERRITE_CORE.get());
        output.accept(RadiocraftItems.COAXIAL_CORE.get());
        output.accept(RadiocraftItems.INSULATION.get());
        output.accept(RadiocraftItems.MICROPHONE_ELEMENT.get());

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

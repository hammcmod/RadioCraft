package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class RadiocraftBlockTagsProvider extends BlockTagsProvider {

    public RadiocraftBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Radiocraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Every block that drops itself needs a pickaxe, as they copy stone (or iron bars). Wires break by hand.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(RadiocraftBlockLootProvider.blocks().stream()
                .filter(block -> block.defaultBlockState().requiresCorrectToolForDrops()).toArray(Block[]::new));
        // The alternator only exists with Create (compat/create).
        tag(BlockTags.MINEABLE_WITH_PICKAXE).addOptional(com.arrl.radiocraft.Radiocraft.id("alternator"));

        tag(BlockTags.CLIMBABLE).add(RadiocraftBlocks.TOWER_FOOTHOLD.get(), RadiocraftBlocks.OLD_TOWER_FOOTHOLD.get());

        // Define the POWER_BLOCKS tag
        tag(RadiocraftTags.Blocks.POWER_BLOCKS)
                .add(RadiocraftBlocks.WIRE.get())
                .add(RadiocraftBlocks.WATERPROOF_WIRE.get())
                .add(RadiocraftBlocks.COLOURED_WIRE.values().stream().map(net.neoforged.neoforge.registries.DeferredHolder::get).toArray(Block[]::new))
                .add(RadiocraftBlocks.WIRE_JUNCTION.get())
                .add(RadiocraftBlocks.SOLAR_PANEL.get())
                .add(RadiocraftBlocks.LARGE_BATTERY.get())
                .add(RadiocraftBlocks.CHARGE_CONTROLLER.get())
                .add(RadiocraftBlocks.AUTO_TUNER.get())
                .add(RadiocraftBlocks.ALL_BAND_RADIO.get())
                .add(RadiocraftBlocks.HF_RADIO_10M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_20M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_40M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_80M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RECEIVER.get())
                .add(RadiocraftBlocks.QRP_RADIO_20M.get())
                .add(RadiocraftBlocks.QRP_RADIO_40M.get())
                .add(RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.VHF_RECEIVER.get())
                .add(RadiocraftBlocks.VHF_REPEATER.get())
                .add(RadiocraftBlocks.COMMERCIAL_REPEATER.get()) // Its charger runs off the station's power wire, to any cell of the rack.
                .add(RadiocraftBlocks.DIRECTION_FINDER_MODULE.get()) // Passes power on to the finder and the next module.
                .add(RadiocraftBlocks.MICROWAVE_RADIO.get())
                .add(RadiocraftBlocks.DIRECTION_FINDER.get(), RadiocraftBlocks.MAP_DIRECTION_FINDER.get())
                .add(RadiocraftBlocks.APRS_TERMINAL.get()); // The H19 is mains fed (TerminalPowerGameTests).

        // Data wire (2026-10-30) links these to radios and to each other; power wire no longer does. Radios take it too
        // (WireBlock#isDataConnection).
        tag(RadiocraftTags.Blocks.DATA_BLOCKS)
                .add(RadiocraftBlocks.DATA_WIRE.get())
                .add(RadiocraftBlocks.WALL_SPEAKER.get(), RadiocraftBlocks.LOUDSPEAKER.get()) // The wire is their speaker line.
                .add(RadiocraftBlocks.MICROWAVE_RADIO.get()) // The line to the stations it serves and its site's other radios:
                .add(RadiocraftBlocks.TELEPHONE.get(), RadiocraftBlocks.APRS_TERMINAL.get(),
                        RadiocraftBlocks.DIRECTION_FINDER.get(), RadiocraftBlocks.MAP_DIRECTION_FINDER.get())
                .add(RadiocraftBlocks.RADIO_FAXES.values().stream().map(net.neoforged.neoforge.registries.DeferredHolder::get).toArray(Block[]::new))
                .add(RadiocraftBlocks.RADIO_INTERFACE.get(), RadiocraftBlocks.TELETYPE.get(), RadiocraftBlocks.WEFAX_RECEIVER.get(),
                        RadiocraftBlocks.TAPE_PLAYER.get(), RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get());

        // Signal sources that only report a reading: Another Weather Mod's instruments re-read the weather every second, and
        // in a storm each new reading popped a nearby radio with interference (2026-10-30).
        tag(RadiocraftTags.Blocks.QUIET_SIGNAL_SOURCES)
                .add(RadiocraftBlocks.WEFAX_RECEIVER.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "wind_vane"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "barometer"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "thermometer"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "rain_gauge"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "solar_detector"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("anotherweathermod", "tornado_sensor"));

        // Define the COAX_BLOCKS tag
        tag(RadiocraftTags.Blocks.COAX_BLOCKS)
                .add(RadiocraftBlocks.COAX_WIRE.get())
                .add(RadiocraftBlocks.COLOURED_COAX.values().stream().map(net.neoforged.neoforge.registries.DeferredHolder::get).toArray(Block[]::new))
                .add(RadiocraftBlocks.DUPLEXER.get())
                .add(RadiocraftBlocks.ANTENNA_SPLITTER.get())
                .add(RadiocraftBlocks.ANTENNA_SWITCH.get())
                .add(RadiocraftBlocks.ANTENNA_TUNER.get())
                .add(RadiocraftBlocks.AUTO_TUNER.get())
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get())
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_FOUR_TO_ONE.get(), RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_NINE_TO_ONE.get())
                .add(RadiocraftBlocks.J_POLE_ANTENNA.get())
                .add(RadiocraftBlocks.RINGO_ANTENNA.get(), RadiocraftBlocks.FOLDED_DIPOLE_ANTENNA.get())
                .add(net.minecraft.world.level.block.Blocks.LIGHTNING_ROD) // A very basic VHF antenna.
                .add(RadiocraftBlocks.IONOSONDE.get())
                .add(RadiocraftBlocks.FIVE_BTV_ANTENNA.get())
                .add(RadiocraftBlocks.SLIM_JIM_ANTENNA.get())
                .add(RadiocraftBlocks.YAGI_ANTENNA.get())
                .add(RadiocraftBlocks.DUMMY_LOAD.get())
                .add(RadiocraftBlocks.DIRECTION_FINDER_MODULE.get())
                .add(RadiocraftBlocks.IONOSPHERIC_PROCESSOR.get()) // Its coax runs to the Ionosonde Antenna.
                .add(RadiocraftBlocks.MICROWAVE_RADIO.get(), RadiocraftBlocks.SATELLITE_DISH.get(), RadiocraftBlocks.MICROWAVE_HORN.get())
                .add(RadiocraftBlocks.ALL_BAND_RADIO.get())
                .add(RadiocraftBlocks.HF_RADIO_10M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_20M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_40M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RADIO_80M_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.HF_RECEIVER.get())
                .add(RadiocraftBlocks.QRP_RADIO_20M.get())
                .add(RadiocraftBlocks.QRP_RADIO_40M.get())
                .add(RadiocraftBlocks.VHF_BASE_STATION_VARIANTS.values().toArray(Block[]::new))
                .add(RadiocraftBlocks.VHF_RECEIVER.get())
                .add(RadiocraftBlocks.VHF_REPEATER.get())
                .add(RadiocraftBlocks.COMMERCIAL_REPEATER.get());

        // Define the ANTENNA_BLOCKS tag
        tag(RadiocraftTags.Blocks.ANTENNA_BLOCKS)
                .add(RadiocraftBlocks.COAX_WIRE.get())
                .add(RadiocraftBlocks.COLOURED_COAX.values().stream().map(net.neoforged.neoforge.registries.DeferredHolder::get).toArray(Block[]::new))
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get()) // I'm not convinced this is the feed end of the antenna - jrddunbr
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_FOUR_TO_ONE.get(), RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_NINE_TO_ONE.get())
                .add(RadiocraftBlocks.RINGO_ANTENNA.get(), RadiocraftBlocks.FOLDED_DIPOLE_ANTENNA.get())
                .add(RadiocraftBlocks.J_POLE_ANTENNA.get())
                .add(RadiocraftBlocks.IONOSONDE.get())
                .add(RadiocraftBlocks.FIVE_BTV_ANTENNA.get())
                .add(RadiocraftBlocks.SLIM_JIM_ANTENNA.get())
                .add(RadiocraftBlocks.YAGI_ANTENNA.get())
                .add(RadiocraftBlocks.DUMMY_LOAD.get())
                .add(RadiocraftBlocks.ANTENNA_TUNER.get())
                .add(RadiocraftBlocks.AUTO_TUNER.get());

        // Define the ANTENNA_WIRE_HOLDERS tag
        tag(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS)
                .add(RadiocraftBlocks.ANTENNA_POLE.get()) // I'm not convinced this is used for this - jrddunbr
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get())
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_FOUR_TO_ONE.get(), RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_NINE_TO_ONE.get());


    }
}

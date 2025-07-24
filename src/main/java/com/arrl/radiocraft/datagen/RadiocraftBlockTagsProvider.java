package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.HolderLookup;
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
        // Define the POWER_BLOCKS tag
        tag(RadiocraftTags.Blocks.POWER_BLOCKS)
                .add(RadiocraftBlocks.WIRE.get())
                .add(RadiocraftBlocks.WATERPROOF_WIRE.get())
                .add(RadiocraftBlocks.SOLAR_PANEL.get())
                .add(RadiocraftBlocks.LARGE_BATTERY.get())
                .add(RadiocraftBlocks.CHARGE_CONTROLLER.get())
                .add(RadiocraftBlocks.ALL_BAND_RADIO.get())
                .add(RadiocraftBlocks.HF_RADIO_10M.get())
                .add(RadiocraftBlocks.HF_RADIO_20M.get())
                .add(RadiocraftBlocks.HF_RADIO_40M.get())
                .add(RadiocraftBlocks.HF_RADIO_80M.get())
                .add(RadiocraftBlocks.HF_RECEIVER.get())
                .add(RadiocraftBlocks.QRP_RADIO_20M.get())
                .add(RadiocraftBlocks.QRP_RADIO_40M.get())
                .add(RadiocraftBlocks.VHF_BASE_STATION.get())
                .add(RadiocraftBlocks.VHF_RECEIVER.get())
                .add(RadiocraftBlocks.VHF_REPEATER.get())
                .add(RadiocraftBlocks.DIGITAL_INTERFACE.get());

        // Define the COAX_BLOCKS tag
        tag(RadiocraftTags.Blocks.COAX_BLOCKS)
                .add(RadiocraftBlocks.COAX_WIRE.get())
                .add(RadiocraftBlocks.DUPLEXER.get())
                .add(RadiocraftBlocks.ANTENNA_TUNER.get())
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get())
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
                .add(RadiocraftBlocks.J_POLE_ANTENNA.get())
                .add(RadiocraftBlocks.SLIM_JIM_ANTENNA.get())
                .add(RadiocraftBlocks.YAGI_ANTENNA.get());

        // Define the ANTENNA_BLOCKS tag
        tag(RadiocraftTags.Blocks.ANTENNA_BLOCKS)
                .add(RadiocraftBlocks.COAX_WIRE.get())
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get()) // I'm not convinced this is the feed end of the antenna - jrddunbr
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_TWO_TO_ONE.get())
                .add(RadiocraftBlocks.J_POLE_ANTENNA.get())
                .add(RadiocraftBlocks.SLIM_JIM_ANTENNA.get())
                .add(RadiocraftBlocks.YAGI_ANTENNA.get())
                .add(RadiocraftBlocks.ANTENNA_TUNER.get());

        // Define the ANTENNA_WIRE_HOLDERS tag
        tag(RadiocraftTags.Blocks.ANTENNA_WIRE_HOLDERS)
                .add(RadiocraftBlocks.ANTENNA_POLE.get()) // I'm not convinced this is used for this - jrddunbr
                .add(RadiocraftBlocks.ANTENNA_CONNECTOR.get())
                .add(RadiocraftBlocks.BALUN_ONE_TO_ONE.get())
                .add(RadiocraftBlocks.BALUN_TWO_TO_ONE.get());


    }
}
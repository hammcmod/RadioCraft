package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RadiocraftRecipesProvider extends RecipeProvider {
    public RadiocraftRecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        // Basic components
        buildWireRecipe(recipeOutput);
        buildWaterproofWireRecipe(recipeOutput);
        buildAntennaPoleRecipe(recipeOutput);
        buildAntennaWireRecipe(recipeOutput);
        buildFerriteCore(recipeOutput);
        buildLargeBatteryRecipe(recipeOutput);
        buildCoaxialCoreRecipe(recipeOutput);
        buildMicrophoneRecipe(recipeOutput);
        buildSolarPanelRecipe(recipeOutput);
        buildRadioCrystalRecipe(recipeOutput);
        buildRadioSpeakerRecipe(recipeOutput);
        buildCoaxWireRecipe(recipeOutput);
        buildDuplexerRecipe(recipeOutput);
        buildSmallBatteryRecipe(recipeOutput);
        buildDigitalInterfaceRecipe(recipeOutput);
        buildHandMicrophoneRecipe(recipeOutput);
        buildAntennaToolsRecipe(recipeOutput);
        buildHfCircuitBoardRecipe(recipeOutput);

        // Receivers and radios
        buildHfReceiverRecipe(recipeOutput);
        buildVhfReceiverRecipe(recipeOutput);
        buildVhfBaseStationRecipe(recipeOutput);
        buildSolarWeatherStationRecipe(recipeOutput);
        buildHfRadio10mRecipe(recipeOutput);
        buildHfRadio20mRecipe(recipeOutput);
        buildHfRadio40mRecipe(recipeOutput);
        buildHfRadio80mRecipe(recipeOutput);
        buildVhfRepeaterRecipe(recipeOutput);
        buildVhfHandheldRecipe(recipeOutput);

        // All band radio variants (shapeless)
        buildAllBandRadioReceiverRecipe(recipeOutput);
        buildAllBandRadioBaseStationRecipe(recipeOutput);
        buildAllBandRadioHandheldRecipe(recipeOutput);
        buildAllBandRadioRepeaterRecipe(recipeOutput);
    }

    private void buildWireRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.WIRE.get(), 9)
                .pattern("#  ")
                .pattern(" # ")
                .pattern("  #")
                .define('#', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);
    }

    private void buildWaterproofWireRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.WATERPROOF_WIRE.get(), 3)
                .pattern("KKK")
                .pattern("###")
                .define('#', RadiocraftItems.WIRE.get())
                .define('K', Items.DRIED_KELP)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildAntennaPoleRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_POLE.get(), 1)
                .pattern(" # ")
                .pattern(" # ")
                .pattern(" W ")
                .define('#', Items.IRON_INGOT)
                .define('W', RadiocraftItems.WIRE.get())
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildAntennaWireRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_WIRE.get(), 1)
                .pattern("K# ")
                .pattern("K# ")
                .pattern("K# ")
                .define('#', RadiocraftItems.WIRE.get())
                .define('K', Items.DRIED_KELP)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildFerriteCore(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.FERRITE_CORE.get(), 1)
                .pattern("BBB")
                .pattern("B#B")
                .pattern("BBB")
                .define('#', Items.IRON_INGOT)
                .define('B', Items.CHARCOAL)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);
    }

    private void buildLargeBatteryRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.LARGE_BATTERY.get(), 1)
                .pattern("LSL")
                .pattern("LSL")
                .pattern("LSL")
                .define('L', Tags.Items.STRIPPED_LOGS)
                .define('S', RadiocraftItems.SMALL_BATTERY.get())
                .unlockedBy("has_small_battery", has(RadiocraftItems.SMALL_BATTERY.get()))
                .save(recipeOutput);
    }

    private void buildCoaxialCoreRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.COAXIAL_CORE.get(), 3)
                .pattern("KKK")
                .pattern("CCC")
                .pattern("KKK")
                .define('K', Items.DRIED_KELP)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);
    }

    private void buildMicrophoneRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.MICROPHONE.get(), 1)
                .pattern(" M ")
                .pattern(" D ")
                .pattern(" C ")
                .define('C', RadiocraftItems.WIRE.get())
                .define('D', Items.IRON_INGOT)
                .define('M', RadiocraftItems.HAND_MICROPHONE.get())
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildSolarPanelRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SOLAR_PANEL.get(), 1)
                .pattern("GGG")
                .pattern("WDW")
                .pattern("   ")
                .define('W', RadiocraftItems.WIRE.get())
                .define('G', Items.GLASS)
                .define('D', Items.DAYLIGHT_DETECTOR)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildRadioCrystalRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.RADIO_CRYSTAL.get(), 1)
                .pattern(" # ")
                .pattern("IAI")
                .pattern(" # ")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);
    }

    private void buildRadioSpeakerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.RADIO_SPEAKER.get(), 1)
                .pattern("   ")
                .pattern("PAP")
                .pattern("P#P")
                .define('#', RadiocraftItems.WIRE.get())
                .define('P', ItemTags.PLANKS)
                .define('A', Items.NOTE_BLOCK)
                .unlockedBy("has_note_block", has(Items.NOTE_BLOCK))
                .save(recipeOutput);
    }

    private void buildCoaxWireRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.COAX_WIRE.get(), 3)
                .pattern("KKK")
                .pattern("###")
                .pattern("CCC")
                .define('#', RadiocraftItems.WIRE.get())
                .define('K', Items.DRIED_KELP)
                .define('C', RadiocraftItems.COAXIAL_CORE.get())
                .unlockedBy("has_coaxial_core", has(RadiocraftItems.COAXIAL_CORE.get()))
                .save(recipeOutput);
    }

    private void buildDuplexerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.DUPLEXER.get(), 1)
                .pattern("#L#")
                .pattern("#B#")
                .pattern("#C#")
                .define('#', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('L', Items.LIGHTNING_ROD)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildSmallBatteryRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SMALL_BATTERY.get(), 1)
                .pattern("IGI")
                .pattern("GCG")
                .pattern("#G#")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('C', Items.COAL)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildDigitalInterfaceRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.DIGITAL_INTERFACE.get(), 1)
                .pattern("   ")
                .pattern("#MS")
                .pattern("##D")
                .define('#', RadiocraftItems.WIRE.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('M', RadiocraftItems.MICROPHONE.get())
                .define('D', Items.DIAMOND)
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildHandMicrophoneRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HAND_MICROPHONE.get(), 1)
                .pattern(" B ")
                .pattern("WCW")
                .pattern("   ")
                .define('W', RadiocraftItems.WIRE.get())
                .define('C', Items.AMETHYST_SHARD)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildAntennaToolsRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_TUNER.get(), 1)
                .pattern("###")
                .pattern("#L#")
                .pattern("BEC")
                .define('#', RadiocraftItems.WIRE.get())
                .define('L', Items.LIGHTNING_ROD)
                .define('C', Items.COPPER_INGOT)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .define('E', Items.EMERALD)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(recipeOutput);
    }

    private void buildHfCircuitBoardRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_CIRCUIT_BOARD.get(), 1)
                .pattern("SSS")
                .pattern("#RN")
                .pattern("III")
                .define('#', RadiocraftItems.WIRE.get())
                .define('S', Items.SAND)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_TORCH)
                .define('N', Items.QUARTZ)
                .unlockedBy("has_redstone_torch", has(Items.REDSTONE_TORCH))
                .save(recipeOutput);
    }

    private void buildHfReceiverRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RECEIVER.get(), 1)
                .pattern("III")
                .pattern("C#S")
                .pattern("L#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('C', Items.COPPER_INGOT)
                .define('L', Items.LIGHTNING_ROD)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildVhfReceiverRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_RECEIVER.get(), 1)
                .pattern("IIW")
                .pattern("G#S")
                .pattern("I#W")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GOLD_INGOT)
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildVhfBaseStationRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_BASE_STATION.get(), 1)
                .pattern("III")
                .pattern("HRS")
                .pattern("#WW")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('H', RadiocraftItems.HAND_MICROPHONE.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('W', ItemTags.PLANKS)
                .unlockedBy("has_radio_crystal", has(RadiocraftItems.RADIO_CRYSTAL.get()))
                .save(recipeOutput);
    }

    private void buildSolarWeatherStationRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SOLAR_WEATHER_STATION.get(), 1)
                .pattern("ATT")
                .pattern("AR#")
                .pattern("L#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('A', RadiocraftItems.ANTENNA_POLE.get())
                .define('L', Items.LIGHTNING_ROD)
                .define('T', Items.TINTED_GLASS)
                .unlockedBy("has_antenna_pole", has(RadiocraftItems.ANTENNA_POLE.get()))
                .save(recipeOutput);
    }

    private void buildHfRadio10mRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RADIO_10M.get(), 1)
                .pattern("GGG")
                .pattern("HRS")
                .pattern("#WW")
                .define('#', RadiocraftItems.WIRE.get())
                .define('G', Items.GOLD_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('H', RadiocraftItems.HAND_MICROPHONE.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('W', ItemTags.PLANKS)
                .unlockedBy("has_radio_crystal", has(RadiocraftItems.RADIO_CRYSTAL.get()))
                .save(recipeOutput);
    }

    private void buildHfRadio20mRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RADIO_20M.get(), 1)
                .pattern("BCC")
                .pattern("#RT")
                .pattern("IBI")
                .define('#', RadiocraftItems.WIRE.get())
                .define('C', Items.COPPER_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('T', Items.TINTED_GLASS)
                .define('I', Items.IRON_INGOT)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildHfRadio40mRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RADIO_40M.get(), 1)
                .pattern("CBC")
                .pattern("#RT")
                .pattern("IBI")
                .define('#', RadiocraftItems.WIRE.get())
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('T', Items.TINTED_GLASS)
                .define('I', Items.IRON_INGOT)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildHfRadio80mRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RADIO_80M.get(), 1)
                .pattern("CCB")
                .pattern("#RT")
                .pattern("IBI")
                .define('#', RadiocraftItems.WIRE.get())
                .define('C', Items.COPPER_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('T', Items.TINTED_GLASS)
                .define('I', Items.IRON_INGOT)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildVhfRepeaterRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_REPEATER.get(), 1)
                .pattern("ITI")
                .pattern("CBR")
                .pattern("I#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('B', RadiocraftItems.VHF_BASE_STATION.get())
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.IRON_TRAPDOOR)
                .unlockedBy("has_vhf_base_station", has(RadiocraftItems.VHF_BASE_STATION.get()))
                .save(recipeOutput);
    }

    private void buildVhfHandheldRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_HANDHELD.get(), 1)
                .pattern("L  ")
                .pattern("MRS")
                .pattern("BBB")
                .define('R', RadiocraftItems.RADIO_CRYSTAL.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('L', Items.LIGHTNING_ROD)
                .define('M', RadiocraftItems.HAND_MICROPHONE.get())
                .define('B', ItemTags.BUTTONS)
                .unlockedBy("has_radio_crystal", has(RadiocraftItems.RADIO_CRYSTAL.get()))
                .save(recipeOutput);
    }

    private void buildAllBandRadioReceiverRecipe(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_RADIO.get(), 1)
                .requires(RadiocraftItems.HF_RADIO_10M.get())
                .requires(RadiocraftItems.HF_RADIO_20M.get())
                .requires(RadiocraftItems.HF_RADIO_40M.get())
                .requires(RadiocraftItems.HF_RADIO_80M.get())
                .requires(RadiocraftItems.VHF_RECEIVER.get())
                .unlockedBy("has_hf_radio_10m", has(RadiocraftItems.HF_RADIO_10M.get()))
                .save(recipeOutput, "radiocraft:all_band_radio_from_receiver");
    }

    private void buildAllBandRadioBaseStationRecipe(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_RADIO.get(), 1)
                .requires(RadiocraftItems.HF_RADIO_10M.get())
                .requires(RadiocraftItems.HF_RADIO_20M.get())
                .requires(RadiocraftItems.HF_RADIO_40M.get())
                .requires(RadiocraftItems.HF_RADIO_80M.get())
                .requires(RadiocraftItems.VHF_BASE_STATION.get())
                .unlockedBy("has_hf_radio_10m", has(RadiocraftItems.HF_RADIO_10M.get()))
                .save(recipeOutput, "radiocraft:all_band_radio_from_base_station");
    }

    private void buildAllBandRadioHandheldRecipe(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_RADIO.get(), 1)
                .requires(RadiocraftItems.HF_RADIO_10M.get())
                .requires(RadiocraftItems.HF_RADIO_20M.get())
                .requires(RadiocraftItems.HF_RADIO_40M.get())
                .requires(RadiocraftItems.HF_RADIO_80M.get())
                .requires(RadiocraftItems.VHF_HANDHELD.get())
                .unlockedBy("has_hf_radio_10m", has(RadiocraftItems.HF_RADIO_10M.get()))
                .save(recipeOutput, "radiocraft:all_band_radio_from_handheld");
    }

    private void buildAllBandRadioRepeaterRecipe(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_RADIO.get(), 1)
                .requires(RadiocraftItems.HF_RADIO_10M.get())
                .requires(RadiocraftItems.HF_RADIO_20M.get())
                .requires(RadiocraftItems.HF_RADIO_40M.get())
                .requires(RadiocraftItems.HF_RADIO_80M.get())
                .requires(RadiocraftItems.VHF_REPEATER.get())
                .unlockedBy("has_hf_radio_10m", has(RadiocraftItems.HF_RADIO_10M.get()))
                .save(recipeOutput, "radiocraft:all_band_radio_from_repeater");
    }
}
package com.arrl.radiocraft.datagen;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.CrystalVariants;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.crystal.CrystalBand;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class RadiocraftRecipesProvider extends RecipeProvider {
    public RadiocraftRecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        // Basic components
        buildWireRecipe(recipeOutput);
        buildWaterproofWireRecipe(recipeOutput);
        buildInsulationRecipes(recipeOutput);
        buildAntennaPoleRecipe(recipeOutput);
        buildAntennaWireRecipe(recipeOutput);
        buildFerriteCore(recipeOutput);
        buildLargeBatteryRecipe(recipeOutput);
        buildCoaxialCoreRecipe(recipeOutput);
        buildMicrophoneRecipe(recipeOutput);
        buildSolarPanelRecipe(recipeOutput);
        buildEncasedCrystalRecipes(recipeOutput);
        buildRadioSpeakerRecipe(recipeOutput);
        buildCoaxWireRecipe(recipeOutput);
        buildDuplexerRecipe(recipeOutput);
        buildSmallBatteryRecipe(recipeOutput);
        buildRadioInterfaceRecipe(recipeOutput);
        buildParrotControllerRecipe(recipeOutput);
        buildHandMicrophoneRecipe(recipeOutput);
        buildMicrophoneElementRecipe(recipeOutput);
        buildAntennaToolsRecipe(recipeOutput);
        buildAntennaAnalyzerRecipe(recipeOutput);
        buildHfCircuitBoardRecipe(recipeOutput);
        buildAntennaConnectorRecipe(recipeOutput);
        buildBalunRecipes(recipeOutput);
        buildDummyLoadRecipe(recipeOutput);
        buildVhfAntennaRecipes(recipeOutput);
        buildPowerDeviceRecipes(recipeOutput);
        buildPassthroughRecipe(recipeOutput);

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
        buildQrpRadioRecipes(recipeOutput);

        // All band radio variants (shapeless)
        buildAllBandRadioRecipes(recipeOutput);
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
                .define('K', RadiocraftItems.INSULATION.get())
                .unlockedBy("has_insulation", has(RadiocraftItems.INSULATION.get()))
                .save(recipeOutput);
    }

    /** Insulation, the mod's rubber, three ways: the better the material, the more a craft gives. */
    private void buildInsulationRecipes(RecipeOutput recipeOutput) {
        insulation(recipeOutput, Ingredient.of(Items.DRIED_KELP), Items.STRING, 6, "kelp", has(Items.DRIED_KELP));
        insulation(recipeOutput, Ingredient.of(Items.LEATHER), Items.STRING, 8, "leather", has(Items.LEATHER));
        insulation(recipeOutput, Ingredient.of(ItemTags.WOOL), Items.SLIME_BALL, 16, "slime", has(Items.SLIME_BALL));
    }

    /** {@code material} plus {@code binder} gives {@code count} insulation. */
    private void insulation(RecipeOutput recipeOutput, Ingredient material, ItemLike binder, int count, String from, Criterion<InventoryChangeTrigger.TriggerInstance> unlock) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.INSULATION.get(), count)
                .requires(material)
                .requires(binder)
                .unlockedBy("has_" + from, unlock)
                .save(recipeOutput, Radiocraft.id("insulation_from_" + from));
    }

    private void buildAntennaPoleRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_POLE.get(), 3)
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
                .define('K', RadiocraftItems.INSULATION.get())
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildFerriteCore(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.FERRITE_CORE.get(), 2)
                .pattern(" K ")
                .pattern("KIK")
                .pattern(" K ")
                .define('I', Items.IRON_INGOT)
                .define('K', Items.CLAY_BALL) // Ferrite is iron oxide fired like a ceramic.
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);
    }

    private void buildLargeBatteryRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.LARGE_BATTERY.get(), 1)
                .pattern("LSL")
                .pattern("LSL")
                .pattern("LSL")
                .define('L', ItemTags.PLANKS) // The crate.
                .define('S', RadiocraftItems.SMALL_BATTERY.get())
                .unlockedBy("has_small_battery", has(RadiocraftItems.SMALL_BATTERY.get()))
                .save(recipeOutput);
    }

    private void buildCoaxialCoreRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.COAXIAL_CORE.get(), 6)
                .pattern("KKK")
                .pattern("CCC")
                .pattern("KKK")
                .define('K', RadiocraftItems.INSULATION.get()) // The dielectric around the centre conductor.
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);
    }

    private void buildMicrophoneRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.MICROPHONE.get(), 1)
                .pattern(" E ")
                .pattern(" D ")
                .pattern(" C ")
                .define('C', RadiocraftItems.WIRE.get())
                .define('D', Items.IRON_INGOT)
                .define('E', RadiocraftItems.MICROPHONE_ELEMENT.get())
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildSolarPanelRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SOLAR_PANEL.get(), 1)
                .pattern("GGG")
                .pattern("LLL")
                .pattern("I#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('G', Items.GLASS)
                .define('L', Items.LAPIS_LAZULI) // The cells.
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildEncasedCrystalRecipes(RecipeOutput recipeOutput) {
        for(CrystalBand band : CrystalBand.values())
            for(CrystalQuality quality : CrystalQuality.values()) {
                Item crystal = RadiocraftItems.CRYSTALS.get(band).get(quality).get();
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ENCASED_CRYSTALS.get(band).get(quality).get())
                        .requires(crystal)
                        .requires(Items.IRON_INGOT)
                        .unlockedBy("has_crystal", has(crystal))
                        .save(recipeOutput);
            }
    }

    private void buildRadioSpeakerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.RADIO_SPEAKER.get(), 1)
                .pattern(" I ")
                .pattern("PAP")
                .pattern("P#P")
                .define('#', RadiocraftItems.WIRE.get())
                .define('P', ItemTags.PLANKS)
                .define('A', Items.NOTE_BLOCK)
                .define('I', Items.IRON_INGOT) // The magnet.
                .unlockedBy("has_note_block", has(Items.NOTE_BLOCK))
                .save(recipeOutput);
    }

    private void buildCoaxWireRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.COAX_WIRE.get(), 6)
                .pattern("KKK")
                .pattern("###")
                .pattern("CCC")
                .define('#', RadiocraftItems.WIRE.get()) // The braided shield.
                .define('K', RadiocraftItems.INSULATION.get()) // The jacket.
                .define('C', RadiocraftItems.COAXIAL_CORE.get())
                .unlockedBy("has_coaxial_core", has(RadiocraftItems.COAXIAL_CORE.get()))
                .save(recipeOutput);
    }

    private void buildDuplexerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.DUPLEXER.get(), 1)
                .pattern("III")
                .pattern("BBB")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET) // The tuned cavities.
                .unlockedBy("has_vhf_base_station", has(RadiocraftItems.VHF_BASE_STATION.get()))
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
                .define('C', ItemTags.COALS) // The carbon rod; charcoal does as well as coal.
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildRadioInterfaceRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.RADIO_INTERFACE.get(), 1)
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
                .pattern("WEW")
                .pattern("   ")
                .define('W', RadiocraftItems.WIRE.get())
                .define('E', RadiocraftItems.MICROPHONE_ELEMENT.get())
                .define('B', ItemTags.BUTTONS)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    /** A carbon microphone element, as vintage mics used: carbon granules behind a paper diaphragm, in a metal cup. */
    private void buildMicrophoneElementRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.MICROPHONE_ELEMENT.get(), 2)
                .pattern(" P ")
                .pattern("NCN")
                .define('P', Items.PAPER)
                .define('N', Items.IRON_NUGGET)
                .define('C', ItemTags.COALS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);
    }

    private void buildAntennaToolsRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_TUNER.get(), 1)
                .pattern("###")
                .pattern("#L#")
                .pattern("CPI")
                .define('#', RadiocraftItems.WIRE.get())
                .define('L', Items.LIGHTNING_ROD)
                .define('C', Items.COPPER_INGOT) // The roller coil.
                .define('P', Items.PAPER) // With the iron plates, the variable capacitor.
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
    }

    private void buildAntennaAnalyzerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_ANALYZER.get(), 1)
                .pattern("CGC")
                .pattern("#H#")
                .pattern("DBD")
                .define('#', RadiocraftItems.WIRE.get())
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS_PANE)
                .define('H', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .define('D', Items.DIAMOND) // A later-game tool.
                .define('B', RadiocraftItems.SMALL_BATTERY.get())
                .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildAntennaConnectorRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ANTENNA_CONNECTOR.get(), 2)
                .pattern(" # ")
                .pattern("KCK")
                .pattern(" O ")
                .define('#', RadiocraftItems.WIRE.get())
                .define('K', RadiocraftItems.INSULATION.get())
                .define('C', Items.COPPER_INGOT)
                .define('O', RadiocraftItems.COAX_WIRE.get())
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
    }

    /** A balun is wire wound on a ferrite core: the more windings, the bigger the ratio. */
    private void buildBalunRecipes(RecipeOutput recipeOutput) {
        balun(recipeOutput, RadiocraftItems.BALUN_ONE_TO_ONE.get(), 1);
        balun(recipeOutput, RadiocraftItems.BALUN_TWO_TO_ONE.get(), 2);
        balun(recipeOutput, RadiocraftItems.BALUN_FOUR_TO_ONE.get(), 3);
        balun(recipeOutput, RadiocraftItems.BALUN_NINE_TO_ONE.get(), 4);
    }

    private void balun(RecipeOutput recipeOutput, ItemLike balun, int windings) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, balun)
                .requires(RadiocraftItems.FERRITE_CORE.get())
                .requires(RadiocraftItems.COAX_WIRE.get())
                .requires(RadiocraftItems.WIRE.get(), windings)
                .unlockedBy("has_ferrite_core", has(RadiocraftItems.FERRITE_CORE.get()))
                .save(recipeOutput);
    }

    /** The VHF antennas, each laid out in its own shape. */
    private void buildVhfAntennaRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.J_POLE_ANTENNA.get(), 1)
                .pattern("  C")
                .pattern("COC")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT) // One bent tube, a J.
                .define('O', RadiocraftItems.COAX_WIRE.get())
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SLIM_JIM_ANTENNA.get(), 1)
                .pattern("#K#")
                .pattern("#K#")
                .pattern("#O#")
                .define('#', RadiocraftItems.WIRE.get()) // Twin-lead: two wires with spacers between.
                .define('K', RadiocraftItems.INSULATION.get())
                .define('O', RadiocraftItems.COAX_WIRE.get())
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.YAGI_ANTENNA.get(), 1)
                .pattern("LIL")
                .pattern("LIL")
                .pattern(" O ")
                .define('L', Items.LIGHTNING_ROD) // The elements across the boom.
                .define('I', Items.IRON_INGOT)
                .define('O', RadiocraftItems.COAX_WIRE.get())
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
    }

    private void buildDummyLoadRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.DUMMY_LOAD.get(), 1)
                .pattern(" O ")
                .pattern("ICI")
                .pattern("III")
                .define('O', RadiocraftItems.COAX_WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHARCOAL) // The carbon resistor.
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
    }

    private void buildPowerDeviceRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.DESK_CHARGER.get(), 1)
                .pattern("I I")
                .pattern("#R#")
                .pattern("SSS")
                .define('I', Items.IRON_INGOT) // The cradle.
                .define('#', RadiocraftItems.WIRE.get())
                .define('R', Items.REDSTONE)
                .define('S', Items.SMOOTH_STONE_SLAB)
                .unlockedBy("has_wire", has(RadiocraftItems.WIRE.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.CHARGE_CONTROLLER.get(), 1)
                .pattern("III")
                .pattern("#C#")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('#', RadiocraftItems.WIRE.get())
                .define('C', Items.COMPARATOR) // Regulates the charge.
                .unlockedBy("has_solar_panel", has(RadiocraftItems.SOLAR_PANEL.get()))
                .save(recipeOutput);
    }

    /** Cheap and plentiful: a thick wall needs one per block. */
    private void buildPassthroughRecipe(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.PASSTHROUGH.get(), 4)
                .requires(RadiocraftItems.WIRE.get())
                .requires(RadiocraftItems.COAX_WIRE.get())
                .requires(RadiocraftItems.INSULATION.get())
                .unlockedBy("has_coax_wire", has(RadiocraftItems.COAX_WIRE.get()))
                .save(recipeOutput);
    }

    private void buildHfCircuitBoardRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_CIRCUIT_BOARD.get(), 1)
                .pattern("SSS")
                .pattern("PRC")
                .pattern("III")
                .define('P', Items.PAPER) // Paper capacitors.
                .define('S', Items.SAND)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_TORCH) // The valve.
                .define('C', Items.COPPER_INGOT) // Tuning coils.
                .unlockedBy("has_redstone_torch", has(Items.REDSTONE_TORCH))
                .save(recipeOutput);
    }

    private void buildHfReceiverRecipe(RecipeOutput recipeOutput) {
        Item[] poorCrystals = Arrays.stream(CrystalBand.values()).filter(band -> band != CrystalBand.VHF)
                .map(band -> RadiocraftItems.ENCASED_CRYSTALS.get(band).get(CrystalQuality.POOR).get()).toArray(Item[]::new);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.HF_RECEIVER.get(), 1)
                .pattern("LII")
                .pattern("CRS")
                .pattern("I#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('R', Ingredient.of(poorCrystals)) // Any poor HF crystal: a use for the ones sand turns up.
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('C', Items.COPPER_INGOT)
                .define('L', Items.LIGHTNING_ROD) // The antenna, on top.
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildVhfReceiverRecipe(RecipeOutput recipeOutput) {
        Item crystal = RadiocraftItems.ENCASED_CRYSTALS.get(CrystalBand.VHF).get(CrystalQuality.POOR).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_RECEIVER.get(), 1)
                .pattern("W#W")
                .pattern("WCS")
                .pattern("III")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('W', ItemTags.PLANKS)
                .define('C', crystal)
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildVhfBaseStationRecipe(RecipeOutput recipeOutput) {
        crystalRadio(recipeOutput, RadiocraftItems.VHF_BASE_STATION_VARIANTS, CrystalBand.VHF, result -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .pattern("III")
                        .pattern("HRS")
                        .pattern("#WW")
                        .define('#', RadiocraftItems.WIRE.get())
                        .define('I', Items.IRON_INGOT)
                        .define('H', RadiocraftItems.HAND_MICROPHONE.get())
                        .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                        .define('W', ItemTags.PLANKS)
                        .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get())));
    }

    private void buildParrotControllerRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.PARROT_CONTROLLER.get(), 1)
                .pattern("III")
                .pattern("SPM")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('P', Items.REPEATER) // Sets the echo delay.
                .define('M', RadiocraftItems.HAND_MICROPHONE.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get()))
                .save(recipeOutput);
    }

    private void buildSolarWeatherStationRecipe(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.SOLAR_WEATHER_STATION.get(), 1)
                .pattern("AKG")
                .pattern("A##")
                .pattern("##I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('A', RadiocraftItems.ANTENNA_POLE.get())
                .define('K', Items.CLOCK)
                .define('G', Items.GLASS)
                .unlockedBy("has_antenna_pole", has(RadiocraftItems.ANTENNA_POLE.get()))
                .save(recipeOutput);
    }

    private void buildHfRadio10mRecipe(RecipeOutput recipeOutput) {
        buildHfRadioRecipe(recipeOutput, RadiocraftItems.HF_RADIO_10M_VARIANTS, CrystalBand.BAND_10M, Items.GOLD_INGOT); // Gold for the high band.
    }

    private void buildHfRadio20mRecipe(RecipeOutput recipeOutput) {
        buildHfRadioRecipe(recipeOutput, RadiocraftItems.HF_RADIO_20M_VARIANTS, CrystalBand.BAND_20M, Items.IRON_INGOT);
    }

    private void buildHfRadio40mRecipe(RecipeOutput recipeOutput) {
        buildHfRadioRecipe(recipeOutput, RadiocraftItems.HF_RADIO_40M_VARIANTS, CrystalBand.BAND_40M, Items.IRON_INGOT);
    }

    private void buildHfRadio80mRecipe(RecipeOutput recipeOutput) {
        buildHfRadioRecipe(recipeOutput, RadiocraftItems.HF_RADIO_80M_VARIANTS, CrystalBand.BAND_80M, Items.IRON_INGOT);
    }

    /** Every HF radio is built the same way: the crystal ('R') is cut for its band, and {@code base} is the bottom row. */
    private void buildHfRadioRecipe(RecipeOutput recipeOutput, CrystalVariants<Item, ? extends Item> radio, CrystalBand band, ItemLike base) {
        crystalRadio(recipeOutput, radio, band, result -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .pattern("BHS")
                        .pattern("#RT")
                        .pattern("III")
                        .define('#', RadiocraftItems.WIRE.get())
                        .define('H', RadiocraftItems.HAND_MICROPHONE.get())
                        .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                        .define('T', Items.TINTED_GLASS)
                        .define('I', base)
                        .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                        .unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get())));
    }

    /**
     * The all-band circuit board holds a pure crystal for every band on an HF circuit board; the all-band radio is
     * then built like the other HF rigs with the board where their crystal goes.
     */
    private void buildAllBandRadioRecipes(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder board = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get(), 1)
                .requires(RadiocraftItems.HF_CIRCUIT_BOARD.get());
        for(CrystalBand band : CrystalBand.values())
            board.requires(RadiocraftItems.ENCASED_CRYSTALS.get(band).get(CrystalQuality.PURE).get());
        board.unlockedBy("has_hf_circuit_board", has(RadiocraftItems.HF_CIRCUIT_BOARD.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.ALL_BAND_RADIO.get(), 1)
                .pattern("BHS")
                .pattern("#RT")
                .pattern("III")
                .define('#', RadiocraftItems.WIRE.get())
                .define('H', RadiocraftItems.HAND_MICROPHONE.get())
                .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                .define('T', Items.TINTED_GLASS)
                .define('I', Items.IRON_INGOT)
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .define('R', RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get())
                .unlockedBy("has_all_band_circuit_board", has(RadiocraftItems.ALL_BAND_CIRCUIT_BOARD.get()))
                .save(recipeOutput);
    }

    private void buildVhfRepeaterRecipe(RecipeOutput recipeOutput) {
        Item crystal = RadiocraftItems.ENCASED_CRYSTALS.get(CrystalBand.VHF).get(CrystalQuality.PURE).get(); // Repeaters only take pure crystals.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RadiocraftItems.VHF_REPEATER.get(), 1)
                .pattern("ITI")
                .pattern("CBR")
                .pattern("I#I")
                .define('#', RadiocraftItems.WIRE.get())
                .define('I', Items.IRON_INGOT)
                .define('R', crystal)
                .define('B', variants(RadiocraftItems.VHF_BASE_STATION_VARIANTS))
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.IRON_TRAPDOOR)
                .unlockedBy("has_vhf_base_station", has(RadiocraftItems.VHF_BASE_STATION.get()))
                .unlockedBy("has_encased_crystal", has(crystal))
                .save(recipeOutput);
    }

    /** QRP rigs are built around poor crystals, and only take those. */
    private void buildQrpRadioRecipes(RecipeOutput recipeOutput) {
        buildQrpRadioRecipe(recipeOutput, RadiocraftItems.QRP_RADIO_20M.get(), CrystalBand.BAND_20M);
        buildQrpRadioRecipe(recipeOutput, RadiocraftItems.QRP_RADIO_40M.get(), CrystalBand.BAND_40M);
    }

    private void buildQrpRadioRecipe(RecipeOutput recipeOutput, ItemLike radio, CrystalBand band) {
        Item crystal = RadiocraftItems.ENCASED_CRYSTALS.get(band).get(CrystalQuality.POOR).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, radio, 1)
                .pattern("CBC")
                .pattern("#R#")
                .pattern("WWW")
                .define('#', RadiocraftItems.WIRE.get())
                .define('C', Items.COPPER_INGOT)
                .define('W', ItemTags.PLANKS) // A wooden case: lighter than the full HF radios.
                .define('B', RadiocraftItems.HF_CIRCUIT_BOARD.get())
                .define('R', crystal)
                .unlockedBy("has_encased_crystal", has(crystal))
                .save(recipeOutput);
    }

    /**
     * {@code recipe} once for each crystal quality: the shaped recipe for the radio of that quality, with the encased
     * crystal for {@code band} as 'R'. The good radio keeps the plain recipe id. Also adds the crystal swap: any other
     * quality of the radio plus an encased crystal gives the radio of that crystal's quality.
     */
    private void crystalRadio(RecipeOutput recipeOutput, CrystalVariants<Item, ? extends Item> radio, CrystalBand band, Function<ItemLike, ShapedRecipeBuilder> recipe) {
        for(CrystalQuality quality : CrystalQuality.values()) {
            Item result = radio.get(quality).get();
            Item crystal = RadiocraftItems.ENCASED_CRYSTALS.get(band).get(quality).get();
            recipe.apply(result)
                    .define('R', crystal)
                    .unlockedBy("has_encased_crystal", has(crystal))
                    .save(recipeOutput);

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
                    .requires(Ingredient.of(radio.values().filter(other -> other != result).toArray(Item[]::new)))
                    .requires(crystal)
                    .unlockedBy("has_encased_crystal", has(crystal))
                    .save(recipeOutput, Radiocraft.id(BuiltInRegistries.ITEM.getKey(result).getPath() + "_from_crystal_swap"));
        }
    }

    /** Any crystal quality of a radio. */
    private static Ingredient variants(CrystalVariants<Item, ? extends Item> radio) {
        return Ingredient.of(radio.values().toArray(Item[]::new));
    }

    private void buildVhfHandheldRecipe(RecipeOutput recipeOutput) {
        crystalRadio(recipeOutput, RadiocraftItems.VHF_HANDHELD_VARIANTS, CrystalBand.VHF, result -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .pattern("L  ")
                        .pattern("MRS")
                        .pattern("BBB")
                        .define('S', RadiocraftItems.RADIO_SPEAKER.get())
                        .define('L', Items.LIGHTNING_ROD)
                        .define('M', RadiocraftItems.HAND_MICROPHONE.get())
                        .define('B', ItemTags.BUTTONS)
                        .unlockedBy("has_radio_speaker", has(RadiocraftItems.RADIO_SPEAKER.get())));
    }




}

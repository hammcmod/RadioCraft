package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.crafting.H19ScreenGlassIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Registry class for Radiocraft's custom recipe ingredients. */
public class RadiocraftIngredientTypes {

	public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Radiocraft.MOD_ID);

	public static final Supplier<IngredientType<H19ScreenGlassIngredient>> H19_SCREEN_GLASS = INGREDIENT_TYPES.register("h19_screen_glass",
			() -> new IngredientType<>(H19ScreenGlassIngredient.CODEC, H19ScreenGlassIngredient.STREAM_CODEC));
}

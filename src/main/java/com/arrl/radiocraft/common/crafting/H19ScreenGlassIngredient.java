package com.arrl.radiocraft.common.crafting;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftIngredientTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.List;
import java.util.stream.Stream;

/**
 * The screen of the H19 APRS terminal in its recipe: tinted glass, or any vanilla stained glass as well on a server
 * with {@code allow_stained_glass_for_h19_aprs} on. Checked when the grid is matched, so the setting applies without a
 * datapack reload (the server config also reaches clients, so the client's recipe preview agrees).
 */
public final class H19ScreenGlassIngredient implements ICustomIngredient {

    public static final H19ScreenGlassIngredient INSTANCE = new H19ScreenGlassIngredient();
    public static final MapCodec<H19ScreenGlassIngredient> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, H19ScreenGlassIngredient> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private static List<Item> stainedGlass = null;

    private H19ScreenGlassIngredient() {}

    /** The sixteen vanilla stained glass blocks (not panes). */
    private static List<Item> stainedGlass() {
        if(stainedGlass == null) {
            stainedGlass = BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(entry -> {
                        ResourceLocation id = entry.getKey().location();
                        return id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) && id.getPath().endsWith("_stained_glass");
                    })
                    .map(entry -> entry.getValue())
                    .toList();
        }
        return stainedGlass;
    }

    /** True when the server allows stained glass; false while the config is not loaded (datagen, main menu). */
    private static boolean stainedAllowed() {
        return RadiocraftServerConfig.SPEC.isLoaded() && RadiocraftServerConfig.APRS_H19_STAINED_GLASS.get();
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(Items.TINTED_GLASS) || (stainedAllowed() && stainedGlass().contains(stack.getItem()));
    }

    @Override
    public Stream<ItemStack> getItems() {
        Stream<ItemStack> tinted = Stream.of(new ItemStack(Items.TINTED_GLASS));
        return stainedAllowed() ? Stream.concat(tinted, stainedGlass().stream().map(ItemStack::new)) : tinted;
    }

    @Override
    public boolean isSimple() {
        return false; // The accepted items depend on the server config.
    }

    @Override
    public IngredientType<?> getType() {
        return RadiocraftIngredientTypes.H19_SCREEN_GLASS.get();
    }
}

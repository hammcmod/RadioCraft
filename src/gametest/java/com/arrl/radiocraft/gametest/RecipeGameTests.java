package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Recipes that depend on the server config. Needs the generated recipes ({@code ./gradlew runData}). */
@GameTestHolder(Radiocraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeGameTests {

    /** What a crafting grid holding the H19's recipe with {@code screen} as its screen crafts, empty if nothing. */
    private static ItemStack craftH19(GameTestHelper helper, Item screen) {
        Item i = Items.IRON_INGOT, b = RadiocraftItems.HF_CIRCUIT_BOARD.get(), u = Items.STONE_BUTTON, w = RadiocraftItems.WIRE.get();
        List<ItemStack> grid = List.of(i, screen, i, i, b, i, u, w, u).stream().map(ItemStack::new).toList();
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** The H19 takes tinted glass; stained glass only with {@code allow_stained_glass_for_h19_aprs} on. */
    @GameTest(template = GameTestUtil.EMPTY, timeoutTicks = 20)
    public static void h19TakesStainedGlassOnlyWhenAllowed(GameTestHelper helper) {
        boolean before = RadiocraftServerConfig.APRS_H19_STAINED_GLASS.get();
        try {
            RadiocraftServerConfig.APRS_H19_STAINED_GLASS.set(false);
            helper.assertTrue(craftH19(helper, Items.TINTED_GLASS).is(RadiocraftItems.APRS_TERMINAL.get()), "Tinted glass does not make an H19");
            helper.assertTrue(craftH19(helper, Items.BLUE_STAINED_GLASS).isEmpty(), "Stained glass makes an H19 with the setting off");
            RadiocraftServerConfig.APRS_H19_STAINED_GLASS.set(true);
            helper.assertTrue(craftH19(helper, Items.BLUE_STAINED_GLASS).is(RadiocraftItems.APRS_TERMINAL.get()), "Stained glass does not make an H19 with the setting on");
            helper.assertTrue(craftH19(helper, Items.GLASS).isEmpty(), "Plain glass makes an H19");
        }
        finally {
            RadiocraftServerConfig.APRS_H19_STAINED_GLASS.set(before);
        }
        helper.succeed();
    }
}

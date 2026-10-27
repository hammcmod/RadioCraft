package com.arrl.radiocraft.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Curios support, safe to call whether or not Curios is installed: a VHF handheld (or a spare small battery) can be
 * worn in the belt slot. A worn handheld receives like one in the inventory, drains its battery, and talks with the
 * "talk on worn radio" key. Nothing here touches a Curios class; {@link CuriosIntegration} does, and only runs when
 * Curios is loaded. The slot itself comes from data: {@code data/radiocraft/curios/entities} gives players the belt
 * slot and {@code data/curios/tags/item/belt} lists what fits.
 */
public final class CuriosCompat {

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Lets the handheld be worn and tick there. Call once, at common setup. */
    public static void register() {
        if(LOADED)
            CuriosIntegration.register();
    }

    /** The HF manpacks a player wears on the back slot; empty without Curios. */
    public static List<ItemStack> wornManpacks(Player player) {
        return LOADED ? CuriosIntegration.wornManpacks(player) : List.of();
    }

    /** The VHF handhelds a player wears, in slot order; empty without Curios. */
    public static List<ItemStack> wornHandhelds(Player player) {
        return LOADED ? CuriosIntegration.wornHandhelds(player) : List.of();
    }
}

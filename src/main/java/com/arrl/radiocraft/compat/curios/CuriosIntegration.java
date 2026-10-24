package com.arrl.radiocraft.compat.curios;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/** The Curios side of {@link CuriosCompat}; only loaded when Curios is installed. */
final class CuriosIntegration {

    private CuriosIntegration() {}

    /** A worn handheld ticks as it would in the inventory, so its battery drains while it is switched on. */
    private static final ICurioItem WORN_HANDHELD = new ICurioItem() {
        @Override
        public void curioTick(SlotContext slotContext, ItemStack stack) {
            stack.getItem().inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), -1, false);
        }
    };

    static void register() {
        RadiocraftItems.VHF_HANDHELD_VARIANTS.values().forEach(item -> CuriosApi.registerCurio(item, WORN_HANDHELD));
    }

    static List<ItemStack> wornHandhelds(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof VHFHandheldItem).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}

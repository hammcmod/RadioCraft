package com.arrl.radiocraft.client.events;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DevItemRenderer {

    public static void renderItemWithDevBackground(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (ItemRenderEvents.isDevItem(itemStack)) {
            // Render the development background first
            ItemRenderEvents.renderDevBackground(guiGraphics, x, y);
        }

        // Then render the normal item
        guiGraphics.renderItem(itemStack, x, y);
    }
}
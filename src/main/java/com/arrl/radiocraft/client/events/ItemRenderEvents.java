
package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Supplier;

public class ItemRenderEvents {

    private static final ResourceLocation DEV_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, "textures/gui/dev_item_background.png");

    /** Items that are only in the creative tab in the development environment; they get the blue "dev" tile. */
    private static final List<Supplier<? extends Item>> DEV_ITEMS = List.of(); // Everything is released at the moment.

    // Lazily initialized set of actual items
    private static Set<Item> devItems = null;

    private static Set<Item> getDevItems() {
        if (devItems == null) {
            Set<Item> items = new HashSet<>();
            for (Supplier<? extends Item> holder : DEV_ITEMS) {
                try {
                    Item item = holder.get();
                    if (item != null)
                        items.add(item);
                } catch (Exception e) {
                    // During data generation, items might not be bound yet
                }
            }
            devItems = items;
        }
        return devItems;
    }

    public static boolean isDevItem(ItemStack itemStack) {
        return Radiocraft.IS_DEVELOPMENT_ENV && getDevItems().contains(itemStack.getItem());
    }

    public static void renderDevBackground(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F); // Semi-transparent
        guiGraphics.blit(DEV_BACKGROUND_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
    }
}
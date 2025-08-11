package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

@EventBusSubscriber(modid = Radiocraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class InventoryRenderEvents {

    @SubscribeEvent
    public static void onInventoryRender(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();

        // Iterate through all slots and render backgrounds for dev items
        for (Slot slot : screen.getMenu().slots) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty() && ItemRenderEvents.isDevItem(itemStack)) {
                int slotX = slot.x;
                int slotY = slot.y;

                // Render the development background behind the item
                ItemRenderEvents.renderDevBackground(event.getGuiGraphics(), slotX, slotY);
            }
        }
    }
}
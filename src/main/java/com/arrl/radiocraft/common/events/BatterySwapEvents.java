package com.arrl.radiocraft.common.events;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;

/**
 * Handles battery swap functionality for creative mode.
 * In survival mode, Item.overrideStackedOnOther() is called.
 * In creative mode, only this event is fired.
 */
@EventBusSubscriber
public class BatterySwapEvents {

    /**
     * Handles battery swap in creative mode.
     * This event fires before Item.overrideStackedOnOther() and works in both modes,
     * but overrideStackedOnOther() doesn't fire in creative, so we need this.
     */
    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        // Only handle in creative mode (survival uses overrideStackedOnOther)
        if (!event.getPlayer().isCreative()) {
            return;
        }
        
        // Only handle left-click (PRIMARY)
        if (event.getClickAction() != ClickAction.PRIMARY) {
            return;
        }
        
        ItemStack carried = event.getCarriedItem();
        ItemStack slotItem = event.getStackedOnItem();
        
        // Check if we're swapping battery and radio
        boolean batteryOnRadio = carried.getItem() == RadiocraftItems.SMALL_BATTERY.get() && 
                                 slotItem.getItem() == RadiocraftItems.VHF_HANDHELD.get();
        boolean radioOnBattery = carried.getItem() == RadiocraftItems.VHF_HANDHELD.get() && 
                                 slotItem.getItem() == RadiocraftItems.SMALL_BATTERY.get();
        
        if (batteryOnRadio || radioOnBattery) {
            // Determine which is radio and which is battery
            ItemStack radio = batteryOnRadio ? slotItem : carried;
            ItemStack battery = batteryOnRadio ? carried : slotItem;
            
            // In creative mode, the event only fires client-side
            VHFHandheldItem.swapBatteryEnergy(radio, battery, event.getPlayer());
            
            // Cancel the event to prevent default behavior
            event.setCanceled(true);
        }
    }
}

package com.arrl.radiocraft.common.events;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;

/**
 * Handles battery swap functionality when a player stacks a battery item on top of a radio in an
 * inventory slot. In survival mode the swap is handled by the item's
 * {@link net.minecraft.world.item.Item#overrideStackedOnOther(ItemStack, net.minecraft.world.inventory.Slot, net.minecraft.world.inventory.ClickAction, net.minecraft.world.entity.player.Player)}
 * hook (which runs on the server). In creative mode that hook is not invoked the same way,
 * and the {@link net.neoforged.neoforge.event.ItemStackedOnOtherEvent} is fired instead (client-side),
 * so we need this subscriber to ensure the behavior is consistent in both modes.
 *
 * Adventure mode behaves like survival for this interaction; the same overrideStackedOnOther() path
 * will be used there, so no special handling is required.
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

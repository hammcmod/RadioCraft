package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

public class SmallBatteryItem extends Item {

    public SmallBatteryItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when this battery (in cursor) is clicked on another item in a slot.
     * Handles battery swap when clicking on a VHF Handheld Radio.
     *
     * In survival (and adventure) mode this logic is invoked via
     * {@link net.minecraft.world.item.Item#overrideStackedOnOther(ItemStack, net.minecraft.world.inventory.Slot, net.minecraft.world.inventory.ClickAction, net.minecraft.world.entity.player.Player)}.
     * In creative mode the {@link net.neoforged.neoforge.event.ItemStackedOnOtherEvent} is fired instead;
     * see {@link com.arrl.radiocraft.common.events.BatterySwapEvents} which handles that case.
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack battery, Slot slot, ClickAction action, Player player) {
        ItemStack slotStack = slot.getItem();
        
        if (action == ClickAction.PRIMARY && !slotStack.isEmpty() && 
            slotStack.getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            
            if (!player.level().isClientSide()) {
                VHFHandheldItem.swapBatteryEnergy(slotStack, battery, player);
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    private @Nullable IEnergyStorage getCapability(ItemStack stack) {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IEnergyStorage cap = getCapability(stack);
        if (cap != null) {
            return Math.round(((float) cap.getEnergyStored() / cap.getMaxEnergyStored()) * 13.0f);
        } else {
            return 0;
        }
    }

    @Override
    public int getBarColor(ItemStack stack) {
        IEnergyStorage cap = getCapability(stack);
        if (cap != null) {
            float charge = (float) cap.getEnergyStored() / cap.getMaxEnergyStored();
            float f = Math.max(0.0f, charge);
            return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
        } else {
            return Mth.hsvToRgb(0.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        IEnergyStorage energyStorage = getCapability(stack);
        if (energyStorage != null) {
            int storedFE = energyStorage.getEnergyStored();
            int maxFE = energyStorage.getMaxEnergyStored();
            
            // Convert FE to Joules (2.5 FE = 1J)
            double storedJoules = storedFE / 2.5;
            double maxJoules = maxFE / 2.5;
            
            // Calculate percentage
            double percentage = maxFE > 0 ? (double) storedFE / maxFE * 100.0 : 0.0;
            
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.energy_stored_joules", 
                Math.round(storedJoules), Math.round(maxJoules)));
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.battery_percentage", 
                String.format("%.1f", percentage)));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.energy_stored_joules", 0, "N/A"));
            tooltipComponents.add(Component.translatable("tooltip.radiocraft.battery_percentage", "0.0"));
        }
        
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.small_battery"));
    }
}

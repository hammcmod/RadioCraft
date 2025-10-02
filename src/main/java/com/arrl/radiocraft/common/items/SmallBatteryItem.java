package com.arrl.radiocraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

import javax.annotation.Nullable;

public class SmallBatteryItem extends Item {

    public SmallBatteryItem(Properties properties) {
        super(properties);
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

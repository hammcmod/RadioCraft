package com.arrl.radiocraft.common.items;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

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
            return Math.round(((float) cap.getEnergyStored() /cap.getMaxEnergyStored()) * 13.0f);
        } else {
            return 0;
        }


        //DataComponent.EnergyRecord record = (DataComponent.EnergyRecord) stack.getComponents().getOrDefault(DataComponent.ENERGY_DATA_COMPONENT.get(), 0.0);
        //int charge = (int) record.energy();
        //return Math.round(charge * 13.0F / CommonConfig.SMALL_BATTERY_CAPACITY.get());
        //return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {

        IEnergyStorage cap = getCapability(stack);
        if (cap != null) {
            float charge = (float) cap.getEnergyStored() /cap.getMaxEnergyStored();
            float f = Math.max(0.0f, charge);
            return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
        } else {
            return Mth.hsvToRgb(0.0f, 1.0f, 1.0f);
        }


        /*DataComponent.EnergyRecord record = (DataComponent.EnergyRecord) stack.getComponents().getOrDefault(DataComponent.ENERGY_DATA_COMPONENT.get(), 0.0);
        int charge = (int) record.energy();
        float capacity = CommonConfig.SMALL_BATTERY_CAPACITY.get();

        float f = Math.max(0.0F, charge / capacity);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);*/
        //return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

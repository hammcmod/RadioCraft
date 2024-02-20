package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SmallBatteryItem extends Item {

    public SmallBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        int charge = nbt.contains("charge") ? nbt.getInt("charge") : 0;

        return Math.round(charge * 13.0F / RadiocraftCommonConfig.SMALL_BATTERY_CAPACITY.get());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        int charge = nbt.contains("charge") ? nbt.getInt("charge") : 0;
        float capacity = RadiocraftCommonConfig.SMALL_BATTERY_CAPACITY.get();

        float f = Math.max(0.0F, charge / capacity);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

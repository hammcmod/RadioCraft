package com.arrl.radiocraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
        //DataComponent.EnergyRecord record = (DataComponent.EnergyRecord) stack.getComponents().getOrDefault(DataComponent.ENERGY_DATA_COMPONENT.get(), 0.0);
        //int charge = (int) record.energy();
        //return Math.round(charge * 13.0F / CommonConfig.SMALL_BATTERY_CAPACITY.get());
        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        /*DataComponent.EnergyRecord record = (DataComponent.EnergyRecord) stack.getComponents().getOrDefault(DataComponent.ENERGY_DATA_COMPONENT.get(), 0.0);
        int charge = (int) record.energy();
        float capacity = CommonConfig.SMALL_BATTERY_CAPACITY.get();

        float f = Math.max(0.0F, charge / capacity);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);*/
        return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.radiocraft.not_implemented"));
    }
}

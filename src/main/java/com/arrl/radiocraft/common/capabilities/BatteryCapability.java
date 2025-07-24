package com.arrl.radiocraft.common.capabilities;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryCapability implements IEnergyStorage {

    int energyStored;
    ItemStack stack;

    public BatteryCapability(ItemStack stack) {

        this.stack = stack;

        this.stack.setDamageValue(0);

        energyStored = 0;
    }

    private void setEnergy(int energy) {
        //energyStored = energy;
        stack.setDamageValue(energy);
    }

    private int getEnergy() {
        //return energyStored;
        return stack.getDamageValue();
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        Radiocraft.LOGGER.info("EnergyStored" + getEnergy());

        int canGet = Math.min(toReceive, getMaxEnergyStored() - getEnergy());

        if (!simulate) {
            setEnergy(getEnergy() + canGet);
        }

        Radiocraft.LOGGER.info("canGet" + canGet);
        Radiocraft.LOGGER.info("EnergyStored receve" + getEnergy());
        return canGet;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        Radiocraft.LOGGER.info("EnergyStored" + getEnergy());

        int canPut = Math.max(getEnergy() - toExtract, 0);

        if (!simulate) {
            setEnergy(getEnergy() - toExtract);
        }

        Radiocraft.LOGGER.info("canPut" + canPut);
        Radiocraft.LOGGER.info("EnergyStored extract" + getEnergy());
        return canPut;
    }

    @Override
    public int getEnergyStored() {
        return getEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return CommonConfig.SMALL_BATTERY_CAPACITY.get();
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}

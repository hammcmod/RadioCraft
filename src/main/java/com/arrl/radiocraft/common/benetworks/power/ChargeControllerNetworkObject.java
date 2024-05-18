package com.arrl.radiocraft.common.benetworks.power;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ChargeControllerNetworkObject extends PowerNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.location("charge_controller");

    public boolean isEnabled;
    private int lastPowerTick = 0;

    public ChargeControllerNetworkObject(Level level, BlockPos pos) {
        this(level, pos, false);
    }

    public ChargeControllerNetworkObject(Level level, BlockPos pos, boolean isEnabled) {
        super(level, pos,
                CommonConfig.CHARGE_CONTROLLER_TICK.get() + CommonConfig.CHARGE_CONTROLLER_BATTERY_CHARGE.get(),
                CommonConfig.CHARGE_CONTROLLER_TICK.get(),
                CommonConfig.CHARGE_CONTROLLER_TICK.get());
        // The max capacity can both supply power to batteries for a tick AND charge a small battery, leaves excess to
        // allow battery charging to be handled by the BE.
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean isIndirectConsumer() {
        return false;
    }

    @Override
    public boolean isDirectConsumer() {
        return true;
    }

    @Override
    public void generate(Level level, BlockPos pos) {
        int toPush = Math.min(energyStorage.getMaxExtract(), energyStorage.getEnergyStored());
        int pushed = 0;

        for(BENetwork n : networks.values()) {
            if(n instanceof PowerBENetwork network) {
                int amountPushed = network.pushPower(toPush, false, true,false);
                toPush -= amountPushed;
                pushed += amountPushed;
            }
            if(toPush <= 0)
                break;
        }
        lastPowerTick = pushed;
        energyStorage.setEnergy(energyStorage.getEnergyStored() - pushed);
    }

    public int getLastPowerTick() {
        return lastPowerTick;
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean("isEnabled", isEnabled);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        isEnabled = nbt.getBoolean("isEnabled");
    }

}

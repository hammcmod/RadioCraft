package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class BatteryNetworkObject extends PowerNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.location("battery");

    public BatteryNetworkObject(Level level, BlockPos pos, int capacity, int maxReceive, int maxExtract) {
        super(level, pos, capacity, maxReceive, maxExtract);
    }

    @Override
    public boolean isIndirectConsumer() {
        return true;
    }

    @Override
    public boolean isDirectConsumer() {
        return false;
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        int toPush = Math.min(energyStorage.getMaxExtract(), energyStorage.getEnergyStored());
        int pushed = 0;

        for(BENetwork n : networks.values()) {
            if(n instanceof PowerBENetwork network) {
                int amountPushed = network.pushPower(toPush, false, false,false);
                toPush -= amountPushed;
                pushed += amountPushed;
            }
            if(toPush <= 0)
                break;
        }

        energyStorage.setEnergy(energyStorage.getEnergyStored() - pushed);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

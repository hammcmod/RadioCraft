package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BatteryNetworkObject extends PowerNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("battery");

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

        // The charge is saved by the block entity (networks are not), so tell it whenever the charge has moved,
        // whether we pushed it out or a charge controller pushed it in.
        if(energyStorage.getEnergyStored() != lastSeenEnergy) {
            lastSeenEnergy = energyStorage.getEnergyStored();
            BlockEntity be = level.getBlockEntity(pos);
            if(be instanceof LargeBatteryBlockEntity battery)
                battery.rememberEnergy(lastSeenEnergy);
            else if(be != null)
                be.setChanged();
        }
    }

    private int lastSeenEnergy = -1;

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

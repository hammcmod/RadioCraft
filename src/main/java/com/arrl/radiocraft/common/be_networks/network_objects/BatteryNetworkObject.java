package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.common.be_networks.IPassThroughNetworkObject;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A large battery. Its terminals are one circuit: every power wire touching it, on any side, is on one network
 * ({@link IPassThroughNetworkObject}), so a load wired to one face of a bank draws on every battery in the bank, not
 * just the one it touches (reported 2026-10-28: radios wired to the bank's end battery went dark when that one ran
 * flat while the rest were charged).
 */
public class BatteryNetworkObject extends PowerNetworkObject implements IPassThroughNetworkObject {

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
        // The batteries on a wire discharge together, each an even share: the first to tick does it for them all.
        for(BENetwork n : networks.values())
            if(n instanceof PowerBENetwork network)
                network.dischargeBatteries(level.getGameTime());

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

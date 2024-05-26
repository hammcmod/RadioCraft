package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PowerBENetwork extends BENetwork {

    public static final ResourceLocation TYPE = Radiocraft.location("power");

    public PowerBENetwork(UUID uuid) {
        super(uuid);
    }

    public PowerBENetwork() {
        super(UUID.randomUUID());
    }

    /**
     * Attempts to pull power from the network.
     *
     * @param simulate If true, do not actually extract energy from providers
     *
     * @return Amount pulled from network
     */
    public int pullPower(int amount, boolean simulate) {
        int pulled = 0;

        for(BENetworkObject obj : networkObjects) {
            pulled += ((PowerNetworkObject)obj).getStorage().extractEnergy(amount - pulled, simulate);

            if(pulled >= amount) // Stop checking if required amount is reached
                return pulled;
        }
        return pulled;
    }

    /**
     * Attempts to push power to {@link PowerNetworkObject}s on the network.
     *
     * @param simulate If true, do not actually push energy to providers
     * @param forBatteries If true, only push power to batteries. Otherwise, only push power to non-batteries.
     *
     * @return Amount pushed to network.
     */
    public int pushPower(int amount, boolean direct, boolean forBatteries, boolean simulate) {
        int pushed = 0;

        for(BENetworkObject o : networkObjects) {
            PowerNetworkObject obj = (PowerNetworkObject)o;
            if((!forBatteries && (!(obj instanceof BatteryNetworkObject))) || (forBatteries && (obj instanceof BatteryNetworkObject))) {
                if((!direct && obj.isIndirectConsumer()) || (direct && obj.isDirectConsumer())) {
                    pushed += obj.getStorage().receiveEnergy(amount - pushed, simulate);

                    if(pushed >= amount) // Stop checking if required amount is reached
                        return pushed;
                }
            }
        }
        return pushed;
    }

    @Override
    public void add(BENetworkObject networkObject) {
        if(networkObject instanceof PowerNetworkObject)
            super.add(networkObject);
        else
            Radiocraft.LOGGER.warn("Tried to add a non PowerNetworkObject to a PowerBENetwork.");
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

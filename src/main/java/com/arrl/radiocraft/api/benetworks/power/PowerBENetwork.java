package com.arrl.radiocraft.api.benetworks.power;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;

import java.util.UUID;

public class PowerBENetwork extends BENetwork {

    public PowerBENetwork(UUID uuid) {
        super(uuid);
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
     *
     * @return Amount pushed to network.
     */
    public int pushPower(int amount, boolean direct, boolean simulate) {
        int pushed = 0;

        for(BENetworkObject o : networkObjects) {
            PowerNetworkObject obj = (PowerNetworkObject)o;
            if(!direct && obj.isIndirectConsumer() || (direct && obj.isDirectConsumer())) {
                pushed += obj.getStorage().receiveEnergy(amount - pushed, simulate);

                if(pushed >= amount) // Stop checking if required amount is reached
                    return pushed;
            }
        }
        return pushed;
    }

    @Override
    public void add(BENetworkObject networkObject) {
        if(networkObject instanceof PowerNetworkObject)
            networkObjects.add(networkObject);
        else
            Radiocraft.LOGGER.warn("Tried to add a non PowerNetworkObject to a PowerBENetwork.");
    }

}

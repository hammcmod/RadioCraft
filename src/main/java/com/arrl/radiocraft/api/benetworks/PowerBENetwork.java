package com.arrl.radiocraft.api.benetworks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a network of compatible power sources and sinks. Power is shared out evenly: everything that can take
 * some gets the same share (one that fills up passes the rest of its share on), and batteries discharge together,
 * as parallel batteries do, rather than one after another in whatever order the network lists them.
 */
public class PowerBENetwork extends BENetwork {

    /**
     * The type of network.
     */
    public static final ResourceLocation TYPE = Radiocraft.id("power");

    /**
     * Creates a new {@link PowerBENetwork} with a specified UUID.
     * @param uuid The UUID of the network.
     */
    public PowerBENetwork(UUID uuid) {
        super(uuid);
    }

    /**
     * Creates a new {@link PowerBENetwork} with a random UUID.
     */
    public PowerBENetwork() {
        super(UUID.randomUUID());
    }

    /**
     * Attempts to pull power from the network.
     *
     * @param amount Amount of power to pull.
     * @param simulate If true, do not actually extract energy from providers
     *
     * @return Amount pulled from network
     */
    public int pullPower(int amount, boolean simulate) {
        List<PowerNetworkObject> sources = new ArrayList<>();
        for(BENetworkObject obj : networkObjects)
            sources.add((PowerNetworkObject)obj);
        return pullEvenly(sources, amount, simulate);
    }

    /** Game time the batteries on this network last discharged, so they do it once a tick, together. */
    private long dischargedAt = Long.MIN_VALUE;

    /**
     * The batteries on this network discharge into what draws from them (indirect consumers), once a tick: the
     * first battery to tick does it for all of them, each giving an even share of what is drawn.
     *
     * @return The energy drawn from the batteries.
     */
    public int dischargeBatteries(long gameTime) {
        if(dischargedAt == gameTime)
            return 0;
        dischargedAt = gameTime;
        List<PowerNetworkObject> batteries = new ArrayList<>();
        long available = 0;
        for(BENetworkObject o : networkObjects)
            if(o instanceof BatteryNetworkObject battery) {
                batteries.add(battery);
                available += battery.getStorage().extractEnergy(Integer.MAX_VALUE, true);
            }
        if(available <= 0)
            return 0;
        int drawn = pushPower((int)Math.min(Integer.MAX_VALUE, available), false, false, false);
        return pullEvenly(batteries, drawn, false);
    }

    /** Takes {@code amount} from {@code sources}, an even share from each (one that runs short leaves the rest to the others). */
    private static int pullEvenly(List<PowerNetworkObject> sources, int amount, boolean simulate) {
        if(amount <= 0 || sources.isEmpty())
            return 0;
        int[] room = new int[sources.size()];
        for(int i = 0; i < room.length; i++)
            room[i] = sources.get(i).getStorage().extractEnergy(Integer.MAX_VALUE, true);
        int[] share = PowerSharing.shareEvenly(amount, room);
        int pulled = 0;
        for(int i = 0; i < share.length; i++)
            if(share[i] > 0)
                pulled += sources.get(i).getStorage().extractEnergy(share[i], simulate);
        return pulled;
    }


    /**
     * Attempts to push power to {@link PowerNetworkObject}s on the network.
     *
     * @param amount Amount of power to push.
     * @param direct If true, push power directly to consumers. Otherwise, push power indirectly to consumers.
     * @param simulate If true, do not actually push energy to providers
     * @param forBatteries If true, only push power to batteries. Otherwise, only push power to non-batteries.
     *
     * @return Amount pushed to network.
     */
    public int pushPower(int amount, boolean direct, boolean forBatteries, boolean simulate) {
        if(amount <= 0)
            return 0;
        List<PowerNetworkObject> takers = new ArrayList<>();
        for(BENetworkObject o : networkObjects) {
            PowerNetworkObject obj = (PowerNetworkObject)o;
            if((forBatteries && obj instanceof BatteryNetworkObject) || (!forBatteries && !(obj instanceof BatteryNetworkObject))) {
                if((!direct && obj.isIndirectConsumer()) || (direct && obj.isDirectConsumer()))
                    takers.add(obj);
            }
        }
        // Every taker gets an even share (batteries charging in parallel each charge slower, together), not first come.
        int[] room = new int[takers.size()];
        for(int i = 0; i < room.length; i++)
            room[i] = takers.get(i).getStorage().receiveEnergy(Integer.MAX_VALUE, true);
        int[] share = PowerSharing.shareEvenly(amount, room);
        int pushed = 0;
        for(int i = 0; i < share.length; i++)
            if(share[i] > 0)
                pushed += takers.get(i).getStorage().receiveEnergy(share[i], simulate);
        return pushed;
    }

    /**
     * Adds a {@link BENetworkObject} to this {@link BENetwork}.
     * @param networkObject The {@link BENetworkObject} to add.
     */
    @Override
    public void add(BENetworkObject networkObject) {
        if(networkObject instanceof PowerNetworkObject)
            super.add(networkObject);
        else
            Radiocraft.LOGGER.warn("Tried to add a non PowerNetworkObject to a PowerBENetwork.");
    }

    /**
     * Gets the {@link ResourceLocation} of this {@link BENetwork}.
     * @return The {@link ResourceLocation} of this {@link BENetwork}.
     */
    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}

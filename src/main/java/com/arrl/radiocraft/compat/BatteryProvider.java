package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.BatteryNetworkObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.HashSet;
import java.util.Set;

/** Jade lines for a battery: its own charge, and the charge of every battery on the wire it is on. */
public enum BatteryProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("energy"))
            return;
        tooltip.add(Component.literal("Charge: " + charge(data.getInt("energy"), data.getInt("max"))));
        int count = data.getInt("count");
        if(count > 1)
            tooltip.add(Component.literal("Network: " + count + " batteries, " + charge(data.getInt("networkEnergy"), data.getInt("networkMax"))));
        else
            tooltip.add(Component.literal("Network: this is the only battery"));
    }

    private static String charge(int energy, int max) {
        int percent = max > 0 ? (int)Math.round(100.0D * energy / max) : 0;
        return String.format("%,d / %,d FE (%d%%)", energy, max, percent);
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if(!(IBENetworks.getObject(accessor.getLevel(), accessor.getPosition()) instanceof BatteryNetworkObject battery))
            return;
        data.putInt("energy", battery.getStorage().getEnergyStored());
        data.putInt("max", battery.getStorage().getMaxEnergyStored());

        // Every battery reachable over this one's power networks, each counted once.
        Set<BENetworkObject> seen = new HashSet<>();
        long energy = 0, max = 0;
        for(BENetwork network : battery.getNetworks().values()) {
            if(!(network instanceof PowerBENetwork))
                continue;
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof BatteryNetworkObject other && seen.add(other)) {
                    energy += other.getStorage().getEnergyStored();
                    max += other.getStorage().getMaxEnergyStored();
                }
            }
        }
        data.putInt("count", seen.size());
        data.putInt("networkEnergy", (int)Math.min(Integer.MAX_VALUE, energy));
        data.putInt("networkMax", (int)Math.min(Integer.MAX_VALUE, max));
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("battery");
    }
}

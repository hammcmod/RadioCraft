package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.ChargeControllerNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.SolarPanelNetworkObject;
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

/** Jade lines for a solar panel: what it is making now, and what the whole row it is part of makes. */
public enum SolarPanelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("output"))
            return;
        int output = data.getInt("output");
        tooltip.add(Component.literal(output > 0 ? "Making: " + output + " FE/tick" : "Making: nothing (" + data.getString("why") + ")"));
        int count = data.getInt("count");
        if(count > 1)
            tooltip.add(Component.literal("Row: " + count + " panels, " + data.getInt("rowOutput") + " FE/tick together"));
        tooltip.add(Component.literal(data.getBoolean("controller") ? "Feeding a charge controller" : "No charge controller connected"));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if(!(IBENetworks.getObject(accessor.getLevel(), accessor.getPosition()) instanceof SolarPanelNetworkObject panel))
            return;
        data.putInt("output", panel.getLastPowerTick());
        data.putString("why", !accessor.getLevel().isDay() ? "night" : !accessor.getLevel().canSeeSky(accessor.getPosition().above()) ? "no sky above" : "nothing to feed");

        Set<BENetworkObject> seen = new HashSet<>();
        int rowOutput = 0;
        boolean controller = false;
        for(BENetwork network : panel.getNetworks().values()) {
            if(!(network instanceof PowerBENetwork))
                continue;
            for(BENetworkObject object : network.getNetworkObjects()) {
                if(object instanceof SolarPanelNetworkObject other && seen.add(other))
                    rowOutput += other.getLastPowerTick();
                else if(object instanceof ChargeControllerNetworkObject)
                    controller = true;
            }
        }
        data.putInt("count", seen.size());
        data.putInt("rowOutput", rowOutput);
        data.putBoolean("controller", controller);
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("solar_panel");
    }
}

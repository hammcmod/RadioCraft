package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blocks.AntennaCenterBlock;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class InfoHelpers {
    public static List<Component> getAntennaInfo(AntennaCenterBlock block, Level level, BlockPos pos) {
        ArrayList<Component> info = new ArrayList<>();
        if(IBENetworks.getObject(level, pos) instanceof AntennaNetworkObject networkObject) {
            StaticAntenna<?> antenna = networkObject.getAntenna();
            if(antenna != null)
                info.add(Component.literal(String.format("Antenna Type: %s", antenna.type.toString())));
            else
                info.add(Component.literal("No valid antenna found"));
        } else {
            info.add(Component.literal("No valid antenna network found"));
        }
        return info;
    }
}

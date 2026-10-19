package com.arrl.radiocraft.common;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class CommonEvents {

    /** Ticks every network object (solar panels, charge controllers, batteries, radios) on the server. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if(!event.getLevel().isClientSide)
            IBENetworks.get(event.getLevel()).tickNetworkObjects(event.getLevel());
    }

}

package com.arrl.radiocraft.common;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class CommonEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel().isClientSide) {
        //if(!event.level.isClientSide && event.phase == Phase.START) {
            //IBENetworks.get(event.getLevel()).tickNetworkObjects(event.getLevel());
        }
    }

}

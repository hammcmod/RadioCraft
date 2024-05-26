package com.arrl.radiocraft.common;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = Radiocraft.MOD_ID, bus = Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent event) {
        if(!event.level.isClientSide && event.phase == Phase.START) {
            IBENetworks.get(event.level).tickNetworkObjects(event.level);
        }
    }

}

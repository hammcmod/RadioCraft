package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.data.BlockEntityCallsignSavedData;
import com.arrl.radiocraft.common.data.PlayerCallsignSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class RadiocraftSavedData {

    @SubscribeEvent
    public static void levelLoad(LevelEvent.Load event) {
        Radiocraft.LOGGER.info("LevelEvent " + event.toString());
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) {
            ServerLevel level = event.getLevel().getServer().overworld();
            Radiocraft.LOGGER.info("Loading the saved data classes");
            PlayerCallsignSavedData.get(level);
            BlockEntityCallsignSavedData.get(level);
        }
    }
}

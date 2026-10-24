package com.arrl.radiocraft.gametest;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import net.minecraft.gametest.framework.GameTestServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Settings the GameTest server runs with. The radios in almost every test have no power source, so power draw is
 * switched off for the whole run; {@link PowerGameTests} switches it on for itself. Radios never blow on a bad SWR,
 * so the tests that key into one stay deterministic; {@link BlownRadioGameTests} switches that on for its own batch.
 * Only the GameTest server is touched: a dev client or dev server keeps the real config.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class GameTestDefaults {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if(event.getServer() instanceof GameTestServer) {
            RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
            RadiocraftServerConfig.RADIO_BLOW_CHANCE.set(0.0D);
        }
    }
}

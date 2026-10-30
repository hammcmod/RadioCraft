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
 * Terrain is off, since the test plots are hills to each other; {@link TerrainGameTests} switches it on for itself.
 * Only the GameTest server is touched: a dev client or dev server keeps the real config.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public class GameTestDefaults {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if(event.getServer() instanceof GameTestServer) {
            RadiocraftServerConfig.RADIO_POWER_REQUIRED.set(false);
            RadiocraftServerConfig.RADIO_BLOW_CHANCE.set(0.0D);
            RadiocraftServerConfig.TERRAIN_ENABLED.set(false);
            RadiocraftServerConfig.WIRE_HAZARDS_ENABLED.set(false); // ElectricalHazardGameTests switches them on.
            RadiocraftServerConfig.BATTERY_FIRES_ENABLED.set(false);
            RadiocraftServerConfig.APRS_BEACON_DEFAULT.set(false); // Stations stay quiet unless a test asks.
            RadiocraftServerConfig.APRS_DIGIPEAT_DEFAULT.set(false);
        }
    }

    /**
     * Puts the level's propagation back to normal: average solar conditions, no aurora, no sporadic E. Batches that
     * change them call it after themselves, and batches that need a quiet sky before, since a failed test never reaches
     * its own clean-up and the next batch would inherit a blackout (seen 2026-10-27/28).
     */
    public static void calmSky(net.minecraft.server.level.ServerLevel level) {
        com.arrl.radiocraft.common.radio.solar.SolarEventManager.setEvent(level,
                com.arrl.radiocraft.common.init.RadiocraftData.SOLAR_EVENTS.getValue(com.arrl.radiocraft.Radiocraft.id("average")).getInstance());
        com.arrl.radiocraft.common.radio.solar.Aurora.clear(level);
        com.arrl.radiocraft.common.radio.solar.SporadicE.close(level);
    }
}

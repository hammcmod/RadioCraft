package com.arrl.radiocraft.client.events;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.client.RadiocraftClientValues;
import com.arrl.radiocraft.common.network.serverbound.SPlayerClickHoldUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Optional;

import static com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities.VHF_HANDHELDS;

@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public class ClientTick {

    // How long to hold the PTT button before it is considered a release from lack of held event (milliseconds)
    private static long PTT_HOLD_MS = 200;

    private static boolean wasPttHeld = false;
    private static long lastPttAction = 0;

    public static void pttActivated() {
        lastPttAction = System.currentTimeMillis();
        SPlayerClickHoldUpdate.updateServer(true);
        RadiocraftClientValues.PTT_PRESSED = true;
        RadiocraftClientValues.VOICE_ENABLED = true;

        if (!wasPttHeld) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            wasPttHeld = true;
        }
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading configEvent) {
        PTT_HOLD_MS = CommonConfig.PTT_HELD_RELEASE_TIME_MS.get();
    }

    @SubscribeEvent
    public static void onConfigReloaded(ModConfigEvent.Reloading configEvent) {
        PTT_HOLD_MS = CommonConfig.PTT_HELD_RELEASE_TIME_MS.get();
    }

    /**
     * Handles right click to PTT on VHF handhelds
     * @param event - the event
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Check if the PTT button hold has expired if we have not already set it as unheld
        if (System.currentTimeMillis() - lastPttAction > PTT_HOLD_MS && wasPttHeld) {
            // Timeout has passed, reset the state
            try { SPlayerClickHoldUpdate.updateServer(false); } catch (NullPointerException ignored) {}
            RadiocraftClientValues.PTT_PRESSED = false;
            RadiocraftClientValues.VOICE_ENABLED = false;
            wasPttHeld = false;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3f));
        }
    }
}

package com.arrl.radiocraft.client.events;

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
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Optional;

import static com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities.VHF_HANDHELDS;

@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public class ClientTick {


    private static boolean wasUseHeld = false;

    /**
     * Handles right click to PTT on VHF handhelds
     * @param event - the event
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        if(Minecraft.getInstance().screen != null) {
            wasUseHeld = Minecraft.getInstance().options.keyUse.isDown();
            return;
        }

        boolean isUseHeld = Minecraft.getInstance().options.keyUse.isDown();

        if(isUseHeld != wasUseHeld) {
            SPlayerClickHoldUpdate.updateServer(isUseHeld);

            boolean holdingRadio = Optional.ofNullable(Minecraft.getInstance().player).map(Player::getMainHandItem).map(i -> i.getCapability(VHF_HANDHELDS)).isPresent();

            RadiocraftClientValues.SCREEN_PTT_PRESSED =
                    RadiocraftClientValues.SCREEN_VOICE_ENABLED =
                            isUseHeld && !Minecraft.getInstance().options.keyShift.isDown() && holdingRadio;

            if(holdingRadio && !Minecraft.getInstance().options.keyShift.isDown()) {
                if(isUseHeld) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                } else {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3f));
                }
            }
        }

        wasUseHeld = isUseHeld;
    }
}

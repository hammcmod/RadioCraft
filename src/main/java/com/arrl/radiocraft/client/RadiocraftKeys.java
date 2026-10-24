package com.arrl.radiocraft.client;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** RadioCraft's key bindings (Options, Controls, under RadioCraft). */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RadiocraftKeys {

    /** Hold to talk on a handheld worn on the belt (Curios), the way right click talks on one in the hand. */
    public static final KeyMapping WORN_PTT = new KeyMapping("key.radiocraft.worn_ptt", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.radiocraft");

    /** A straight key: hold to send CW from an open HF radio screen in CW mode (proposal: the Alt key). */
    public static final KeyMapping CW_KEY = new KeyMapping("key.radiocraft.cw_key", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.radiocraft");

    private RadiocraftKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(WORN_PTT);
        event.register(CW_KEY);
    }
}

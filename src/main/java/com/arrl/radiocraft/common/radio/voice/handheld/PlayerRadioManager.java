package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class PlayerRadioManager {

    private static final HashMap<UUID, PlayerRadio> playerRadios = new HashMap<>();

    public static PlayerRadio get(UUID uuid) {
        return playerRadios.get(uuid);
    }

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        playerRadios.put(player.getUUID(), new PlayerRadio(player));
    }

    @SubscribeEvent
    public static void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        get(uuid).setPlayer(null);
        playerRadios.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        get(event.getOriginal().getUUID()).setPlayer(event.getEntity()); // Ensure to update the player object when cloned (e.g. dimension change)
    }

}

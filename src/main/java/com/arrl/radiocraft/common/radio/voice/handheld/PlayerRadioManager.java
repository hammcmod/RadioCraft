package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class PlayerRadioManager {

    private static final HashMap<UUID, PlayerRadio> playerRadios = new HashMap<>();

    public static Optional<PlayerRadio> get(UUID uuid) {
        return Optional.ofNullable(playerRadios.get(uuid));
    }

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        playerRadios.put(player.getUUID(), new PlayerRadio(player));
        Radiocraft.LOGGER.debug("Player radio added for " + player.getName() + " uuid " + player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        get(uuid).ifPresentOrElse(u -> u.setPlayer(null), () -> System.out.println("Player radio was null on leave, onPlayerJoined not called? Player: " + event.getEntity().getName() + " uuid " + event.getEntity().getUUID()));
        Radiocraft.LOGGER.debug("Player radio added for " + event.getEntity().getName() + " uuid " + event.getEntity().getUUID());
        playerRadios.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if(event.isWasDeath()) {
            get(event.getOriginal().getUUID()).ifPresentOrElse(
                    r -> r.setPlayer(event.getEntity()),
                    () -> {
                        playerRadios.put(event.getEntity().getUUID(), new PlayerRadio(event.getEntity()));
                        System.out.println("Player radio was null on death, onPlayerJoined not called? Player: " + event.getEntity().getName());
                    }
            ); // Ensure to update the player object when cloned (e.g. dimension change)
        }
    }

}

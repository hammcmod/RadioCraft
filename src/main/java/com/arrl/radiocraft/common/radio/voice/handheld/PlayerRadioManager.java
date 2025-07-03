package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class PlayerRadioManager {

    private static final Map<UUID, PlayerRadio> playerRadios = new ConcurrentHashMap<>();

    public static Optional<PlayerRadio> get(UUID uuid) {
        return Optional.ofNullable(playerRadios.get(uuid));
    }

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        playerRadios.put(player.getUUID(), new PlayerRadio(player));
        Radiocraft.LOGGER.debug("Player radio added for {} uuid {}", player.getName(), player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        get(uuid).ifPresentOrElse(u -> u.setPlayer(null), () -> Radiocraft.LOGGER.error("Player radio was null on leave, onPlayerJoined not called? Player: {} uuid {}", event.getEntity().getName(), event.getEntity().getUUID()));
        Radiocraft.LOGGER.debug("Player radio removed for {} uuid {}", event.getEntity().getName(), event.getEntity().getUUID());
        playerRadios.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if(event.isWasDeath()) {
            get(event.getOriginal().getUUID()).ifPresentOrElse(
                    r -> r.setPlayer(event.getEntity()),
                    () -> {
                        playerRadios.put(event.getEntity().getUUID(), new PlayerRadio(event.getEntity()));
                        Radiocraft.LOGGER.error("Player radio was null on death, onPlayerJoined not called? Player: {}", event.getEntity().getName());
                    }
            ); // Ensure to update the player object when cloned (e.g. dimension change)
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event){
        for(PlayerRadio playerRadio : playerRadios.values()) {
            playerRadio.tick();
        }
    }

}

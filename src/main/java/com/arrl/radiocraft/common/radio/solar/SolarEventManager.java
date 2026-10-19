package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.network.clientbound.CNoisePacket;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Picks and ticks the current solar weather event per dimension, and keeps clients informed of its noise floor
 * through {@link CNoisePacket}.
 */
@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class SolarEventManager {

	private static final Map<ResourceKey<Level>, SolarEventInstance> currentEvents = new HashMap<>();

	public static void setEvent(ResourceKey<Level> dimension, SolarEventInstance event) {
		currentEvents.put(dimension, event);
	}

	public static void setEvent(Level level, SolarEventInstance event) {
		setEvent(level.dimension(), event);
	}

	public static SolarEventInstance getEvent(Level level) {
		return getEvent(level.dimension());
	}

	public static SolarEventInstance getEvent(ResourceKey<Level> dimension) {
		return currentEvents.get(dimension);
	}

	@SubscribeEvent
	public static void tick(LevelTickEvent.Pre event) {
		if(event.getLevel() instanceof ServerLevel level) {
			SolarEventInstance solarEvent = getEvent(level);

			if(solarEvent == null || solarEvent.isFinished()) {
				solarEvent = RadiocraftData.SOLAR_EVENTS.getWeightedRandom().getInstance(); // Keep picking new event if the current one is finished
				setEvent(level, solarEvent);
				CNoisePacket.sendToLevel(level); // Update noise value for all players in that level
				Radiocraft.LOGGER.info("Dimension: {} Solar Event Noise: {}", level.dimension(), solarEvent.getEvent().getNoise());
			}
			solarEvent.tick();
		}
	}

	@SubscribeEvent
	public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { // Update noise every time player changes dimension
		if(event.getEntity() instanceof ServerPlayer player)
			CNoisePacket.sendToPlayer(player);
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.getEntity() instanceof ServerPlayer player)
			CNoisePacket.sendToPlayer(player);
	}

}

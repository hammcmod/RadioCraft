package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.init.RadiocraftPackets;
import com.arrl.radiocraft.common.network.packets.ClientboundNoisePacket;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid=Radiocraft.MOD_ID, bus=Bus.FORGE)
public class SolarEventManager {

	private static Map<ResourceKey<Level>, SolarEventInstance> currentEvents = new HashMap<>();

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
	public static void tick(LevelTickEvent event) {
		if(!event.level.isClientSide) {
			if(event.phase == Phase.START) {
				SolarEventInstance solarEvent = getEvent(event.level);

				if(solarEvent == null || solarEvent.isFinished()) {
					solarEvent = RadiocraftData.SOLAR_EVENTS.getWeightedRandom().getInstance(); // Keep picking new event if the current one is finished
					setEvent(event.level, solarEvent);
					RadiocraftPackets.sendToLevel(new ClientboundNoisePacket(solarEvent.getEvent().getNoise()), (ServerLevel)event.level); // Update noise value for all players in that level
					Radiocraft.LOGGER.info(event.level.dimension() + " " + solarEvent.getEvent().getNoise());
				}

				solarEvent.tick();
			}
		}
	}

	@SubscribeEvent
	public static void playerChangedDimension(PlayerChangedDimensionEvent event) { // Update noise every time player changes dimension
		if(event.getEntity() instanceof ServerPlayer player) {
			RadiocraftPackets.sendToPlayer(new ClientboundNoisePacket(getEvent(event.getTo()).getEvent().getNoise()), player);
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerLoggedInEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
			RadiocraftPackets.sendToPlayer(new ClientboundNoisePacket(getEvent(player.getLevel()).getEvent().getNoise()), player);
		}
	}

}

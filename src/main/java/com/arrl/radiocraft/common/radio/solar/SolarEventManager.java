package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.network.clientbound.CAuroraPacket;
import com.arrl.radiocraft.common.network.clientbound.CNoisePacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks and ticks the current solar weather event per dimension, and keeps clients informed of its noise floor
 * through {@link CNoisePacket}.
 */
@EventBusSubscriber(modid=Radiocraft.MOD_ID)
public class SolarEventManager {

	private static final Map<ResourceKey<Level>, SolarEventInstance> currentEvents = new ConcurrentHashMap<>(); // Read from the voice chat thread.

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

	/**
	 * How {@code band} carries skip in {@code level} right now, from the current event and the time of day. Good while
	 * no event has been picked yet, and for bands without a name.
	 */
	public static BandCondition getCondition(Level level, @Nullable Band band) {
		SolarEventInstance event = getEvent(level);
		if(event == null || band == null)
			return BandCondition.GOOD;
		return event.getEvent().getCondition(band.name(), level.isDay());
	}

	/**
	 * How {@code band} carries skip between stations at {@code positions}: {@link #getCondition(Level, Band)}, one step
	 * worse if any of them is under an aurora.
	 */
	public static BandCondition getCondition(Level level, @Nullable Band band, BlockPos... positions) {
		BandCondition condition = getCondition(level, band);
		if(Aurora.isActive(level))
			for(BlockPos pos : positions)
				if(Aurora.inZone(pos))
					return condition.worse();
		return condition;
	}

	/** Starts a fresh spell of {@code event} in {@code level}, tells its players the new noise and rolls for an aurora. */
	public static void startEvent(ServerLevel level, SolarEvent event) {
		setEvent(level, event.getInstance());
		CNoisePacket.sendToLevel(level); // Update noise value for all players in that level
		Aurora.onEventStarted(level, event);
		Radiocraft.LOGGER.info("Dimension: {} Solar Event Noise: {}", level.dimension(), event.getNoise());
	}

	@SubscribeEvent
	public static void tick(LevelTickEvent.Pre event) {
		if(event.getLevel() instanceof ServerLevel level) {
			SolarEventInstance solarEvent = getEvent(level);

			if(solarEvent == null || solarEvent.isFinished()) // Keep picking new event if the current one is finished
				startEvent(level, RadiocraftData.SOLAR_EVENTS.getWeightedRandom());
			getEvent(level).tick();
			Aurora.tick(level);
		}
	}

	@SubscribeEvent
	public static void serverStopped(ServerStoppedEvent event) { // In single player the next world must not inherit this one's aurora.
		Aurora.clearAll();
	}

	@SubscribeEvent
	public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { // Update noise every time player changes dimension
		if(event.getEntity() instanceof ServerPlayer player) {
			CNoisePacket.sendToPlayer(player);
			CAuroraPacket.sendToPlayer(player);
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
			CNoisePacket.sendToPlayer(player);
			CAuroraPacket.sendToPlayer(player);
		}
	}

}

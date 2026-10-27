package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
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
	private static final Map<ResourceKey<Level>, Upcoming> upcomingEvents = new ConcurrentHashMap<>();

	/** The event that follows the current one, picked {@link RadiocraftServerConfig#SOLAR_FORECAST_MINUTES} ahead, and when it starts. */
	public record Upcoming(SolarEvent event, long startGameTime) {}

	/** The next event if it has been picked yet (in the last minutes of the current one), else null. */
	@Nullable
	public static Upcoming getUpcoming(Level level) {
		return upcomingEvents.get(level.dimension());
	}

	/**
	 * Picks {@code event} to follow the current one, which still ends when it would have (a forecast for tests and
	 * /rcdebug solar forecast). With no current event it starts at the end of the forecast window.
	 */
	public static void forecast(ServerLevel level, SolarEvent event) {
		SolarEventInstance current = getEvent(level);
		long remaining = current == null ? RadiocraftServerConfig.SOLAR_FORECAST_MINUTES.get() * 1200L : Math.max(0, current.getDuration() - current.getTicks());
		upcomingEvents.put(level.dimension(), new Upcoming(event, level.getGameTime() + remaining));
	}

	/** Forgets the forecast: the next event is picked afresh (for tests, which must not leave a blackout coming). */
	public static void clearForecast(Level level) {
		upcomingEvents.remove(level.dimension());
	}

	/** True for the noisiest event there is: a radio blackout. */
	public static boolean isBlackout(@Nullable SolarEvent event) {
		return event != null && RadiocraftData.SOLAR_EVENTS.conditionLevel(event) == 1;
	}

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

			if(!RadiocraftServerConfig.SOLAR_EVENTS_ENABLED.get()) { // Solar weather off: average conditions, always.
				SolarEvent average = RadiocraftData.SOLAR_EVENTS.getValue(Radiocraft.id("average"));
				if(average != null && (solarEvent == null || solarEvent.getEvent() != average))
					startEvent(level, average);
			}
			else if(solarEvent == null || solarEvent.isFinished()) { // The forecast event, or a fresh pick if none was made.
				Upcoming next = upcomingEvents.remove(level.dimension());
				startEvent(level, next != null ? next.event() : RadiocraftData.SOLAR_EVENTS.getWeightedRandom());
			}
			SolarEventInstance current = getEvent(level);
			current.tick();
			int remaining = current.getDuration() - current.getTicks();
			if(RadiocraftServerConfig.SOLAR_EVENTS_ENABLED.get() && !upcomingEvents.containsKey(level.dimension())
					&& remaining <= RadiocraftServerConfig.SOLAR_FORECAST_MINUTES.get() * 1200) // Late in this event: pick the next one now, so it can be forecast.
				upcomingEvents.put(level.dimension(), new Upcoming(RadiocraftData.SOLAR_EVENTS.getWeightedRandom(), level.getGameTime() + remaining));
			Aurora.tick(level);
			SporadicE.tick(level);
		}
	}

	@SubscribeEvent
	public static void serverStopped(ServerStoppedEvent event) { // In single player the next world must not inherit this one's aurora.
		Aurora.clearAll();
		SporadicE.clearAll();
		upcomingEvents.clear();
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

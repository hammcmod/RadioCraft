package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.solar.SolarEvent;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the solar weather of its dimension for the {@link com.arrl.radiocraft.common.blocks.SolarWeatherStationBlock}:
 * the current event's name, its rank for a comparator, and whether 10m is open. The only reason this is a block entity
 * is the comparator: it has to be told when the event changes, which the server tick notices by watching the event
 * instance. The printed paper report from the proposal is still to come.
 */
public class SolarWeatherStationBlockEntity extends BlockEntity {

	/** The band the station reports on by name, the one the proposal calls out as opening and closing with the weather. */
	public static final String REPORTED_BAND = "10m";

	@Nullable
	private SolarEventInstance lastSeenEvent;

	public SolarWeatherStationBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_WEATHER_STATION.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SolarWeatherStationBlockEntity be) {
		SolarEventInstance current = SolarEventManager.getEvent(level);
		if(current != be.lastSeenEvent) {
			be.lastSeenEvent = current;
			level.updateNeighbourForOutputSignal(pos, state.getBlock()); // Comparators only re-read when told to.
		}
	}

	/** The solar event in this station's dimension, null before the manager has picked one. */
	@Nullable
	public SolarEvent getCurrentEvent() {
		if(level == null)
			return null;
		SolarEventInstance instance = SolarEventManager.getEvent(level);
		return instance == null ? null : instance.getEvent();
	}

	/** Data pack id of the current event ("radiocraft:good"), null if none. */
	@Nullable
	public ResourceLocation getCurrentEventKey() {
		SolarEvent event = getCurrentEvent();
		return event == null ? null : RadiocraftData.SOLAR_EVENTS.getKey(event);
	}

	/** Comparator output: 1 for the noisiest conditions (blackout) up to 6 for the quietest (great), 0 with no event. */
	public int getConditionLevel() {
		return RadiocraftData.SOLAR_EVENTS.conditionLevel(getCurrentEvent());
	}

	/** Whether {@link #REPORTED_BAND} propagates in the current weather. */
	public boolean isReportedBandOpen() {
		SolarEvent event = getCurrentEvent();
		return event != null && event.isBandOpen(REPORTED_BAND);
	}

	/** The event's display name: a translation if the pack has one, else the id's path with a capital ("Good"). */
	public static MutableComponent eventName(@Nullable ResourceLocation key) {
		if(key == null)
			return Component.translatable("solar_event.radiocraft.none");
		String path = key.getPath();
		String fallback = path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
		return Component.translatableWithFallback("solar_event." + key.getNamespace() + "." + path, fallback);
	}

	/** The one-line report the block shows on right click. */
	public Component getReport() {
		SolarEvent event = getCurrentEvent();
		if(event == null)
			return Component.translatable("message.radiocraft.solar_weather_station.no_data");
		return Component.translatable("message.radiocraft.solar_weather_station.report",
				eventName(getCurrentEventKey()),
				Math.round(event.getNoise() * 100.0F),
				REPORTED_BAND,
				Component.translatable(isReportedBandOpen() ? "message.radiocraft.solar_weather_station.open" : "message.radiocraft.solar_weather_station.closed"));
	}
}

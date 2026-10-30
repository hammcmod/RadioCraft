package com.arrl.radiocraft.api.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Where RadioCraft's weather gear (WX reports, WEFAX charts, weather stations) gets its weather. Vanilla weather is
 * always available; a weather mod registers its own with {@link WeatherSources#register} and is used in the levels it
 * {@link #handles}. Call on the server thread.
 *
 * <p>Another Weather Mod is supported out of the box; its source is in {@code compat/anotherweathermod}.
 */
public interface WeatherSource {

	/** A short name for reports: "VANILLA", "AWM". */
	String name();

	/** Whether this source runs the weather in {@code level}. */
	boolean handles(Level level);

	/** Whether the weather differs from place to place (storms you can see coming), or is the same everywhere. */
	boolean isLocal();

	/** The weather at {@code pos} now. */
	default WeatherReport report(ServerLevel level, BlockPos pos) {
		return reportAt(level, pos, level.getGameTime());
	}

	/** The weather at {@code pos} at {@code gameTime}, as forecast if that is ahead. */
	WeatherReport reportAt(ServerLevel level, BlockPos pos, long gameTime);

	/** How hard it rains at ({@code x}, {@code z}), 0..1, ignoring the biome. Cheap: radar charts call it per pixel. */
	double rainAt(ServerLevel level, double x, double z, long gameTime);

	/** The thunder at ({@code x}, {@code z}), 0..1. Cheap, like {@link #rainAt}. */
	double thunderAt(ServerLevel level, double x, double z, long gameTime);

	/**
	 * Storms within {@code radius} blocks of {@code center} (their edge, that is), with their forecast tracks.
	 *
	 * @param forecast include storms that have not formed yet but are forecast to
	 */
	List<StormReport> storms(ServerLevel level, BlockPos center, double radius, boolean forecast);

	/**
	 * When the next storm reaches {@code pos} within {@code horizon} ticks. A storm already there counts, with an
	 * arrival of now.
	 */
	Optional<Arrival> nextArrival(ServerLevel level, BlockPos pos, long horizon);

	/** How far ahead this source can forecast, ticks. */
	long forecastHorizon(ServerLevel level);

	/**
	 * True if a tornado is possible near {@code pos} soon, though probably will not happen: a tornado watch. WEFAX prints
	 * it and gives it redstone level 3. False for sources that cannot tell.
	 */
	default boolean tornadoWatch(ServerLevel level, BlockPos pos) {
		return false;
	}

	/** The day {@code daysAhead} from today (0 today, 1 tomorrow) at {@code pos}; {@link DayOutlook#UNKNOWN} if the source cannot say. */
	default DayOutlook outlook(ServerLevel level, BlockPos pos, int daysAhead) {
		return DayOutlook.UNKNOWN;
	}

	/**
	 * A storm reaching a place.
	 *
	 * @param peak the worst weather it brings there
	 */
	record Arrival(StormReport storm, long arrivalTick, long departureTick, WeatherReport peak, long peakTick) {
	}
}

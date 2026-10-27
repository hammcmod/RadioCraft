package com.arrl.radiocraft.api.weather;

import com.arrl.radiocraft.common.radio.weather.VanillaWeatherSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The registered {@link WeatherSource}s, and the one to use in a level. */
public final class WeatherSources {

	/** Vanilla's world-wide weather. Always there; used where no other source handles the level. */
	public static final WeatherSource VANILLA = new VanillaWeatherSource();

	private static final List<WeatherSource> SOURCES = new CopyOnWriteArrayList<>();

	private WeatherSources() {
	}

	/** Adds a source. The first registered that handles a level is used there. Call at common setup. */
	public static void register(WeatherSource source) {
		SOURCES.add(source);
	}

	/** The source for {@code level}. */
	public static WeatherSource get(Level level) {
		for(WeatherSource source : SOURCES)
			if(source.handles(level))
				return source;
		return VANILLA;
	}

	/** Whether it rains at {@code pos}, from whichever source runs {@code level}. Client levels ask vanilla. */
	public static boolean isRaining(Level level, BlockPos pos) {
		if(level instanceof ServerLevel server)
			return get(level).rainAt(server, pos.getX() + 0.5, pos.getZ() + 0.5, level.getGameTime()) > 0.2;
		return level.isRaining();
	}

	/** Hazards that make weather severe. */
	private static final java.util.Set<WeatherReport.Hazard> SEVERE = java.util.EnumSet.of(WeatherReport.Hazard.HAIL,
			WeatherReport.Hazard.BLIZZARD, WeatherReport.Hazard.SANDSTORM, WeatherReport.Hazard.DAMAGING_WIND,
			WeatherReport.Hazard.TORNADO, WeatherReport.Hazard.HURRICANE_EYE);

	/**
	 * Whether there is severe weather at {@code pos}: hail, a blizzard or sandstorm, damaging wind, a tornado or a
	 * hurricane. Only a local source (a weather mod with storms) reports it; vanilla weather never is severe.
	 */
	public static boolean isSevere(Level level, BlockPos pos) {
		if(!(level instanceof ServerLevel server))
			return false;
		WeatherSource source = get(level);
		if(!source.isLocal())
			return false;
		for(WeatherReport.Hazard hazard : source.report(server, pos).hazards())
			if(SEVERE.contains(hazard))
				return true;
		return false;
	}

	/** Whether it thunders at {@code pos} (vanilla's threshold, 0.9). Client levels ask vanilla. */
	public static boolean isThundering(Level level, BlockPos pos) {
		if(level instanceof ServerLevel server)
			return get(level).thunderAt(server, pos.getX() + 0.5, pos.getZ() + 0.5, level.getGameTime()) > 0.9;
		return level.isThundering();
	}
}

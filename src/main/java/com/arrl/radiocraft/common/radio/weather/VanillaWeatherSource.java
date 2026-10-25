package com.arrl.radiocraft.common.radio.weather;

import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherReport.Hazard;
import com.arrl.radiocraft.api.weather.WeatherReport.Precipitation;
import com.arrl.radiocraft.api.weather.WeatherSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Vanilla's weather: the same everywhere, but forecastable, since the server counts down to the next rain and thunder
 * ({@link ServerLevelData#getRainTime()}). There is no wind; temperature comes from the biome and the time of day.
 */
public final class VanillaWeatherSource implements WeatherSource {

	/** How long vanilla rain is assumed to last once it starts, for forecasts: half a day. */
	static final long RAIN_SPELL = 12000;

	@Override
	public String name() {
		return "VANILLA";
	}

	@Override
	public boolean handles(Level level) {
		return true;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	/** Ticks until vanilla rain starts, 0 if raining now, or -1 if it is not counting down to rain. */
	static long ticksToRain(ServerLevel level) {
		if(level.isRaining())
			return 0;
		ServerLevelData data = level.getServer().getWorldData().overworldData();
		if(data.getClearWeatherTime() > 0)
			return data.getClearWeatherTime(); // Kept clear (/weather clear), then the cycle starts again.
		return data.getRainTime() > 0 ? data.getRainTime() : -1;
	}

	private static boolean rainingAt(ServerLevel level, long gameTime) {
		long now = level.getGameTime();
		if(gameTime <= now)
			return level.isRaining();
		long toRain = ticksToRain(level);
		if(level.isRaining()) {
			ServerLevelData data = level.getServer().getWorldData().overworldData();
			return gameTime - now < data.getRainTime();
		}
		return toRain >= 0 && gameTime - now >= toRain && gameTime - now < toRain + RAIN_SPELL;
	}

	@Override
	public WeatherReport reportAt(ServerLevel level, BlockPos pos, long gameTime) {
		boolean raining = rainingAt(level, gameTime);
		float rain = gameTime <= level.getGameTime() ? level.getRainLevel(1) : raining ? 1 : 0;
		float thunder = gameTime <= level.getGameTime() ? level.getThunderLevel(1) : 0;
		Biome biome = level.getBiome(pos).value();
		Precipitation precipitation = Precipitation.NONE;
		if(rain > 0.2)
			precipitation = switch(biome.getPrecipitationAt(pos)) {
				case RAIN -> Precipitation.RAIN;
				case SNOW -> Precipitation.SNOW;
				case NONE -> Precipitation.NONE;
			};
		Set<Hazard> hazards = EnumSet.noneOf(Hazard.class);
		if(thunder > 0.9)
			hazards.add(Hazard.LIGHTNING);
		long dayTime = level.getDayTime() + (gameTime - level.getGameTime());
		double temperature = WeatherReports.celsius(biome.getBaseTemperature()) - (pos.getY() - level.getSeaLevel()) * 0.0065
				- 5 * WeatherReports.night(dayTime) - 4 * rain;
		double dry = biome.hasPrecipitation() ? 60 : 30;
		return new WeatherReport(precipitation, rain, thunder, 0, 0, temperature, 1013.25 - 8 * rain - 6 * thunder,
				dry + (100 - dry) * rain, Set.copyOf(hazards));
	}

	@Override
	public double rainAt(ServerLevel level, double x, double z, long gameTime) {
		return gameTime <= level.getGameTime() ? level.getRainLevel(1) : rainingAt(level, gameTime) ? 1 : 0;
	}

	@Override
	public double thunderAt(ServerLevel level, double x, double z, long gameTime) {
		return gameTime <= level.getGameTime() ? level.getThunderLevel(1) : 0;
	}

	@Override
	public List<StormReport> storms(ServerLevel level, BlockPos center, double radius, boolean forecast) {
		long now = level.getGameTime();
		long toRain = ticksToRain(level);
		if(toRain == 0 || forecast && toRain > 0 && toRain <= forecastHorizon(level))
			return List.of(new StormReport(0, level.isThundering() ? StormReport.Kind.THUNDERSTORM : StormReport.Kind.SHOWER,
					true, now + toRain, List.of(new StormReport.TrackPoint(now + toRain, center.getX(), center.getZ(), 0, 1))));
		return List.of();
	}

	@Override
	public Optional<Arrival> nextArrival(ServerLevel level, BlockPos pos, long horizon) {
		long now = level.getGameTime();
		long toRain = ticksToRain(level);
		if(toRain < 0 || toRain > horizon)
			return Optional.empty();
		StormReport storm = storms(level, pos, 0, true).stream().findFirst().orElse(null);
		if(storm == null)
			return Optional.empty();
		long arrival = now + toRain;
		long departure = level.isRaining() ? now + level.getServer().getWorldData().overworldData().getRainTime() : arrival + RAIN_SPELL;
		return Optional.of(new Arrival(storm, arrival, departure, reportAt(level, pos, arrival + 1), arrival + 1));
	}

	@Override
	public long forecastHorizon(ServerLevel level) {
		return 3 * 24000;
	}
}

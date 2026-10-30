package com.arrl.radiocraft.compat.anotherweathermod;

import com.arrl.radiocraft.api.weather.DayOutlook;
import com.arrl.radiocraft.api.weather.StormReport;
import com.arrl.radiocraft.api.weather.WeatherReport;
import com.arrl.radiocraft.api.weather.WeatherSource;
import com.itcmd.anotherweathermod.api.AnotherWeatherApi;
import com.itcmd.anotherweathermod.api.Hazard;
import com.itcmd.anotherweathermod.api.StormView;
import com.itcmd.anotherweathermod.api.WeatherSample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Another Weather Mod's localized storms as a {@link WeatherSource}. Only loaded when that mod is (see
 * {@link AwmCompat}).
 */
final class AwmWeatherSource implements WeatherSource {

	/** Forecast tracks have a point this often, ticks (a minute). */
	static final int TRACK_STEP = 1200;

	private static AnotherWeatherApi api() {
		return AnotherWeatherApi.get();
	}

	@Override
	public String name() {
		return "AWM";
	}

	@Override
	public boolean handles(Level level) {
		return api().isManaged(level);
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean tornadoWatch(ServerLevel level, BlockPos pos) {
		return api().tornadoWatch(level, pos);
	}

	@Override
	public DayOutlook outlook(ServerLevel level, BlockPos pos, int daysAhead) {
		com.itcmd.anotherweathermod.api.DayOutlook day = api().outlook(level, pos, daysAhead);
		if(day.regionEdge())
			return new DayOutlook(DayOutlook.Type.CLOUDY, day.tornadoWatch());
		DayOutlook.Type type = switch(day.type()) {
			case DRY -> DayOutlook.Type.DRY;
			case CHANCE -> DayOutlook.Type.CHANCE;
			case RAINY -> DayOutlook.Type.RAINY;
			default -> DayOutlook.Type.UNKNOWN; // A kind of day a newer Another Weather Mod knows and we do not.
		};
		return new DayOutlook(type, day.tornadoWatch());
	}

	@Override
	public WeatherReport reportAt(ServerLevel level, BlockPos pos, long gameTime) {
		return report(api().sampleAt(level, pos, gameTime));
	}

	static WeatherReport report(WeatherSample s) {
		Set<WeatherReport.Hazard> hazards = EnumSet.noneOf(WeatherReport.Hazard.class);
		for(Hazard hazard : s.hazards()) {
			try {
				hazards.add(WeatherReport.Hazard.valueOf(hazard.name()));
			}
			catch(IllegalArgumentException e) {
				// A hazard from a newer version of the mod: leave it out rather than fail the report.
			}
		}
		return new WeatherReport(WeatherReport.Precipitation.valueOf(s.precipitation().name()), s.rainLevel(),
				s.thunderLevel(), s.windSpeed(), s.windFromDegrees(), s.temperature(), s.pressure(), s.humidity(),
				Set.copyOf(hazards));
	}

	@Override
	public double rainAt(ServerLevel level, double x, double z, long gameTime) {
		return api().rainLevelAt(level, x, z, gameTime);
	}

	@Override
	public double thunderAt(ServerLevel level, double x, double z, long gameTime) {
		return api().thunderLevelAt(level, x, z, gameTime);
	}

	@Override
	public List<StormReport> storms(ServerLevel level, BlockPos center, double radius, boolean forecast) {
		long now = level.getGameTime();
		long horizon = forecastHorizon(level);
		List<StormView> views = new ArrayList<>(api().storms(level));
		if(forecast)
			views.addAll(api().pendingStorms(level));
		List<StormReport> reports = new ArrayList<>();
		for(StormView view : views) {
			long from = Math.max(now, view.birthTick());
			Vec3 at = view.centerAt(from);
			double reach = Math.max(view.radiusAt(from), view.lifetimeTicks() > 0 ? view.radiusAt(from) : 0) + radius;
			if(at.distanceToSqr(center.getX(), 0, center.getZ()) > reach * reach && !closeLater(view, center, radius, from, now + horizon))
				continue;
			reports.add(report(view, now, horizon));
		}
		return reports;
	}

	/** Whether {@code view} comes within reach of {@code center} before {@code until}. */
	private static boolean closeLater(StormView view, BlockPos center, double radius, long from, long until) {
		for(long t = from; t <= Math.min(until, view.deathTick()); t += TRACK_STEP) {
			double reach = view.radiusAt(t) + radius;
			if(view.centerAt(t).distanceToSqr(center.getX(), 0, center.getZ()) <= reach * reach)
				return true;
		}
		return false;
	}

	static StormReport report(StormView view, long now, long horizon) {
		long from = Math.max(now, view.birthTick());
		long to = Math.min(view.deathTick() - 1, now + horizon);
		List<StormReport.TrackPoint> track = new ArrayList<>();
		for(long t = from; ; t += TRACK_STEP) {
			long tick = Math.min(t, Math.max(from, to));
			Vec3 c = view.centerAt(tick);
			// A pending storm's size and strength at forming are 0; show it as it will be a little later.
			long look = Math.max(tick, view.birthTick() + 200);
			track.add(new StormReport.TrackPoint(tick, c.x, c.z, view.radiusAt(look), view.intensityAt(look)));
			if(tick >= to)
				break;
		}
		StormReport.Kind kind;
		try {
			kind = StormReport.Kind.valueOf(view.kind().name());
		}
		catch(IllegalArgumentException e) {
			kind = StormReport.Kind.OTHER;
		}
		int category = kind == StormReport.Kind.HURRICANE ? view.category()
				: kind == StormReport.Kind.TORNADO ? view.tornadoRating() : -1;
		return new StormReport(view.id(), kind, false, view.birthTick(), List.copyOf(track), category);
	}

	@Override
	public Optional<Arrival> nextArrival(ServerLevel level, BlockPos pos, long horizon) {
		long now = level.getGameTime();
		return api().nextArrival(level, pos, horizon).map(a -> new Arrival(report(a.storm(), now, forecastHorizon(level)),
				a.arrivalTick(), a.departureTick(), report(a.peak()), a.peakTick()));
	}

	@Override
	public long forecastHorizon(ServerLevel level) {
		return api().forecastHorizonTicks(level);
	}
}

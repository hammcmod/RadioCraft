package com.arrl.radiocraft.api.weather;

import java.util.List;

/**
 * A storm, as a {@link WeatherSource} reports it, with where it is expected to go.
 *
 * @param id        unique within its level (and stable while the storm lives)
 * @param kind      what sort of storm
 * @param worldWide true for weather that covers the whole world (vanilla rain), which has no centre or track
 * @param formsAt   the game tick it forms, or one in the past if it already has
 * @param track     where it will be, oldest first: at least the present (or its forming), then forecast points
 */
public record StormReport(long id, Kind kind, boolean worldWide, long formsAt, List<TrackPoint> track) {

	/** What sort of storm. */
	public enum Kind {
		SHOWER, THUNDERSTORM, SUPERCELL, HURRICANE, OTHER;

		/** A short name for charts and teletype reports. */
		public String abbreviation() {
			return switch(this) {
				case SHOWER -> "SHWR";
				case THUNDERSTORM -> "TSTM";
				case SUPERCELL -> "SVR TSTM";
				case HURRICANE -> "HURCN";
				case OTHER -> "WX";
			};
		}
	}

	/**
	 * Where a storm is at {@code tick}.
	 *
	 * @param x         centre, block x
	 * @param z         centre, block z
	 * @param radius    blocks
	 * @param intensity 0..1
	 */
	public record TrackPoint(long tick, double x, double z, double radius, float intensity) {
	}

	/** Whether it has formed by {@code now}. */
	public boolean formed(long now) {
		return now >= formsAt;
	}

	/** Where it is at {@code tick}, between track points (the first or last point outside them). */
	public TrackPoint at(long tick) {
		TrackPoint first = track.getFirst();
		if(tick <= first.tick() || track.size() == 1)
			return first;
		for(int i = 1; i < track.size(); i++) {
			TrackPoint b = track.get(i);
			if(tick <= b.tick()) {
				TrackPoint a = track.get(i - 1);
				double f = (tick - a.tick()) / (double)Math.max(1, b.tick() - a.tick());
				return new TrackPoint(tick, a.x() + (b.x() - a.x()) * f, a.z() + (b.z() - a.z()) * f,
						a.radius() + (b.radius() - a.radius()) * f, (float)(a.intensity() + (b.intensity() - a.intensity()) * f));
			}
		}
		return track.getLast();
	}
}

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
 * @param category  a hurricane's category or a tornado's EF rating, 0 to 5, or -1 for none
 */
public record StormReport(long id, Kind kind, boolean worldWide, long formsAt, List<TrackPoint> track, int category) {

	/** A storm with no category. */
	public StormReport(long id, Kind kind, boolean worldWide, long formsAt, List<TrackPoint> track) {
		this(id, kind, worldWide, formsAt, track, -1);
	}

	/** Its short name with its category: "HURCN 3", "TOR EF2", "SVR TSTM". */
	public String label() {
		if(category < 0)
			return kind.abbreviation();
		return switch(kind) {
			case HURRICANE -> category == 0 ? "TROP STM" : "HURCN " + category;
			case TORNADO -> "TOR EF" + category;
			default -> kind.abbreviation();
		};
	}

	/** What sort of storm. */
	public enum Kind {
		SHOWER, THUNDERSTORM,
		/** Heavier rain and more lightning, but gusts that break nothing. */
		STRONG_THUNDERSTORM,
		/** A thunderstorm with damaging gusts but no hail. */
		SEVERE_THUNDERSTORM,
		/** The hailstorm: hail, or a blizzard or sandstorm over snow and desert. */
		SUPERCELL,
		/** A supercell with a tornado down for part of its life. */
		TORNADO, HURRICANE,
		/** Thick fog, heat waves and cold snaps: weather with no rain. */
		DENSE_FOG, HEAT_WAVE, COLD_SNAP, OTHER;

		/** Whether it brings rain (fog, heat waves and cold snaps do not). */
		public boolean bringsRain() {
			return this != DENSE_FOG && this != HEAT_WAVE && this != COLD_SNAP;
		}

		/** A short name for charts and teletype reports. */
		public String abbreviation() {
			return switch(this) {
				case SHOWER -> "SHWR";
				case THUNDERSTORM -> "TSTM";
				case STRONG_THUNDERSTORM -> "HVY TSTM";
				case SEVERE_THUNDERSTORM -> "SVR TSTM";
				case SUPERCELL -> "SUPERCELL";
				case TORNADO -> "TOR";
				case HURRICANE -> "HURCN";
				case DENSE_FOG -> "FOG";
				case HEAT_WAVE -> "HEAT";
				case COLD_SNAP -> "COLD";
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

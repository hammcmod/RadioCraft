package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;

import java.util.Collection;
import java.util.Set;

/**
 * One kind of solar weather, loaded from {@code data/radiocraft/solar_events/*.json}: its noise floor, how long a spell
 * of it lasts, and which normally closed bands it opens.
 */
public class SolarEvent {

	private final float noise;
	private final int minDuration;
	private final int maxDuration;
	private final Set<String> openBands;

	public SolarEvent(float noise, int minDuration, int maxDuration) {
		this(noise, minDuration, maxDuration, Set.of());
	}

	public SolarEvent(float noise, int minDuration, int maxDuration, Set<String> openBands) {
		this.noise = noise;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
		this.openBands = Set.copyOf(openBands);
	}

	public float getNoise() {
		return noise;
	}

	/**
	 * Bands that only propagate while this event is on, by name ("10m"). The lower HF bands are always open and are
	 * not listed; 10m is listed by the good and great events, following the proposal's "10m band has some limited
	 * propagation" during those.
	 */
	public Set<String> getOpenBands() {
		return openBands;
	}

	/** True if this event opens {@code band} (see {@link #getOpenBands()}). */
	public boolean isBandOpen(String band) {
		return openBands.contains(band);
	}

	/**
	 * Where {@code event} ranks among {@code all}, 1 for the noisiest (a blackout) up to the number of events for the
	 * quietest (great): what the solar weather station puts out to a comparator. Events with the same noise share a rank.
	 */
	public static int conditionLevel(Collection<SolarEvent> all, SolarEvent event) {
		if(event == null)
			return 0;
		int level = 0;
		for(SolarEvent other : all)
			if(other.getNoise() >= event.getNoise())
				level++;
		return Math.max(1, level);
	}

	public SolarEventInstance getInstance() {
		return new SolarEventInstance(this, Radiocraft.RANDOM.nextInt(minDuration, maxDuration+1));
	}



	public static class SolarEventInstance {

		private final SolarEvent event;
		private final int duration;
		private int ticks;

		protected SolarEventInstance(SolarEvent event, int duration) {
			this.event = event;
			this.duration = duration;
		}

		public SolarEvent getEvent() {
			return event;
		}

		public int getDuration() {
			return duration;
		}

		public int getTicks() {
			return ticks;
		}

		public void tick() {
			ticks++;
		}

		public boolean isFinished() {
			return ticks >= getDuration();
		}

	}

}

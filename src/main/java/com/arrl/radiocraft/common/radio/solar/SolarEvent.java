package com.arrl.radiocraft.common.radio.solar;

import com.arrl.radiocraft.Radiocraft;

public class SolarEvent {

	private final float noise;
	private final int minDuration;
	private final int maxDuration;

	public SolarEvent(float noise, int minDuration, int maxDuration) {
		this.noise = noise;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
	}

	public float getNoise() {
		return noise;
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

		public void tick() {
			ticks++;
		}

		public boolean isFinished() {
			return ticks >= getDuration();
		}

	}

}
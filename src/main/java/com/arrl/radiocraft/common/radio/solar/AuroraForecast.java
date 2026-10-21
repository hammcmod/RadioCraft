package com.arrl.radiocraft.common.radio.solar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * The chance that a solar event is followed by an aurora, read from the event's optional {@code "aurora"} object:
 * {@code {"chance": 0.3, "delay": 72000, "duration": 24000}}. In real life a big flare's blackout is felt at once, while
 * the cloud of plasma it throws off reaches Earth a few days later and lights up the polar sky.
 *
 * @param chance   Chance (0 to 1) of an aurora each time the event starts.
 * @param delay    Ticks from the start of the event to the start of the aurora.
 * @param duration Ticks the aurora lasts.
 */
public record AuroraForecast(double chance, int delay, int duration) {

	public AuroraForecast {
		if(chance < 0.0D || chance > 1.0D)
			throw new JsonParseException("Aurora chance must be between 0 and 1, was " + chance);
		if(delay < 0 || duration <= 0)
			throw new JsonParseException("Aurora delay must be 0 or more and duration above 0");
	}

	/** True if a roll of {@code roll} (0 to 1, uniform) brings an aurora. */
	public boolean hits(double roll) {
		return roll < chance;
	}

	public static AuroraForecast parse(JsonObject json) {
		return new AuroraForecast(json.get("chance").getAsDouble(), json.get("delay").getAsInt(), json.get("duration").getAsInt());
	}
}

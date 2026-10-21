package com.arrl.radiocraft.common.radio.solar;

import java.util.Locale;

/**
 * How well one HF band carries sky-wave (skip) signals under the current solar weather, as the solar weather station
 * reports it. The multiplier scales the skip strength: a weaker signal plays quieter against the same static, so a
 * poor band is hard to copy and a closed one is barely there. Line of sight (ground wave) is not affected.
 */
public enum BandCondition {

	GOOD(1.0D),
	FAIR(0.6D),
	POOR(0.3D),
	CLOSED(0.05D);

	private final double skipMultiplier;

	BandCondition(double skipMultiplier) {
		this.skipMultiplier = skipMultiplier;
	}

	/** Factor applied to the skip strength of a signal on a band in this condition. */
	public double getSkipMultiplier() {
		return skipMultiplier;
	}

	/** Lower-case name, as written in the solar event data and used in translation keys ("good", "closed"). */
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The condition named {@code name} (any case), or null if there is none. */
	public static BandCondition byName(String name) {
		for(BandCondition condition : values())
			if(condition.getSerializedName().equalsIgnoreCase(name))
				return condition;
		return null;
	}
}

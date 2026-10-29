package com.arrl.radiocraft.common.radio.voice;

/**
 * What a radio's audio comes out of, and how that colours it. A small speaker cannot reproduce the whole voice
 * channel: {@link #range} is the share of the receiver's passband ({@link RadioVoiceProcessor.Settings}) that gets
 * through, taken off both ends, and {@link #driveFactor} pushes the soft clipping harder for a speaker that buzzes.
 * <ul>
 *     <li>{@link #BUILT_IN}: the radio's own little speaker (and a handheld's), 80% of the passband.</li>
 *     <li>{@link #STANDARD}: a desk or large speaker touching the radio, 90%.</li>
 *     <li>{@link #PREMIUM}: a premium desk speaker touching the radio, the whole passband.</li>
 *     <li>{@link #WALL}: a wall speaker on the radio's power wire, 80% with a little extra distortion.</li>
 *     <li>{@link #LOUDSPEAKER}: a PA horn on the radio's power wire: loud, heard from much further, rather
 *     distorted, and narrow like any horn.</li>
 * </ul>
 */
public enum SpeakerKind {
	BUILT_IN(0.8D, 1.0D, 1.0D, 16.0F),
	STANDARD(0.9D, 1.0D, 1.0D, 16.0F),
	PREMIUM(1.0D, 1.0D, 1.0D, 16.0F),
	WALL(0.8D, 1.6D, 1.0D, 16.0F),
	LOUDSPEAKER(0.8D, 2.2D, 1.5D, 64.0F);

	/** Share of the receiver's passband this speaker reproduces. */
	public final double range;
	/** How much harder it drives the soft clipping than a clean speaker. */
	public final double driveFactor;
	/** Output level against a plain speaker; the AGC keeps it in bounds. */
	public final double loudness;
	/** How far away, in blocks, it can be heard. */
	public final float distance;

	SpeakerKind(double range, double driveFactor, double loudness, float distance) {
		this.range = range;
		this.driveFactor = driveFactor;
		this.loudness = loudness;
		this.distance = distance;
	}

	/** True for speakers wired to the radio (they play the audio where they stand); false for ones that touch it. */
	public boolean isWired() {
		return this == WALL || this == LOUDSPEAKER;
	}

	/** {@code settings} as heard through this speaker: the passband narrowed to {@link #range}, the drive raised. */
	public RadioVoiceProcessor.Settings apply(RadioVoiceProcessor.Settings settings) {
		double width = Math.max(0.0D, settings.highHz() - settings.lowHz());
		double trim = width * (1.0D - range) / 2.0D;
		return new RadioVoiceProcessor.Settings(settings.lowHz() + trim, settings.highHz() - trim, settings.drive() * driveFactor, settings.enabled(),
				settings.smoothSeconds());
	}

	/** The better of two speakers touching one radio. */
	public static SpeakerKind better(SpeakerKind a, SpeakerKind b) {
		return a.range >= b.range ? a : b;
	}
}

package com.arrl.radiocraft.common.radio.voice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * What a commercial radio module puts on the air, over and over, as 20 ms frames of audio: a numbers station, a
 * crackling static beacon, a repeater left keyed on dead air, a beeper for direction finders, or a Morse beacon. Each
 * program keeps its own schedule; {@link #frames} hands out the frames due this tick (none while it is off the air).
 * Pure: it only needs the game time, so it is tested without a world.
 */
public final class StationProgram {

	public enum Kind {
		/** Every five minutes: an interval tune, a call-up, then groups of five figures in Morse, or a voice tape. */
		NUMBERS,
		/** A burst of crackling static every eight seconds. */
		STATIC_BEACON,
		/** A carrier that never drops, with now and then a kerchunk: a repeater stuck keyed on dead air. */
		DEAD_AIR,
		/** A short beep every three seconds, for direction finders to home in on. */
		DF_BEEPER,
		/** VVV and its callsign in Morse, then a long dash to tune on, every half minute. */
		CW_BEACON;

		public static Kind byName(String name) {
			try {
				return valueOf(name.toUpperCase(Locale.ROOT));
			}
			catch(IllegalArgumentException e) {
				return DF_BEEPER;
			}
		}
	}

	public static final int NUMBERS_PERIOD = 5 * 60 * 20;
	public static final int STATIC_PERIOD = 8 * 20;
	public static final int BEEPER_PERIOD = 3 * 20;
	public static final int CW_PERIOD = 30 * 20;

	private final Kind kind;
	private final long seed;
	private final String callsign;
	/** A voice recording to read the numbers instead of Morse, if the server has any; null or empty for none. */
	private final Supplier<List<short[]>> voice;
	private final ArrayDeque<short[]> queue = new ArrayDeque<>();
	private long nextStart = Long.MIN_VALUE;
	private long kerchunkAt = Long.MIN_VALUE;

	public StationProgram(Kind kind, long seed, String callsign, Supplier<List<short[]>> voice) {
		this.kind = kind;
		this.seed = seed;
		this.callsign = callsign;
		this.voice = voice;
	}

	public Kind kind() {
		return kind;
	}

	/**
	 * The frames to send at {@code gameTime}: {@code count} of them while on the air (2 or 3 a tick, as the repeater
	 * paces its audio), none while off it.
	 */
	public List<short[]> frames(long gameTime, int count) {
		if(queue.isEmpty() && gameTime >= nextStart)
			start(gameTime);
		List<short[]> out = new ArrayList<>(count);
		if(kind == Kind.DEAD_AIR) {
			for(int i = 0; i < count; i++)
				out.add(queue.isEmpty() ? RepeaterAudio.silence() : queue.poll());
			return out;
		}
		for(int i = 0; i < count && !queue.isEmpty(); i++)
			out.add(queue.poll());
		return out;
	}

	/** True while it has a transmission under way (or, on dead air, always). */
	public boolean onAir() {
		return kind == Kind.DEAD_AIR || !queue.isEmpty();
	}

	private void start(long gameTime) {
		SplittableRandom random = new SplittableRandom(seed ^ (gameTime / 20) * 0x9E3779B97F4A7C15L);
		switch(kind) {
			case NUMBERS -> {
				numbers(random);
				nextStart = gameTime + NUMBERS_PERIOD;
			}
			case STATIC_BEACON -> {
				int frames = 50 + random.nextInt(50);
				double level = 0.25D + random.nextDouble() * 0.25D;
				for(int i = 0; i < frames; i++)
					queue.add(RepeaterAudio.noise(level * (0.6D + 0.4D * random.nextDouble()), random.nextLong()));
				nextStart = gameTime + STATIC_PERIOD;
			}
			case DEAD_AIR -> {
				if(gameTime >= kerchunkAt) {
					if(kerchunkAt != Long.MIN_VALUE)
						for(int i = 0; i < 8; i++)
							queue.add(RepeaterAudio.noise(0.35D, random.nextLong()));
					kerchunkAt = gameTime + 400 + random.nextInt(400);
				}
				nextStart = kerchunkAt;
			}
			case DF_BEEPER -> {
				queue.addAll(RepeaterAudio.tone(1000.0D, 0.5D, 0.3D));
				nextStart = gameTime + BEEPER_PERIOD;
			}
			case CW_BEACON -> {
				queue.addAll(RepeaterAudio.morse("VVV VVV DE " + callsign + " " + callsign, 18, 700.0D));
				queue.addAll(RepeaterAudio.tone(700.0D, 3.0D, 0.35D));
				nextStart = gameTime + CW_PERIOD;
			}
		}
	}

	/** An interval tune, the station's call-up, then ten groups of five figures (or a voice tape) and the end mark. */
	private void numbers(SplittableRandom random) {
		double[] tune = { 523.25D, 659.25D, 783.99D, 659.25D }; // A falling and rising chime.
		for(int repeat = 0; repeat < 3; repeat++) {
			for(double hz : tune)
				queue.addAll(RepeaterAudio.tone(hz, 0.35D, 0.25D));
			queue.addAll(RepeaterAudio.tone(1.0D, 0.6D, 0.0D)); // A pause.
		}
		List<short[]> recording = voice == null ? null : voice.get();
		if(recording != null && !recording.isEmpty()) {
			queue.addAll(recording);
			return;
		}
		String call = String.format(Locale.ROOT, "%03d", Math.floorMod(seed, 1000));
		StringBuilder text = new StringBuilder(call + " " + call + " " + call + " ");
		for(int group = 0; group < 10; group++) {
			for(int digit = 0; digit < 5; digit++)
				text.append(random.nextInt(10));
			text.append(' ');
		}
		text.append("000 000");
		queue.addAll(RepeaterAudio.morse(text.toString(), 15, 800.0D));
	}
}

package com.arrl.radiocraft.common.radio.voice;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Two or more stations heard on one receiver at the same time. Each arrives as its own stream of 20 ms frames; a
 * receiver plays one stream, so they are lined up ({@link Aligner}) and combined into one frame ({@link Renderer}):
 * <ul>
 *     <li>SSB (and anything not FM) adds up: both voices are heard over each other, each at its own level.</li>
 *     <li>FM captures the stronger signal: once it is {@link #FM_CAPTURE_RATIO} times the next, only it is heard, roughened
 *     the closer the other comes. Closer than that, neither captures: the receiver flips between them in garbled bursts
 *     (doubling).</li>
 * </ul>
 * One station alone goes through untouched, with no added delay.
 */
public final class VoiceMix {

	private VoiceMix() {}

	/** The weaker FM signal is captured (shut out) below this share of the stronger one's strength. */
	public static final double FM_CAPTURE_RATIO = 0.5D;
	/** Garble on a captured FM signal as the other comes up to the capture ratio (splatter, 0 to 1). */
	static final double FM_CAPTURE_GARBLE = 0.3D;
	/** Level the captured-out signal bleeds through at, just under the capture ratio. */
	static final double FM_CAPTURE_BLEED = 0.25D;
	/** Garble and bleed while two FM signals double. */
	static final double FM_DOUBLING_GARBLE = 0.5D;
	static final double FM_DOUBLING_BLEED = 0.45D;

	/**
	 * One station's frame as the receiver heard it: the parameters of {@link RadioVoiceProcessor#process} and, for FM,
	 * {@link RadioVoiceProcessor#fmFade}.
	 *
	 * @param key      Who it is: the talker and the antenna it came from (a parrot echoing you is not you).
	 * @param source   The talking player, for the squelch tail; may be null.
	 * @param fmSignal The signal's strength on an FM receiver, below 0 for SSB/AM.
	 */
	public record Signal(Object key, @Nullable UUID source, short[] pcm, double level, double rasp, double pitchShiftHz,
						 double splatter, double qrm, double fmSignal) {
		public boolean fm() {
			return fmSignal >= 0.0D;
		}

		/** How strong it is on air, to rank it against the others. */
		double strength() {
			return fm() ? fmSignal : level;
		}
	}

	/** The key for a talker heard through an antenna. */
	public static Object key(@Nullable UUID player, @Nullable Object antenna) {
		return new Key(player, antenna == null ? 0 : System.identityHashCode(antenna));
	}

	private record Key(@Nullable UUID player, int antenna) {}

	/**
	 * Lines up frames from stations talking at once. The first station to talk leads: each of its frames goes out at
	 * once, with one waiting frame from each other station. The others' frames wait (a few at most) for the lead's next
	 * one. When the lead stops, the next station to send a frame leads. A station alone always leads, so it is never
	 * held back. Not thread safe: hold the receiver's lock.
	 */
	public static final class Aligner {
		/** The lead is given up after this long without a frame from it. */
		static final long LEAD_TIMEOUT_NANOS = 70_000_000L;
		/** A waiting station is forgotten after this long without a frame. */
		static final long FORGET_NANOS = 250_000_000L;
		/** Frames kept waiting per station; older ones are dropped (60 ms of delay at most). */
		static final int MAX_WAITING = 3;

		private Object lead;
		private long leadLast;
		private final Map<Object, ArrayDeque<Signal>> waiting = new LinkedHashMap<>();
		private final Map<Object, Long> heard = new HashMap<>();

		/** {@code signal} arrived at {@code nanos}: the frames to play now, one per station, the lead first; maybe none. */
		public List<Signal> offer(Signal signal, long nanos) {
			Object key = signal.key();
			heard.put(key, nanos);
			heard.entrySet().removeIf(e -> nanos - e.getValue() > FORGET_NANOS);
			waiting.keySet().removeIf(k -> !heard.containsKey(k));
			if(lead != null && !lead.equals(key) && nanos - leadLast > LEAD_TIMEOUT_NANOS)
				lead = null;
			if(lead == null)
				lead = key;
			if(!lead.equals(key)) {
				ArrayDeque<Signal> queue = waiting.computeIfAbsent(key, k -> new ArrayDeque<>());
				queue.addLast(signal);
				while(queue.size() > MAX_WAITING)
					queue.removeFirst();
				return List.of();
			}
			leadLast = nanos;
			List<Signal> out = new ArrayList<>();
			ArrayDeque<Signal> own = waiting.get(key); // Frames it sent while it was waiting go first, in order.
			if(own != null && !own.isEmpty()) {
				own.addLast(signal);
				out.add(own.removeFirst());
			}
			else
				out.add(signal);
			for(Map.Entry<Object, ArrayDeque<Signal>> entry : waiting.entrySet())
				if(!entry.getKey().equals(key) && !entry.getValue().isEmpty())
					out.add(entry.getValue().removeFirst());
			return out;
		}
	}

	/** A frame of mixed audio and the station that dominates it (for the squelch tail and meters). */
	public record Mixed(short[] pcm, Signal loudest) {}

	/**
	 * Turns a set of simultaneous frames into the one frame a speaker plays. Each station keeps its own processor
	 * (filters, AGC and level smoothing carry from frame to frame), and FM keeps one more for the hiss of the mix.
	 * Not thread safe: hold the receiver's lock.
	 */
	public static final class Renderer {
		/** A station's processor is let go after this long unheard. */
		static final long IDLE_NANOS = 2_000_000_000L;

		private static final class Track {
			final RadioVoiceProcessor processor = new RadioVoiceProcessor();
			double weight = -1.0D;
			long lastNanos;
		}

		private final Map<Object, Track> tracks = new HashMap<>();
		private final RadioVoiceProcessor fm = new RadioVoiceProcessor();
		private Object doublingPick;
		private int doublingFramesLeft;
		private long random = System.nanoTime() | 1L;

		/**
		 * Mixes {@code signals} (the input frames are left alone) for a speaker with {@code settings}, at
		 * {@code loudness} times each station's level.
		 */
		public Mixed render(List<Signal> signals, RadioVoiceProcessor.Settings settings, double loudness, long nanos) {
			tracks.values().removeIf(t -> nanos - t.lastNanos > IDLE_NANOS);
			if(signals.size() == 1) {
				Signal s = signals.getFirst();
				Track track = track(s, nanos);
				track.weight = -1.0D; // A mix that starts later starts from its full weight.
				short[] pcm = track.processor.process(s.pcm().clone(), settings, s.level() * loudness, s.rasp(), s.pitchShiftHz(), s.splatter(), s.qrm());
				if(s.fm())
					fm.fmFade(pcm, s.fmSignal(), s.level() * loudness, settings.smoothSeconds());
				return new Mixed(pcm, s);
			}
			List<Signal> ranked = new ArrayList<>(signals);
			ranked.sort(Comparator.comparingDouble(Signal::strength).reversed());
			Signal strongest = ranked.getFirst();
			if(!strongest.fm()) { // SSB: everyone is heard, each at the level its own path gives it.
				double[] sum = new double[strongest.pcm().length];
				for(Signal s : ranked)
					add(sum, s, s.splatter(), 1.0D, settings, loudness, nanos);
				return new Mixed(limit(sum), strongest);
			}
			Signal on;
			double garble, bleed;
			double ratio = strongest.fmSignal() <= 0.0D ? 1.0D : ranked.get(1).fmSignal() / strongest.fmSignal();
			if(ratio <= FM_CAPTURE_RATIO) { // Captured: the strong one wins, roughened as the other comes closer.
				on = strongest;
				double closeness = ratio / FM_CAPTURE_RATIO;
				garble = FM_CAPTURE_GARBLE * closeness;
				bleed = FM_CAPTURE_BLEED * closeness * closeness;
				doublingPick = null;
			}
			else { // Doubling: neither captures, and the receiver jumps between them.
				on = doublingPick(ranked);
				garble = FM_DOUBLING_GARBLE;
				bleed = FM_DOUBLING_BLEED;
			}
			double[] sum = new double[on.pcm().length];
			for(Signal s : ranked)
				add(sum, s, s == on ? Math.max(s.splatter(), garble) : s.splatter(), s == on ? 1.0D : bleed, settings, loudness, nanos);
			short[] pcm = limit(sum);
			fm.fmFade(pcm, on.fmSignal(), on.level() * loudness, settings.smoothSeconds());
			return new Mixed(pcm, on);
		}

		/** Who an FM receiver locks onto in doubling, for a few frames at a time: more often the stronger. */
		private Signal doublingPick(List<Signal> ranked) {
			Signal current = null;
			for(Signal s : ranked)
				if(Objects.equals(s.key(), doublingPick))
					current = s;
			if(current != null && --doublingFramesLeft > 0)
				return current;
			double total = 0.0D;
			for(Signal s : ranked)
				total += s.fmSignal() * s.fmSignal();
			double roll = nextRandom() * total;
			Signal pick = ranked.getFirst();
			for(Iterator<Signal> it = ranked.iterator(); it.hasNext(); ) {
				Signal s = it.next();
				roll -= s.fmSignal() * s.fmSignal();
				if(roll <= 0.0D || !it.hasNext()) {
					pick = s;
					break;
				}
			}
			doublingPick = pick.key();
			doublingFramesLeft = 2 + (int)(nextRandom() * 5.0D); // 40 to 120 ms.
			return pick;
		}

		private Track track(Signal s, long nanos) {
			Track track = tracks.computeIfAbsent(s.key(), k -> new Track());
			track.lastNanos = nanos;
			return track;
		}

		/** Processes one station's frame and adds it into {@code sum} at {@code weight}, which glides over 5 ms. */
		private void add(double[] sum, Signal s, double splatter, double weight, RadioVoiceProcessor.Settings settings, double loudness, long nanos) {
			Track track = track(s, nanos);
			short[] pcm = track.processor.process(s.pcm().clone(), settings, s.level() * loudness, s.rasp(), s.pitchShiftHz(), splatter, s.qrm());
			if(track.weight < 0.0D)
				track.weight = weight;
			double glide = RadioVoiceProcessor.coefficient(0.005D);
			for(int i = 0; i < Math.min(sum.length, pcm.length); i++) {
				track.weight = glide * track.weight + (1.0D - glide) * weight;
				sum[i] += pcm[i] / 32768.0D * track.weight;
			}
		}

		private double nextRandom() {
			random ^= random << 13;
			random ^= random >>> 7;
			random ^= random << 17;
			return (random >>> 11) * 0x1.0p-53;
		}
	}

	/** Two voices at full level add past full scale: a soft knee above 0.7 keeps the sum from hard clipping. */
	static short[] limit(double[] sum) {
		short[] out = new short[sum.length];
		for(int i = 0; i < sum.length; i++) {
			double x = sum[i], magnitude = Math.abs(x);
			if(magnitude > 0.7D)
				x = Math.signum(x) * (0.7D + 0.3D * RadioVoiceProcessor.softClip((magnitude - 0.7D) / 0.3D));
			out[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(x * 32767.0D)));
		}
		return out;
	}
}

package com.arrl.radiocraft.common.radio.voice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The squelch tail of an FM receiver: when a signal it was hearing drops, the squelch takes a moment to shut, and in
 * that moment the speaker gives a short burst of static ("kssh"). Short and crisp after a strong signal, longer and
 * louder after a weak one. One per receiver: {@link #heard} is called for every frame played (voice chat thread or
 * game thread), {@link #drain} once a tick on the game thread, which hands out the tail's frames a tick's worth at a
 * time once the signal has been gone for {@link #GAP_NANOS}.
 */
public final class SquelchTail {

	/** How long after its last frame a signal counts as gone: two ticks' worth of frames. */
	public static final long GAP_NANOS = 100_000_000L;
	/** The tail after a full-quieting signal, and after one that only just opened the squelch, in 20 ms frames. */
	public static final int SHORTEST_FRAMES = 6, LONGEST_FRAMES = 10;

	/** A tail due: how strong the signal was, the level it played at, and whose encoder its frames continue. */
	public record Due(double signal, double level, UUID source) {}

	private boolean open = false;
	private long lastFrameNanos;
	private double signal, level;
	private UUID source;
	private long seed = System.nanoTime() | 1L;
	private UUID queuedSource;
	private final ArrayDeque<short[]> queued = new ArrayDeque<>();

	/** A frame of a signal of strength {@code signal} was just played at {@code level}. */
	public synchronized void heard(double signal, double level, UUID source, long nowNanos) {
		open = true;
		lastFrameNanos = nowNanos;
		this.signal = signal;
		this.level = level;
		this.source = source;
		queued.clear(); // A signal back before its tail finished: the squelch never shut.
	}

	/**
	 * The tail frames to play this tick, at most {@code perTick}; empty while the signal is still coming in, or once the
	 * tail has played. {@link #source()} says whose encoder they continue.
	 */
	public synchronized List<short[]> drain(long nowNanos, int perTick) {
		if(open && nowNanos - lastFrameNanos >= GAP_NANOS) {
			open = false;
			queuedSource = source;
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			queued.addAll(frames(signal, level, seed));
		}
		List<short[]> out = new ArrayList<>(perTick);
		for(int i = 0; i < perTick && !queued.isEmpty(); i++)
			out.add(queued.poll());
		return out;
	}

	/** The source of the tail being drained (its frames go through that source's encoder). */
	public synchronized UUID source() {
		return queuedSource;
	}

	/** Frames in the tail after a signal of strength {@code signal}: shorter the stronger it was. */
	public static int tailFrames(double signal) {
		double weak = 1.0D - Math.max(0.0D, Math.min(1.0D, signal / ReceiverMix.FM_QUIETING));
		return SHORTEST_FRAMES + (int)Math.round((LONGEST_FRAMES - SHORTEST_FRAMES) * weak);
	}

	/** How loud the tail is, as a share of the receiver's open-squelch static: louder after a weak signal. */
	public static double tailLevel(double signal) {
		double weak = 1.0D - Math.max(0.0D, Math.min(1.0D, signal / ReceiverMix.FM_QUIETING));
		return 0.55D + 0.45D * weak;
	}

	/**
	 * The tail itself, at output {@code level}: the same softened white noise as a fading FM signal's static
	 * ({@link RadioVoiceProcessor#fmFade}), in at once and dying away over its last half.
	 */
	public static List<short[]> frames(double signal, double level, long seed) {
		int count = tailFrames(signal);
		int frame = RepeaterAudio.FRAME, total = count * frame;
		double amplitude = RadioVoiceProcessor.FM_STATIC * tailLevel(signal) * level * 32767.0D;
		long state = seed | 1L;
		double soft = 0.0D;
		List<short[]> out = new ArrayList<>(count);
		for(int f = 0; f < count; f++) {
			short[] pcm = new short[frame];
			for(int i = 0; i < frame; i++) {
				int n = f * frame + i;
				state ^= state << 13;
				state ^= state >>> 7;
				state ^= state << 17;
				soft = 0.5D * soft + 0.5D * (((state >>> 11) * 0x1.0p-53) * 2.0D - 1.0D);
				double envelope = Math.min(1.0D, n / 96.0D) * Math.min(1.0D, 2.0D * (total - n) / (double)total);
				pcm[i] = (short)Math.max(-32768, Math.min(32767, Math.round(soft * amplitude * envelope)));
			}
			out.add(pcm);
		}
		return out;
	}
}

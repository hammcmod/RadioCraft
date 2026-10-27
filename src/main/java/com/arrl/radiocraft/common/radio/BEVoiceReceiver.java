package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import com.arrl.radiocraft.common.radio.voice.SquelchTail;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link BEVoiceReceiver} is used for interacting with the Simple Voice Chat API to send sound packets being received by a Radio.
 * PCM audio gets re-encoded here and sent on an {@link AudioChannel}.
 * <p>
 * The radio plays through its own speaker, or a better one touching it ({@link #setSpeaker}), and also through every
 * wired speaker on its wire ({@link #setWiredSpeakers}): each of those is its own channel where the speaker stands,
 * with its own processor (filters carry state) and Opus encoder (so the streams do not share encoder state).
 */
public class BEVoiceReceiver implements IVoiceReceiver {

	private LocationalAudioChannel receiveChannel = null;
	private final int x;
	private final int y;
	private final int z;

	private boolean isReceiving;
	private final RadioVoiceProcessor processor = new RadioVoiceProcessor(); // Only touched from the voice chat thread.
	private static boolean warnedNoApi = false;

	/** A wired speaker this radio plays through. */
	public record WiredSpeaker(BlockPos pos, SpeakerKind kind) {}

	private volatile SpeakerKind speaker = SpeakerKind.BUILT_IN; // Set on the game thread, read on the voice thread.
	private volatile List<WiredSpeaker> wiredSpeakers = List.of();
	// Voice thread only: the channel, processor and encoder of each wired speaker, kept while it stays wired.
	private final Map<WiredSpeaker, SpeakerOutput> outputs = new HashMap<>(); // Guarded by itself: the squelch tail plays from the game thread.
	private final SquelchTail tail = new SquelchTail();
	private List<WiredSpeaker> outputsFor = List.of();

	public BEVoiceReceiver(int x, int y, int z) {
		this(x, y, z, false);
	}

	public BEVoiceReceiver(int x, int y, int z, boolean isReceiving) {
		this.isReceiving = isReceiving;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void openChannel(ServerLevel level) {
		receiveChannel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), level, RadiocraftVoicePlugin.API.createPosition(x, y, z));
		if(receiveChannel != null)
			receiveChannel.setDistance(16.0F);
	}

	/** What the radio itself plays through: its own speaker, or a desk or premium speaker touching it. */
	public void setSpeaker(SpeakerKind kind) {
		speaker = kind;
	}

	public SpeakerKind getSpeaker() {
		return speaker;
	}

	/** The wired speakers on the radio's wire; game thread. */
	public void setWiredSpeakers(List<WiredSpeaker> speakers) {
		if(!speakers.equals(wiredSpeakers))
			wiredSpeakers = List.copyOf(speakers);
	}

	public List<WiredSpeaker> getWiredSpeakers() {
		return wiredSpeakers;
	}

	/** Plays the packet's audio at the radio at its own strength, with no noise to sit against. */
	public void receive(AntennaVoicePacket antennaPacket) {
		receive(antennaPacket, antennaPacket.getStrength(), 0.0D);
	}

	/**
	 * Plays the packet's audio at the radio through the receiver's {@link RadioVoiceProcessor}. Does nothing when the
	 * voice chat server API is not available (no voice chat server running, e.g. a GameTest server), so the rest of the
	 * radio path can still be exercised without it.
	 *
	 * @param level Output level, see {@link com.arrl.radiocraft.common.radio.voice.ReceiverMix#voiceLevel}.
	 * @param rasp  Aurora rasp, 0 to 1.
	 */
	public void receive(AntennaVoicePacket antennaPacket, double level, double rasp) {
		receive(antennaPacket, level, rasp, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * As {@link #receive(AntennaVoicePacket, double, double)}, for a signal that is off frequency or has a neighbour
	 * splattering onto it; see {@link RadioVoiceProcessor#process(short[], RadioVoiceProcessor.Settings, double, double, double, double, double)}.
	 */
	public void receive(AntennaVoicePacket antennaPacket, double level, double rasp, double pitchShiftHz, double splatter, double qrm) {
		receive(antennaPacket, level, rasp, pitchShiftHz, splatter, qrm, -1.0D);
	}

	/**
	 * As above, for an FM receiver when {@code fmSignal} (the signal's strength) is 0 or more: the voice stays at
	 * {@code level} and fades into static instead ({@link RadioVoiceProcessor#fmFade}).
	 */
	public void receive(AntennaVoicePacket antennaPacket, double level, double rasp, double pitchShiftHz, double splatter, double qrm, double fmSignal) {
		if(!isReceiving)
			return;
		if(RadiocraftVoicePlugin.API == null) {
			if(!warnedNoApi) {
				warnedNoApi = true;
				Radiocraft.LOGGER.warn("Radio at {},{},{} received audio but the voice chat server API is not available; audio will not be played.", x, y, z);
			}
			return;
		}

		ServerLevel voiceLevel = RadiocraftVoicePlugin.API.fromServerLevel(antennaPacket.getLevel());
		if(receiveChannel == null)
			openChannel(voiceLevel);
		if(receiveChannel == null)
			return;

		RadioVoiceProcessor.Settings settings = RadioVoiceProcessor.currentSettings();
		List<WiredSpeaker> wired = wiredSpeakers;
		short[] clean = wired.isEmpty() ? null : antennaPacket.getRawAudio().clone(); // The radio's processing works in place.

		short[] rawAudio = processor.process(antennaPacket.getRawAudio(), speaker.apply(settings), level, rasp, pitchShiftHz, splatter, qrm);
		if(fmSignal >= 0.0D)
			processor.fmFade(rawAudio, fmSignal, level);
		byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
		receiveChannel.send(opusAudio);
		if(fmSignal >= 0.0D)
			tail.heard(fmSignal, level, antennaPacket.getSourcePlayer(), System.nanoTime());

		synchronized(outputs) {
			syncOutputs(wired);
			if(clean == null)
				return;
			for(WiredSpeaker target : wired) {
				SpeakerOutput output = outputs.computeIfAbsent(target, t -> new SpeakerOutput(voiceLevel, t));
				output.play(clean.clone(), target.kind().apply(settings), level * target.kind().loudness, rasp, pitchShiftHz, splatter, qrm, fmSignal);
			}
		}
	}

	/**
	 * Plays this tick's share of an FM squelch tail, once a signal the radio was playing has dropped
	 * ({@link SquelchTail}): through the radio's speaker and every wired speaker. Game thread, once a tick.
	 */
	public void tickSquelchTail(long gameTime) {
		List<short[]> frames = tail.drain(System.nanoTime(), gameTime % 2 == 0 ? 3 : 2); // 2.5 frames of 20 ms a tick.
		if(frames.isEmpty() || receiveChannel == null || RadiocraftVoicePlugin.API == null)
			return;
		UUID source = tail.source();
		OpusEncoder encoder = RadiocraftVoicePlugin.encodingManager.getOrCreate(source).getEncoder(); // Carries on from the voice.
		for(short[] frame : frames)
			receiveChannel.send(encoder.encode(frame));
		synchronized(outputs) {
			for(Map.Entry<WiredSpeaker, SpeakerOutput> entry : outputs.entrySet())
				for(short[] frame : frames)
					entry.getValue().send(scaled(frame, entry.getKey().kind().loudness));
		}
	}

	private static short[] scaled(short[] frame, double gain) {
		short[] out = new short[frame.length];
		for(int i = 0; i < frame.length; i++)
			out[i] = (short)Math.max(-32768, Math.min(32767, Math.round(frame[i] * gain)));
		return out;
	}

	/** Lets go of the outputs of speakers no longer wired; holding {@link #outputs}. */
	private void syncOutputs(List<WiredSpeaker> wired) {
		if(wired == outputsFor)
			return;
		outputsFor = wired;
		outputs.entrySet().removeIf(entry -> {
			if(wired.contains(entry.getKey()))
				return false;
			entry.getValue().close();
			return true;
		});
	}

	public boolean isReceiving() {
		return isReceiving;
	}

	public void setReceiving(boolean value) {
		isReceiving = value;
	}

	/** One wired speaker's audio: a channel where it stands, heard as far as its kind carries. */
	private static final class SpeakerOutput {
		private final LocationalAudioChannel channel;
		private final RadioVoiceProcessor processor = new RadioVoiceProcessor();
		private final OpusEncoder encoder;

		SpeakerOutput(ServerLevel level, WiredSpeaker speaker) {
			BlockPos pos = speaker.pos();
			channel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), level,
					RadiocraftVoicePlugin.API.createPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
			if(channel != null)
				channel.setDistance(speaker.kind().distance);
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		}

		void play(short[] pcm, RadioVoiceProcessor.Settings settings, double level, double rasp, double pitchShiftHz, double splatter, double qrm, double fmSignal) {
			if(channel == null)
				return;
			short[] audio = processor.process(pcm, settings, level, rasp, pitchShiftHz, splatter, qrm);
			if(fmSignal >= 0.0D)
				processor.fmFade(audio, fmSignal, level);
			channel.send(encoder.encode(audio));
		}

		/** Sends a frame as it is (the squelch tail, already at its level). */
		void send(short[] pcm) {
			if(channel != null)
				channel.send(encoder.encode(pcm));
		}

		void close() {
			encoder.close();
		}
	}
}

package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.voice.RadioVoiceProcessor;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import com.arrl.radiocraft.common.radio.voice.SpeakerKind;
import com.arrl.radiocraft.common.radio.voice.SquelchTail;
import com.arrl.radiocraft.common.radio.voice.VoiceMix;
import com.arrl.radiocraft.compat.sable.Sublevels;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

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
 * <p>
 * Stations heard at the same time are mixed into one stream ({@link VoiceMix}): SSB adds them up, FM captures the
 * stronger. Everything here runs under this receiver's lock: voice arrives on the voice chat thread, and parrot echoes
 * and the squelch tail on the game thread.
 */
public class BEVoiceReceiver implements IVoiceReceiver {

	private LocationalAudioChannel receiveChannel = null;
	// Where the radio's Sable ship stands (GROUND when it is not on one): set by the game thread each tick, read here.
	private volatile Sublevels.Frame frame = Sublevels.Frame.GROUND;
	private final int x;
	private final int y;
	private final int z;

	private boolean isReceiving;
	private final VoiceMix.Aligner aligner = new VoiceMix.Aligner();
	private final VoiceMix.Renderer renderer = new VoiceMix.Renderer();
	private OpusEncoder encoder; // This receiver's own: an encoder per talker would be shared by every radio hearing them.
	private static boolean warnedNoApi = false;

	/** A wired speaker this radio plays through. */
	public record WiredSpeaker(BlockPos pos, SpeakerKind kind) {}

	private volatile SpeakerKind speaker = SpeakerKind.BUILT_IN; // Set on the game thread, read on the voice thread.
	private volatile List<WiredSpeaker> wiredSpeakers = List.of();
	// Voice thread only: the channel, processor and encoder of each wired speaker, kept while it stays wired.
	private final Map<WiredSpeaker, SpeakerOutput> outputs = new HashMap<>();
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

	/** Where the radio's Sable ship stands this tick ({@link Sublevels.Frame#GROUND} off a ship). Game thread. */
	public void setFrame(Sublevels.Frame frame) {
		this.frame = frame;
	}

	/** Moves {@code channel} to where {@code local} (a point in the radio's own block grid) really is, on a ship. */
	static void follow(@org.jetbrains.annotations.Nullable LocationalAudioChannel channel, Sublevels.Frame frame, double x, double y, double z) {
		if(channel == null || frame.isGround())
			return;
		Vec3 at = frame.apply(new Vec3(x, y, z));
		channel.updateLocation(RadiocraftVoicePlugin.API.createPosition(at.x, at.y, at.z));
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
		VoiceMix.Signal signal = new VoiceMix.Signal(VoiceMix.key(antennaPacket.getSourcePlayer(), antennaPacket.getSource()),
				antennaPacket.getSourcePlayer(), antennaPacket.getRawAudio(), level, rasp, pitchShiftHz, splatter, qrm, fmSignal);
		RadioVoiceProcessor.Settings settings = RadioVoiceProcessor.currentSettings();
		List<WiredSpeaker> wired = wiredSpeakers;
		long now = System.nanoTime();
		synchronized(this) {
			if(receiveChannel == null)
				openChannel(voiceLevel);
			if(receiveChannel == null)
				return;
			List<VoiceMix.Signal> slot = aligner.offer(signal, now);
			if(slot.isEmpty())
				return; // Another station is leading: this frame goes out mixed with its next one.

			VoiceMix.Mixed mixed = renderer.render(slot, speaker.apply(settings), 1.0D, now);
			follow(receiveChannel, this.frame, x, y, z);
			receiveChannel.send(encoder().encode(mixed.pcm()));
			VoiceMix.Signal loudest = mixed.loudest();
			if(loudest.fm())
				tail.heard(loudest.fmSignal(), loudest.level(), loudest.source(), now);

			syncOutputs(wired);
			for(WiredSpeaker target : wired) {
				SpeakerOutput output = outputs.computeIfAbsent(target, t -> new SpeakerOutput(voiceLevel, t));
				output.play(slot, target.kind().apply(settings), target.kind().loudness, this.frame, now);
			}
		}
	}

	private OpusEncoder encoder() {
		if(encoder == null)
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		return encoder;
	}

	/** Lets go of the encoders; the receiver makes new ones if it plays again. */
	public synchronized void close() {
		if(encoder != null) {
			encoder.close();
			encoder = null;
		}
		outputs.values().forEach(SpeakerOutput::close);
		outputs.clear();
		outputsFor = List.of();
	}

	/**
	 * Plays this tick's share of an FM squelch tail, once a signal the radio was playing has dropped
	 * ({@link SquelchTail}): through the radio's speaker and every wired speaker. Game thread, once a tick.
	 */
	public void tickSquelchTail(long gameTime) {
		List<short[]> frames = tail.drain(System.nanoTime(), gameTime % 2 == 0 ? 3 : 2); // 2.5 frames of 20 ms a tick.
		if(frames.isEmpty() || receiveChannel == null || RadiocraftVoicePlugin.API == null)
			return;
		synchronized(this) {
			Sublevels.Frame ship = this.frame;
			follow(receiveChannel, ship, x, y, z);
			OpusEncoder encoder = encoder(); // Carries on from the voice.
			for(short[] frame : frames)
				receiveChannel.send(encoder.encode(frame));
			for(Map.Entry<WiredSpeaker, SpeakerOutput> entry : outputs.entrySet()) {
				entry.getValue().follow(ship);
				for(short[] frame : frames)
					entry.getValue().send(scaled(frame, entry.getKey().kind().loudness));
			}
		}
	}

	private static short[] scaled(short[] frame, double gain) {
		short[] out = new short[frame.length];
		for(int i = 0; i < frame.length; i++)
			out[i] = (short)Math.max(-32768, Math.min(32767, Math.round(frame[i] * gain)));
		return out;
	}

	/** Lets go of the outputs of speakers no longer wired; holding the lock. */
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
		private final BlockPos pos;
		private final VoiceMix.Renderer renderer = new VoiceMix.Renderer();
		private final OpusEncoder encoder;

		SpeakerOutput(ServerLevel level, WiredSpeaker speaker) {
			pos = speaker.pos();
			channel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), level,
					RadiocraftVoicePlugin.API.createPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
			if(channel != null)
				channel.setDistance(speaker.kind().distance);
			encoder = RadiocraftVoicePlugin.API.createEncoder();
		}

		/** A wired speaker rides the same ship as its radio. */
		void follow(Sublevels.Frame frame) {
			BEVoiceReceiver.follow(channel, frame, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
		}

		void play(List<VoiceMix.Signal> slot, RadioVoiceProcessor.Settings settings, double loudness, Sublevels.Frame ship, long nanos) {
			if(channel == null)
				return;
			follow(ship);
			channel.send(encoder.encode(renderer.render(slot, settings, loudness, nanos).pcm()));
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

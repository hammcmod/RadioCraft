package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class AntennaVoicePacket implements IAntennaPacket {

	private final de.maxhenkel.voicechat.api.ServerLevel level; // This is a voice api ServerLevel not a minecraft one.
	private final short[] rawAudio;
	private final Band band;
	private final float frequency;
	private double strength;
	private final IAntenna source;
	private final UUID sourcePlayer;
	private final float ctcssTone; // Sub-audible tone under the voice, 0 for none (see Ctcss).


	public AntennaVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequency, double strength, IAntenna source, UUID sourcePlayer) {
		this(level, rawAudio, band, frequency, strength, source, sourcePlayer, 0.0F);
	}

	public AntennaVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequency, double strength, IAntenna source, UUID sourcePlayer, float ctcssTone) {
		this.ctcssTone = ctcssTone;
		this.level = level;
		this.rawAudio = rawAudio;
		this.band = band;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
		this.sourcePlayer = sourcePlayer;
	}

	@Override
	public Band getBand() {
		return band;
	}

	@Override
	public float getFrequency() {
		return frequency;
	}

	@Override
	public double getStrength() {
		return strength;
	}

	@Override
	public IAntenna getSource() {
		return source;
	}

	public ServerLevel getLevel() {
		return (ServerLevel)level.getServerLevel();
	}

	public short[] getRawAudio() {
		return rawAudio;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}

	public UUID getSourcePlayer() {
		return sourcePlayer;
	}

	/** The CTCSS tone the transmitter sent, 0 for none. */
	public float getCtcssTone() {
		return ctcssTone;
	}

}

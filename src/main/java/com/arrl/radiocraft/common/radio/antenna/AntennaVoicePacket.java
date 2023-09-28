package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import de.maxhenkel.voicechat.api.ServerLevel;

import java.util.UUID;

public class AntennaVoicePacket implements IAntennaPacket {

	private final ServerLevel level; // This is a voice api ServerLevel not a minecraft one.
	private final short[] rawAudio;
	private final int wavelength;
	private final int frequency;
	private double strength;
	private final IAntenna source;
	private final UUID sourcePlayer;


	public AntennaVoicePacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, double strength, IAntenna source, UUID sourcePlayer) {
		this.level = level;
		this.rawAudio = rawAudio;
		this.wavelength = wavelength;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
		this.sourcePlayer = sourcePlayer;
	}

	@Override
	public int getWavelength() {
		return wavelength;
	}

	@Override
	public int getFrequency() {
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
		return level;
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

}

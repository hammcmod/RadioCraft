package com.arrl.radiocraft.common.radio.antenna;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public class AntennaNetworkPacket {

	private final ServerLevel level; // This is a voice api ServerLevel not a minecraft one.
	private final short[] rawAudio;
	private final int wavelength;
	private final int frequency;
	private double strength;
	private final BlockPos source;
	private final UUID sourcePlayer;


	public AntennaNetworkPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, double strength, BlockPos source, UUID sourcePlayer) {
		this.level = level;
		this.rawAudio = rawAudio;
		this.wavelength = wavelength;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
		this.sourcePlayer = sourcePlayer;
	}

	public ServerLevel getLevel() {
		return level;
	}

	public short[] getRawAudio() {
		return rawAudio;
	}

	public int getWavelength() {
		return wavelength;
	}

	public int getFrequency() {
		return frequency;
	}

	public double getStrength() {
		return strength;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}

	public BlockPos getSource() {
		return source;
	}

	public UUID getSourcePlayer() {
		return sourcePlayer;
	}

}

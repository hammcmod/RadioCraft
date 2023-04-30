package com.arrl.radiocraft.common.radio.voice;

import net.minecraft.core.BlockPos;

public class AntennaNetworkPacket {

	private final short[] rawAudio;
	private final int wavelength;
	private final int frequency;
	private float strength;
	private final BlockPos source;


	public AntennaNetworkPacket(short[] rawAudio, int wavelength, int frequency, float strength, BlockPos source) {
		this.rawAudio = rawAudio;
		this.wavelength = wavelength;
		this.frequency = frequency;
		this.strength = strength;
		this.source = source;
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

	public float getStrength() {
		return strength;
	}

	public void setStrength(float strength) {
		this.strength = strength;
	}

	public BlockPos getSource() {
		return source;
	}

}

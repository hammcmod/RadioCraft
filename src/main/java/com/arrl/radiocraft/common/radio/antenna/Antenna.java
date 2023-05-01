package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.AntennaNetwork;
import com.arrl.radiocraft.common.radio.voice.AntennaNetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

public class Antenna<T extends AntennaData> implements INBTSerializable<CompoundTag> {

	public final IAntennaType<T> type;
	public final T data;
	public final BlockPos pos;

	private AntennaNetwork network = null;

	public Antenna(IAntennaType<T> type, BlockPos pos, T data) {
		this.type = type;
		this.data = data;
		this.pos = pos;
	}

	public Antenna(IAntennaType<T> type, BlockPos pos) {
		this.type = type;
		this.data = type.getDefaultData();
		this.pos = pos;
	}

	private AntennaNetworkPacket getTransmitAudioPacket(short[] rawAudio, int wavelength, int frequency, BlockPos destination) {
		AntennaNetworkPacket networkPacket = new AntennaNetworkPacket(rawAudio, wavelength, frequency, 1.0F, pos);
		type.applyTransmitStrength(networkPacket, data, destination);
		return networkPacket;
	}

	public void processReceiveAudioPacket(AntennaNetworkPacket packet) {
		type.applyReceiveStrength(packet, data, pos);
	}

	public void setNetwork(AntennaNetwork network) {
		if(this.network != null)
			this.network.removeAntenna(pos);
		network.addAntenna(pos, this);
		this.network = network;
	}

	public void transmitAudioPacket(short[] rawAudio, int wavelength, int frequency) {
		if(network != null) {
			Map<BlockPos, Antenna<?>> antennas = network.allAntennas();
			for(BlockPos targetPos : antennas.keySet()) {
				AntennaNetworkPacket antennaPacket = getTransmitAudioPacket(rawAudio, wavelength, frequency, pos);
				if(antennaPacket.getStrength() > 0.02F) // Cutoff at 0.02 strength for performance.
					antennas.get(targetPos).processReceiveAudioPacket(antennaPacket);
			}
		}
	}

	@Override
	public CompoundTag serializeNBT() {
		return data.serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		data.deserializeNBT(nbt);
	}

}

package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.radio.AntennaNetwork;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
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

	private AntennaNetworkPacket getTransmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, BlockPos destination) {
		short[] rawAudioCopy = rawAudio.clone(); // Use a copy as every antenna modifies this differently.
		AntennaNetworkPacket networkPacket = new AntennaNetworkPacket(level, rawAudioCopy, wavelength, frequency, 1.0F, pos);
		type.applyTransmitStrength(networkPacket, data, destination);
		return networkPacket;
	}

	public void processReceiveAudioPacket(AntennaNetworkPacket packet) {
		// level#getBlockEntity is thread sensitive for some unknown reason.
		if(network.getLevel().getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof AntennaBlockEntity be) {
			type.applyReceiveStrength(packet, data, pos);
			be.receiveAudioPacket(packet);
		}
	}

	public void setNetwork(AntennaNetwork network) {
		if(this.network != null)
			this.network.removeAntenna(pos);
		network.addAntenna(pos, this);
		this.network = network;
	}

	public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency) {
		if(network != null) {
			Map<BlockPos, Antenna<?>> antennas = network.allAntennas();
			for(BlockPos targetPos : antennas.keySet()) {
				if(!targetPos.equals(pos)) {
					AntennaNetworkPacket antennaPacket = getTransmitAudioPacket(level, rawAudio, wavelength, frequency, pos);
					if(antennaPacket.getStrength() > 0.02F) // Cutoff at 0.02 strength for performance.
						antennas.get(targetPos).processReceiveAudioPacket(antennaPacket);
				}
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

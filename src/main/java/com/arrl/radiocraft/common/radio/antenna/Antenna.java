package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.radio.AntennaNetwork;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Antenna<T extends AntennaData> implements INBTSerializable<CompoundTag> {

	public final IAntennaType<T> type;
	public final T data;
	public final BlockPos pos;

	private AntennaNetwork network = null;

	private final Map<BlockPos, Double> ssbSendCache = new HashMap<>();
	private final Map<BlockPos, Double> ssbReceiveCache = new HashMap<>();

	private final Map<BlockPos, Double> cwSendCache = new HashMap<>();
	private final Map<BlockPos, Double> cwReceiveCache = new HashMap<>();

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

	public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, UUID sourcePlayer) {
		if(network != null) {
			Map<BlockPos, Antenna<?>> antennas = network.allAntennas();
			for(BlockPos targetPos : antennas.keySet()) {
				if(!targetPos.equals(pos)) {
					AntennaVoicePacket antennaPacket = getTransmitAudioPacket(level, rawAudio, wavelength, frequency, pos, sourcePlayer);
					if(antennaPacket.getStrength() > 0.02F) // Cutoff at 0.02 strength for performance.
						antennas.get(targetPos).processReceiveAudioPacket(antennaPacket);
				}
			}
		}
	}

	private AntennaVoicePacket getTransmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, BlockPos destination, UUID sourcePlayer) {
		AntennaVoicePacket networkPacket = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequency, 1.0F, pos, sourcePlayer);

		if(ssbSendCache.containsKey(destination))
			networkPacket.setStrength(ssbSendCache.get(destination));
		else {
			networkPacket.setStrength(type.getSSBTransmitStrength(networkPacket, data, destination));
			ssbSendCache.put(destination, networkPacket.getStrength());
		}

		return networkPacket;
	}

	public void processReceiveAudioPacket(AntennaVoicePacket packet) {
		// level#getBlockEntity is thread sensitive for some unknown reason.
		if(network.getLevel().getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof AntennaBlockEntity be) {

			if(ssbReceiveCache.containsKey(packet.getSource()))
				packet.setStrength(ssbReceiveCache.get(packet.getSource()));
			else {
				packet.setStrength(type.getReceiveStrength(packet, data, pos));
				ssbReceiveCache.put(packet.getSource(), packet.getStrength());
			}

			be.receiveAudioPacket(packet);
		}
	}

	public void setNetwork(AntennaNetwork network) {
		if(this.network != null)
			this.network.removeAntenna(pos);
		network.addAntenna(pos, this);
		this.network = network;
	}

	public void transmitMorsePacket(net.minecraft.server.level.ServerLevel level, int wavelength, int frequency) {
		if(network != null) {
			Map<BlockPos, Antenna<?>> antennas = network.allAntennas();
			for(BlockPos targetPos : antennas.keySet()) {
				if(!targetPos.equals(pos)) {
					AntennaMorsePacket antennaPacket = getTransmitMorsePacket(level, wavelength, frequency, pos);
					if(antennaPacket.getStrength() > 0.02F) // Cutoff at 0.02 strength for performance.
						antennas.get(targetPos).processReceiveMorsePacket(antennaPacket);
				}
			}
		}
	}

	private AntennaMorsePacket getTransmitMorsePacket(net.minecraft.server.level.ServerLevel level, int wavelength, int frequency, BlockPos destination) {
		AntennaMorsePacket networkPacket = new AntennaMorsePacket(level, wavelength, frequency, 1.0F, pos);

		if(cwSendCache.containsKey(destination))
			networkPacket.setStrength(cwSendCache.get(destination));
		else {
			networkPacket.setStrength(type.getCWTransmitStrength(networkPacket, data, destination));
			cwSendCache.put(destination, networkPacket.getStrength());
		}

		return networkPacket;
	}

	public void processReceiveMorsePacket(AntennaMorsePacket packet) {
		if(network.getLevel().getBlockEntity(pos) instanceof AntennaBlockEntity be) {
			if(cwReceiveCache.containsKey(packet.getSource()))
				packet.setStrength(cwReceiveCache.get(packet.getSource()));
			else {
				packet.setStrength(type.getReceiveStrength(packet, data, pos));
				cwReceiveCache.put(packet.getSource(), packet.getStrength());
			}

			be.receiveMorsePacket(packet);
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

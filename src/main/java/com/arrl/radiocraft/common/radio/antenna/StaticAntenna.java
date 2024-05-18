package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.benetworks.power.AntennaNetworkObject;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

/**
 * {@link StaticAntenna} represents an Antenna which never moves, meaning the receiving and sending values can always be
 * cached.
 */
public class StaticAntenna<T extends AntennaData> implements IAntenna, INBTSerializable<CompoundTag> {

	public final IAntennaType<T> type;
	public final T data;
	public final BlockPos pos;

	private AntennaNetwork network = null;

	private final Map<BlockPos, Double> ssbSendCache = new HashMap<>();
	private final Map<BlockPos, Double> ssbReceiveCache = new HashMap<>();

	private final Map<BlockPos, Double> cwSendCache = new HashMap<>();
	private final Map<BlockPos, Double> cwReceiveCache = new HashMap<>();

	public StaticAntenna(IAntennaType<T> type, BlockPos pos, T data) {
		this.type = type;
		this.data = data;
		this.pos = pos;
	}

	public StaticAntenna(IAntennaType<T> type, BlockPos pos) {
		this.type = type;
		this.data = type.getDefaultData();
		this.pos = pos;
	}

	@Override
	public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, UUID sourcePlayer) {
		if(network != null) {
			Set<IAntenna> antennas = network.allAntennas();

			for(IAntenna antenna : antennas) {
				if(antenna != this) {
					AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequency, 1.0F, this, sourcePlayer);

					// Calculate the strength this packet should be sent at.
					BlockPos destination = antenna.getBlockPos();
					if(ssbSendCache.containsKey(antenna.getBlockPos())) // Recalculate if value wasn't already present.
						packet.setStrength(ssbSendCache.get(destination));
					else {
						packet.setStrength(type.getTransmitEfficiency(packet, data, destination, false) * SWRHelper.getEfficiencyMultiplier(getSWR(wavelength)));
						ssbSendCache.put(destination, packet.getStrength());
					}

					antenna.receiveAudioPacket(packet);
				}
			}
		}
	}

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {
		AntennaNetworkObject obj = getNetworkObj();
		if(obj != null) {
			BlockPos source = packet.getSource().getBlockPos();
			if(ssbReceiveCache.containsKey(source))
				packet.setStrength(packet.getStrength() * ssbReceiveCache.get(source));
			else {
				packet.setStrength(type.getReceiveEfficiency(packet, data, pos));
				ssbReceiveCache.put(source, packet.getStrength());
			}

			obj.receiveAudioPacket(packet);
		}
	}

	@Override
	public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequency) {
		if(network != null) {
			Set<IAntenna> antennas = network.allAntennas();
			for(IAntenna antenna : antennas) {
				if(antenna != this) {
					AntennaCWPacket packet = new AntennaCWPacket(level, buffers, wavelength, frequency, 1.0F, this);

					// Calculate the strength this packet should be sent at.
					BlockPos destination = antenna.getBlockPos();
					if(cwSendCache.containsKey(destination))
						packet.setStrength(cwSendCache.get(destination));
					else {
						packet.setStrength(type.getTransmitEfficiency(packet, data, destination, true));
						cwSendCache.put(destination, packet.getStrength());
					}

					antenna.receiveCWPacket(packet);
				}
			}
		}
	}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {
		AntennaNetworkObject obj = getNetworkObj();

		if(obj != null) {
			BlockPos source = packet.getSource().getBlockPos();
			if(cwReceiveCache.containsKey(source))
				packet.setStrength(cwReceiveCache.get(source));
			else {
				packet.setStrength(type.getReceiveEfficiency(packet, data, pos));
				cwReceiveCache.put(source, packet.getStrength());
			}

			obj.receiveCWPacket(packet);
		}
	}

	public AntennaNetworkObject getNetworkObj() {
		return (AntennaNetworkObject)IBENetworks.getObject(network.getLevel(), pos);
	}

	@Override
	public BlockPos getBlockPos() {
		return pos;
	}

	@Override
	public CompoundTag serializeNBT() {
		return data.serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		data.deserializeNBT(nbt);
	}

	public void setNetwork(AntennaNetwork network) {
		if(this.network != null)
			this.network.removeAntenna(this);
		network.addAntenna(this);
		this.network = network;
	}

	public double getSWR(int wavelength) {
		return type.getSWR(data, wavelength);
	}

}

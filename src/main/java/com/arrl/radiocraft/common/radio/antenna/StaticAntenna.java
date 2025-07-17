package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link StaticAntenna} represents an Antenna which never moves, meaning the receiving and sending values can always be
 * cached.
 */
public class StaticAntenna<T extends AntennaData> implements IAntenna, INBTSerializable<CompoundTag> {

	public final IAntennaType<T> type;
	public final T data;
	public final AtomicReference<AntennaPos> pos = new AtomicReference<>();
	public final Level level;

	private AntennaNetwork network = null;

	//Disabled due to numerous bugs regarding the complete lack of cache invalidation
//	private final Map<BlockPos, Double> ssbSendCache = new HashMap<>();
//	private final Map<BlockPos, Double> ssbReceiveCache = new HashMap<>();
//
//	private final Map<BlockPos, Double> cwSendCache = new HashMap<>();
//	private final Map<BlockPos, Double> cwReceiveCache = new HashMap<>();

	public StaticAntenna(IAntennaType<T> type, BlockPos pos, Level level, T data) {
		this.type = type;
		this.data = data;
		this.pos.set(new AntennaPos(pos, level));
		this.level = level;
	}

	public StaticAntenna(IAntennaType<T> type, BlockPos pos, Level level) {
		this.type = type;
		this.data = type.getDefaultData();
		this.pos.set(new AntennaPos(pos, level));
		this.level = level;
	}

	@Override
	public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequencyKiloHertz, UUID sourcePlayer) {
		if(network != null) {
			Set<IAntenna> antennas = network.allAntennas();

			for(IAntenna antenna : antennas) {
				if(antenna != this) {
					AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequencyKiloHertz, 1.0F, this, sourcePlayer);

					// Calculate the strength this packet should be sent at.
					AntennaPos destination = antenna.getAntennaPos();
//					if(ssbSendCache.containsKey(antenna.getBlockPos())) // Recalculate if value wasn't already present.
//						packet.setStrength(ssbSendCache.get(destination));
//					else {
						packet.setStrength(type.getTransmitEfficiency(packet, data, destination.position(), false) * SWRHelper.getEfficiencyMultiplier(getSWR(wavelength)));
//						ssbSendCache.put(destination, packet.getStrength());
//					}

					antenna.receiveAudioPacket(packet);
				}
			}
		}
	}

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {
		AntennaNetworkObject obj = getNetworkObj();
		if(obj != null) {
//			BlockPos source = packet.getSource().getBlockPos();
//			if(ssbReceiveCache.containsKey(source))
//				packet.setStrength(packet.getStrength() * ssbReceiveCache.get(source));
//			else {
				packet.setStrength(type.getReceiveEfficiency(packet, data, pos.get().position()));
//				ssbReceiveCache.put(source, packet.getStrength());
//			}

			obj.receiveAudioPacket(packet);
		}
	}

	@Override
	public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequencyKiloHertz) {
		if(network != null) {
			Set<IAntenna> antennas = network.allAntennas();
			for(IAntenna antenna : antennas) {
				if(antenna != this) {
					AntennaCWPacket packet = new AntennaCWPacket(level, buffers, wavelength, frequencyKiloHertz, 1.0F, this);

					// Calculate the strength this packet should be sent at.
					AntennaPos destination = antenna.getAntennaPos();
//					if(cwSendCache.containsKey(destination))
//						packet.setStrength(cwSendCache.get(destination));
//					else {
						packet.setStrength(type.getTransmitEfficiency(packet, data, destination.position(), true));
//						cwSendCache.put(destination, packet.getStrength());
//					}

					antenna.receiveCWPacket(packet);
				}
			}
		}
	}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {
		AntennaNetworkObject obj = getNetworkObj();

		if(obj != null) {
//			BlockPos source = packet.getSource().getBlockPos();
//			if(cwReceiveCache.containsKey(source))
//				packet.setStrength(cwReceiveCache.get(source));
//			else {
				packet.setStrength(type.getReceiveEfficiency(packet, data, pos.get().position()));
//				cwReceiveCache.put(source, packet.getStrength());
//			}

			obj.receiveCWPacket(packet);
		}
	}

	public AntennaNetworkObject getNetworkObj() {
		//TODO remove when confirmed seeing the old implementation inline is not useful for reference
		//return (AntennaNetworkObject)IBENetworks.getObject(network.getLevel(), pos);
		AntennaPos p = this.getAntennaPos();
		return (AntennaNetworkObject)IBENetworks.getObject(p.level(), p.position());
	}

	@Override
	public AntennaPos getAntennaPos() {
		return pos.get();
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

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		return data.serializeNBT(provider);
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		data.deserializeNBT(provider, nbt);
	}
}

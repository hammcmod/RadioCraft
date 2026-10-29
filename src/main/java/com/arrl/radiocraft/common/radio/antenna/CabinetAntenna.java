package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.CabinetAntennaType;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A transmit-only antenna built into a block that is its own station (the commercial radio module): it sits on the
 * antenna network of its band and sends what the block gives it with {@link CabinetAntennaType}'s propagation, line of
 * sight and skip alike. It hears nothing.
 */
public class CabinetAntenna implements IAntenna {

	/** How long the strengths {@link #transmitAudioFrames} works out are kept, in ticks. */
	public static final long REACH_TICKS = 20L;

	private final AntennaPos pos;
	@Nullable
	private AntennaNetwork network;
	private Map<IAntenna, Double> reach = Map.of();
	private long reachAt = Long.MIN_VALUE;
	@Nullable
	private Band reachBand;
	private float reachHertz;

	public CabinetAntenna(Level level, BlockPos pos) {
		this.pos = new AntennaPos(pos.immutable(), level);
	}

	/** Puts this on the network of {@code band} (HF or VHF), moving it if it was on the other. */
	public void register(Band band) {
		AntennaNetwork wanted = AntennaNetworkManager.getNetwork(band.isVhf() ? AntennaNetworkManager.VHF_ID : AntennaNetworkManager.HF_ID);
		if(wanted == network)
			return;
		unregister();
		wanted.addAntenna(this);
		reachAt = Long.MIN_VALUE;
		network = wanted;
	}

	public void unregister() {
		if(network != null) {
			network.removeAntenna(this);
			network = null;
		}
	}

	private Set<IAntenna> others() {
		AntennaNetwork current = network;
		if(current == null)
			return Set.of();
		Set<IAntenna> all = current.allAntennas();
		Set<IAntenna> copy;
		synchronized(all) {
			copy = new HashSet<>(all);
		}
		copy.remove(this);
		return copy;
	}

	@Override
	public void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer) {
		TransmitterLog.record(this, pos, band, frequencyHertz); // On the air: direction finders can hear it.
		for(IAntenna antenna : others()) {
			AntennaPos there = antenna.getAntennaPos();
			if(there == null || there.level() != pos.level())
				continue;
			AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), band, frequencyHertz, 1.0F, this, sourcePlayer, 0.0F);
			packet.setDestination(antenna);
			packet.setStrength(CabinetAntennaType.INSTANCE.getTransmitEfficiency(packet, new EmptyAntennaData(), there.position(), false));
			if(packet.getStrength() > 0.0D)
				antenna.receiveAudioPacket(packet);
		}
	}

	/**
	 * Sends a tick's {@code frames} together: how strongly each antenna hears this one is worked out once for them all,
	 * and kept for {@link #REACH_TICKS} (the bands and the terrain change slowly), so a world full of stations sending
	 * every tick stays cheap. An antenna that has left the network since is skipped, and a new one is heard from the
	 * next time the strengths are worked out.
	 */
	public void transmitAudioFrames(de.maxhenkel.voicechat.api.ServerLevel level, List<short[]> frames, Band band, float frequencyHertz, UUID sourcePlayer) {
		if(frames.isEmpty())
			return;
		TransmitterLog.record(this, pos, band, frequencyHertz); // On the air: direction finders can hear it.
		long now = pos.level().getGameTime();
		if(reachAt == Long.MIN_VALUE || now - reachAt >= REACH_TICKS || now < reachAt || band != reachBand || frequencyHertz != reachHertz) {
			Map<IAntenna, Double> fresh = new HashMap<>();
			for(IAntenna antenna : others()) {
				AntennaPos there = antenna.getAntennaPos();
				if(there == null || there.level() != pos.level())
					continue;
				AntennaVoicePacket packet = new AntennaVoicePacket(level, frames.getFirst(), band, frequencyHertz, 1.0F, this, sourcePlayer, 0.0F);
				packet.setDestination(antenna);
				double strength = CabinetAntennaType.INSTANCE.getTransmitEfficiency(packet, new EmptyAntennaData(), there.position(), false);
				if(strength > 0.0D)
					fresh.put(antenna, strength);
			}
			reach = fresh;
			reachAt = now;
			reachBand = band;
			reachHertz = frequencyHertz;
		}
		AntennaNetwork current = network;
		if(current == null)
			return;
		Set<IAntenna> all = current.allAntennas();
		for(Map.Entry<IAntenna, Double> entry : reach.entrySet()) {
			boolean present;
			synchronized(all) {
				present = all.contains(entry.getKey());
			}
			if(!present)
				continue;
			for(short[] frame : frames) {
				AntennaVoicePacket packet = new AntennaVoicePacket(level, frame.clone(), band, frequencyHertz, 1.0F, this, sourcePlayer, 0.0F);
				packet.setDestination(entry.getKey());
				packet.setStrength(entry.getValue());
				entry.getKey().receiveAudioPacket(packet);
			}
		}
	}

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {}

	@Override
	public void transmitCWPacket(ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {}

	@Override
	public AntennaPos getAntennaPos() {
		return pos;
	}

	@Override
	public IAntennaType<? extends AntennaData> getType() {
		return CabinetAntennaType.INSTANCE;
	}

	@Override
	public AntennaData getData() {
		return new EmptyAntennaData();
	}

	@Override
	public Player getPlayer() {
		return null;
	}
}

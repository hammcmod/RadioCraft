package com.arrl.radiocraft.common.radio.antenna;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.antenna.data.RubberDuckyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.RubberDuckyAntennaType;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The antenna socket of a radio with nothing on its coax. A real rig with no antenna still leaks a little RF out of the
 * connector and picks a little up, so a radio switched on without an antenna works, but only over a few blocks
 * ({@link RadiocraftServerConfig#BARE_CONNECTOR_RANGE}), both ways. The radio registers one of these on the antenna
 * network of its band while it is on and has no antenna, and drops it as soon as one is connected.
 */
public class BareConnectorAntenna implements IAntenna {

	private final RadioBlockEntity radio;
	private final AntennaPos pos;
	private ResourceLocation networkId = null;

	public BareConnectorAntenna(RadioBlockEntity radio) {
		this.radio = radio;
		this.pos = new AntennaPos(radio.getBlockPos(), radio.getLevel());
	}

	/** Strength left after {@code distance} blocks: full at the radio, nothing at the configured range. */
	public static double strengthAt(double distance) {
		double range = Math.max(1.0D, RadiocraftServerConfig.BARE_CONNECTOR_RANGE.get());
		return Math.max(0.0D, 1.0D - distance / range);
	}

	/** Puts this on the network of {@code band} (HF or VHF), moving it if the radio changed band; null takes it off. */
	public void register(Band band) {
		ResourceLocation wanted = band == null ? null : band.isVhf() ? AntennaNetworkManager.VHF_ID : AntennaNetworkManager.HF_ID;
		if(wanted != null && wanted.equals(networkId))
			return;
		unregister();
		if(wanted != null) {
			AntennaNetworkManager.getNetwork(wanted).addAntenna(this);
			networkId = wanted;
		}
	}

	public void unregister() {
		if(networkId != null) {
			AntennaNetworkManager.getNetwork(networkId).removeAntenna(this);
			networkId = null;
		}
	}

	public boolean isRegistered() {
		return networkId != null;
	}

	private Set<IAntenna> others() {
		if(networkId == null)
			return Set.of();
		Set<IAntenna> all = AntennaNetworkManager.getNetwork(networkId).allAntennas();
		synchronized(all) {
			Set<IAntenna> copy = new HashSet<>(all);
			copy.remove(this);
			return copy;
		}
	}

	private double distanceTo(IAntenna other) {
		AntennaPos there = other.getAntennaPos();
		if(there == null || there.level() != pos.level())
			return Double.MAX_VALUE;
		return Math.sqrt(there.position().distSqr(pos.position()));
	}

	@Override
	public void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer) {
		transmitAudioPacket(level, rawAudio, band, frequencyHertz, sourcePlayer, 0.0F);
	}

	@Override
	public void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer, float ctcssTone) {
		com.arrl.radiocraft.common.radio.TransmitterLog.record(this, pos, band, frequencyHertz); // On the air: direction finders can hear it.
		for(IAntenna other : others()) {
			double strength = strengthAt(distanceTo(other));
			if(strength <= 0.0D)
				continue;
			AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), band, frequencyHertz, strength, this, sourcePlayer, ctcssTone);
			other.receiveAudioPacket(packet);
		}
	}

	@Override
	public void receiveAudioPacket(AntennaVoicePacket packet) {
		packet.setStrength(packet.getStrength() * strengthAt(Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(pos.position()))));
		if(packet.getStrength() > 0.0D)
			radio.receiveVoice(packet);
	}

	@Override
	public void transmitCWPacket(ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {
		com.arrl.radiocraft.common.radio.TransmitterLog.record(this, pos, band, frequencyHertz); // On the air: direction finders can hear it.
		for(IAntenna other : others()) {
			double strength = strengthAt(distanceTo(other));
			if(strength > 0.0D)
				other.receiveCWPacket(new AntennaCWPacket(level, buffers, band, frequencyHertz, strength, this));
		}
	}

	@Override
	public void receiveCWPacket(AntennaCWPacket packet) {
		packet.setStrength(packet.getStrength() * strengthAt(Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(pos.position()))));
		if(packet.getStrength() > 0.0D && radio instanceof HFRadioBlockEntity hf
				&& BandUtils.areFrequenciesEqualWithTolerance(hf.getFrequency(), packet.getFrequency(), 1000.0F))
			hf.receiveCW(packet);
	}

	@Override
	public void transmitDataPacket(ServerLevel level, TncPacket payload, Band band, float frequencyHertz) {
		com.arrl.radiocraft.common.radio.TransmitterLog.record(this, pos, band, frequencyHertz); // On the air: direction finders can hear it.
		for(IAntenna other : others()) {
			double strength = strengthAt(distanceTo(other));
			if(strength > 0.0D)
				other.receiveDataPacket(new AntennaDataPacket(level, payload, band, frequencyHertz, strength, this));
		}
	}

	@Override
	public void receiveDataPacket(AntennaDataPacket packet) {
		packet.setStrength(packet.getStrength() * strengthAt(Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(pos.position()))));
		if(packet.getStrength() > 0.0D)
			radio.receiveData(packet);
	}

	@Override
	public AntennaPos getAntennaPos() {
		return pos;
	}

	public BlockPos getPos() {
		return pos.position();
	}

	@Override
	public IAntennaType<? extends AntennaData> getType() {
		return new RubberDuckyAntennaType(); // Something harmless for code that asks; nothing about it is used.
	}

	@Override
	public AntennaData getData() {
		return new RubberDuckyAntennaData(0.0D);
	}

	@Override
	public Player getPlayer() {
		return null;
	}
}

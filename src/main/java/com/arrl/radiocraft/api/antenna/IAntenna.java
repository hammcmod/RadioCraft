package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.UUID;

/**
 * {@link IAntenna} is an intermediary object acting as a link between the packets being sent around an
 * {@link IAntennaNetwork} and the {@link Level} the antennas are in.
 *
 * <p>See {@link StaticAntenna} for an example implementation of this, where {@link IAntenna}s interact with
 * {@link BlockEntity}s. Caching the values from transmit and receive strengths is highly recommended.</p>
 */
public interface IAntenna {

	/**
	 * Transmit an audio packet to other {@link IAntenna}s on the network.
	 * @param level The {@link ServerLevel} object (SVC API) for this {@link IAntenna}'s {@link Level}.
	 * @param rawAudio The raw PCM audio being sent in the packet.
	 * @param wavelength The wavelength of the transmission.
	 * @param frequencyKiloHertz The frequencyKiloHertz of the transmission.
	 * @param sourcePlayer The {@link UUID} of the player who sent the audio. Used for opus encoding/decoding.
	 */
	void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, int wavelength, int frequencyKiloHertz, UUID sourcePlayer);

	/**
	 * Handle receiving an audio packet from another {@link IAntenna} on the network.
	 * @param packet The {@link AntennaVoicePacket} being received.
	 */
	void receiveAudioPacket(AntennaVoicePacket packet);

	/**
	 * Transmit a CW/morse packet to other {@link IAntenna}s on the network.
	 * @param level The {@link ServerLevel} object for this {@link IAntenna}.
	 * @param buffers The {@link CWBuffer}s being sent, may not be in order.
	 * @param wavelength The wavelength of the transmission.
	 * @param frequencyKiloHertz The frequencyKiloHertz of the transmission.
	 */
	void transmitCWPacket(ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequencyKiloHertz);

	/**
	 * Handle receiving a CW/morse packet from another {@link IAntenna} on the network.
	 * @param packet The {@link AntennaCWPacket} being received.
	 */
	void receiveCWPacket(AntennaCWPacket packet);

	/**
	 * Thread safe, used for calculating signal strengths, must be backed by {@link java.util.concurrent.atomic.AtomicReference}
	 * @return Position of this antenna
	 */
	AntennaPos getAntennaPos();

    /**
     * Get the type of this antenna.
     * @return The type of this antenna.
     */
    IAntennaType<? extends AntennaData> getType();

    /**
     * Get the data for this antenna.
     * @return The data for this antenna.
     */
    AntennaData getData();

	/**
	 * Immutable single record for the position of an antenna. Used for calculating signal strengths.
	 * @param position current {@link BlockPos} of the antenna
	 * @param level current {@link Level} of the antenna
	 */
	record AntennaPos(BlockPos position, Level level) {}

}

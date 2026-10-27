package com.arrl.radiocraft.api.antenna;

import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaDataPacket;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
	 * @param band The band of the transmission.
	 * @param frequencyHertz The frequency (Hertz) of the transmission.
	 * @param sourcePlayer The {@link UUID} of the player who sent the audio. Used for opus encoding/decoding.
	 */
	void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer);

	/**
	 * As {@link #transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel, short[], Band, float, UUID)}, with a CTCSS
	 * tone under the voice (0 for none). Antennas that can carry a tone override this; the default drops it.
	 */
	default void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer, float ctcssTone) {
		transmitAudioPacket(level, rawAudio, band, frequencyHertz, sourcePlayer);
	}

	/**
	 * As above, from a transmitter {@code power} times as strong as a standard one (1): what the far end hears is that
	 * much stronger, still only out to the band's range. Antennas that do not model it send at standard strength.
	 */
	default void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer, float ctcssTone, float power) {
		transmitAudioPacket(level, rawAudio, band, frequencyHertz, sourcePlayer, ctcssTone);
	}

	/**
	 * Handle receiving an audio packet from another {@link IAntenna} on the network.
	 * @param packet The {@link AntennaVoicePacket} being received.
	 */
	void receiveAudioPacket(AntennaVoicePacket packet);

	/**
	 * Transmit a CW/morse packet to other {@link IAntenna}s on the network.
	 * @param level The {@link ServerLevel} object for this {@link IAntenna}.
	 * @param buffers The {@link CWBuffer}s being sent, may not be in order.
	 * @param band The band of the transmission.
	 * @param frequencyHertz The frequency (Hertz) of the transmission.
	 */
	void transmitCWPacket(ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz);

	/**
	 * Handle receiving a CW/morse packet from another {@link IAntenna} on the network.
	 * @param packet The {@link AntennaCWPacket} being received.
	 */
	void receiveCWPacket(AntennaCWPacket packet);

	/**
	 * Transmit a data packet (from a digital interface) to other {@link IAntenna}s on the network. Antennas that do not
	 * carry data leave this as it is and send nothing.
	 * @param level The {@link ServerLevel} object for this {@link IAntenna}.
	 * @param payload The {@link TncPacket} being sent.
	 * @param band The band of the transmission.
	 * @param frequencyHertz The frequency (Hertz) of the transmission.
	 */
	default void transmitDataPacket(ServerLevel level, TncPacket payload, Band band, float frequencyHertz) {}

	/**
	 * Handle receiving a data packet from another {@link IAntenna} on the network. Ignored unless overridden.
	 * @param packet The {@link AntennaDataPacket} being received.
	 */
	default void receiveDataPacket(AntennaDataPacket packet) {}

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

	/**
	 * True while this antenna has no sky above it (a handheld indoors or underground), which costs it some signal when
	 * terrain is on. Thread safe: read on the voice thread. Fixed antennas are outside by nature: false.
	 */
	default boolean isIndoors() {
		return false;
	}

    /**
     * Get the player this antenna belongs to.
     * @return The player this antenna belongs to, or null if it doesn't belong to a player.
     */
    Player getPlayer();

}

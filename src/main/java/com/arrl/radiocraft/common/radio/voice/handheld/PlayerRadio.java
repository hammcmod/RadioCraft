package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.api.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.IVoiceReceiver;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * {@link PlayerRadio} acts as a container used for the transmission and receiving of signals for VHF handsets.
 */
public class PlayerRadio implements IVoiceTransmitter, IVoiceReceiver, IAntenna {

    private EntityAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private AntennaNetwork network = null;

    public PlayerRadio(Player player) {
        setPlayer(player);
    }

    public Player getPlayer() {
        return playerRef == null ? null : playerRef.get();
    }

    /**
     * Set the player reference for this object and update it's {@link AntennaNetwork}. If player is null, this radio
     * will be disconnected from all networks.
     * @param player The player to target.
     */
    public void setPlayer(Player player) {
        playerRef = new WeakReference<>(player);
        if(receiveChannel != null)
            receiveChannel.updateEntity(RadiocraftVoicePlugin.api.fromEntity(player));

        if(player != null)
            setNetwork(AntennaNetworkManager.getNetwork(player.getLevel(), AntennaNetworkManager.VHF_ID)); // Always swap to a new network in case it was a dimension change.
        else if(network != null) {
            network.removeAntenna(this);
            network = null;
        }
    }

    /**
     * Attempt to find a valid VHF handheld on the given player.
     * @param player The player to be checked.
     * @return A {@link LazyOptional} containing the {@link IVHFHandheldCapability} of the found radio, or empty if none
     * were found.
     */
    public static LazyOptional<IVHFHandheldCapability> getHandheldCap(Player player) {
        if(player.getMainHandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get())
            return player.getMainHandItem().getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
        else if(player.getOffhandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get())
            return player.getOffhandItem().getCapability(RadiocraftCapabilities.VHF_HANDHELDS);

        return LazyOptional.empty();
    }

    public static IVHFHandheldCapability getHandheldCapOrNull(Player player) {
        LazyOptional<IVHFHandheldCapability> optional = getHandheldCap(player);
        return optional.orElse(null);
    }

    @Override
    public Vec3 getPos() {
        Player player = getPlayer();
        return player == null ? null : player.position();
    }

    @Override
    public BlockPos getBlockPos() {
        Player player = getPlayer();
        return player == null ? null : player.blockPosition();
    }

    @Override
    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequency) {
        // Handheld doesn't have CW capability.
    }

    @Override
    public void receiveCWPacket(AntennaCWPacket packet) {
        // Handheld doesn't have CW capability.
    }

    @Override
    public boolean isReceiving() {
        return true;
    }

    @Override
    public void setReceiving(boolean value) {

    }

    @Override
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequency, UUID sourcePlayer) {
        if(network != null) {
            Set<IAntenna> antennas = network.allAntennas();

            for(IAntenna antenna : antennas) {
                if(antenna != this) {
                    AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequency, 1.0F, this, sourcePlayer);

                    double distance = Math.sqrt(packet.getSource().getBlockPos().distSqr( antenna.getBlockPos()));
                    packet.setStrength(BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

                    antenna.receiveAudioPacket(packet);
                }
            }
        }
    }

    @Override
    public boolean canTransmitVoice() {
        IVHFHandheldCapability cap = getHandheldCapOrNull(getPlayer());
        return cap != null && cap.isPowered() && cap.isPTTDown();
    }

    public void openChannel() {
        if(RadiocraftVoicePlugin.api == null)
            Radiocraft.LOGGER.error("Radiocraft VoiceChatServerApi is null.");
        receiveChannel = RadiocraftVoicePlugin.api.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.api.fromEntity(getPlayer()));
    }

    @Override
    public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        IVHFHandheldCapability cap = getHandheldCapOrNull(getPlayer());
        if(cap != null)
            transmitAudioPacket(level, rawAudio, 2, cap.getFrequency(), sourcePlayer);
    }

    @Override
    public void receiveAudioPacket(AntennaVoicePacket packet) {
        receive(packet);
    }

    @Override
    public void receive(AntennaVoicePacket antennaPacket) {
        if(isReceiving()) {
            if(receiveChannel == null)
                openChannel();

            short[] rawAudio = antennaPacket.getRawAudio();
            for(int i = 0; i < rawAudio.length; i++)
                rawAudio[i] = (short)Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength

            byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
            receiveChannel.send(opusAudio);
        }
    }

    public void setNetwork(AntennaNetwork network) {
        if(this.network != null)
            this.network.removeAntenna(this);
        network.addAntenna(this);
        this.network = network;
    }

}

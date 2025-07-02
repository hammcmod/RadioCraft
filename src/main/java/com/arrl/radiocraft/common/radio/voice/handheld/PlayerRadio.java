package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.IVoiceReceiver;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * {@link PlayerRadio} acts as a container used for the transmission and receiving of signals for VHF handsets.
 */
public class PlayerRadio implements IVoiceTransmitter, IVoiceReceiver, IAntenna {

    private LocationalAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private AntennaNetwork network = null;
    private Level currentlevel; //keeps track of the current level, used only to reset LocationalAudioChannel receiveChannel when the player changes dimension

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

        Player old = playerRef == null ? null : playerRef.get();
        Radiocraft.LOGGER.error("Setting new player {}, old player was {}", player == null ? null : player.getName(), old == null ? (playerRef != null ? "already cleared" : "never set?") : old.getName());

        playerRef = new WeakReference<>(player);
        if(receiveChannel != null) {
            if (player != null){
                //In the event that entityChannels can be persuaded to work again, this is where the entity needs to be updated
//                receiveChannel.updateEntity(RadiocraftVoicePlugin.API.fromEntity(player));
            }else {
                receiveChannel.flush();
            }
            receiveChannel = null; //I don't see a way to close the channel, yet there is an isClosed method?
        }

        if(player != null)
            setNetwork(AntennaNetworkManager.getNetwork(player.level(), AntennaNetworkManager.VHF_ID)); // Always swap to a new network in case it was a dimension change.
        else if(network != null) {
            network.removeAntenna(this);
            network = null;
        }
    }

    /**
     * Attempt to find a valid VHF handheld on the given player.
     * @param player The player to be checked.
     * @return A {@link IVHFHandheldCapability} of the found radio, or empty if none
     * were found.
     */
    public static IVHFHandheldCapability getHandheldCapOrNull(Player player) {
        if (player.getMainHandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            return RadiocraftCapabilities.VHF_HANDHELDS.getCapability(player.getMainHandItem(), null);
        } else if (player.getOffhandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            return RadiocraftCapabilities.VHF_HANDHELDS.getCapability(player.getOffhandItem(), null);
        }
        return null;
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
    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequencyKiloHertz) {
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
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequencyKiloHertz, UUID sourcePlayer) {
        if(network != null) {
            //TODO move transmission logic into the AntennaNetwork
            Set<IAntenna> antennas = network.allAntennas();

            for(IAntenna antenna : antennas) {
                if(antenna != this) {
                    AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequencyKiloHertz, 1.0F, this, sourcePlayer);

                    double distance = Math.sqrt(packet.getSource().getBlockPos().distSqr( antenna.getBlockPos()));
                    packet.setStrength(BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

                    antenna.receiveAudioPacket(packet);
                }
            }
        }
    }

    @Override
    public boolean canTransmitVoice() {
        Player player = getPlayer();
        if(player == null){
            return false;
        }
        IVHFHandheldCapability cap = getHandheldCapOrNull(player);
        return cap != null && cap.isPowered() && cap.isPTTDown();
    }

    public boolean openChannel() {
        if(RadiocraftVoicePlugin.API == null)
            Radiocraft.LOGGER.error("VoiceChatServerApi is null, cannot open channel.");
//        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        currentlevel = getPlayer().level();
        receiveChannel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromServerLevel(currentlevel), getPosInVoiceApiFormat());
        if(receiveChannel == null) return false;
        receiveChannel.setDistance(16f);
//        receiveChannel.setCategory(RadiocraftVoicePlugin.handheldRadiosVolumeCategory.getId()); //TODO not currently working
//        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        return true;
    }

    private Position getPosInVoiceApiFormat() {
        Vec3 p = getPlayer().getEyePosition();
        return RadiocraftVoicePlugin.API.createPosition(p.x, p.y, p.z);
    }

    @Override
    public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        IVHFHandheldCapability cap = getHandheldCapOrNull(getPlayer());
        if(cap != null)
            transmitAudioPacket(level, rawAudio, 2, cap.getFrequencyKiloHertz(), sourcePlayer);
    }

    @Override
    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if(this.isReceiving() && canReceiveVoice()) {
            receive(packet);
        }
    }

    //TODO reconsider where canTransmit and canRecieve should be called from
    private boolean canReceiveVoice() {
        Player player = getPlayer();
        if(player == null){
            return false;
        }
        IVHFHandheldCapability cap = getHandheldCapOrNull(player);
        return cap != null && cap.isPowered();
    }

    @Override
    public void receive(AntennaVoicePacket antennaPacket) {
        if(isReceiving()) {
            Player player = getPlayer();
            if(player == null){
                Radiocraft.LOGGER.error("receiving but player is null?");
                return;
            }
            //TODO remove
            System.err.println("Receiving audio length " + antennaPacket.getRawAudio().length + " strength " + antennaPacket.getStrength() + " player " + player.getName() + " " + player.getUUID() + " eye position of player" + player.getEyePosition() + " from " + antennaPacket.getSourcePlayer());
            if(receiveChannel == null)
                if(!openChannel()) return; //if the channel cannot be opened, return early

            //if entityChannel can be gotten working again, this is where you check to make sure it's still bound to the player
            //most likely redundant with the onPlayerCloned hook in PlayerRadioManager calling set player
//            if(receiveChannel.getEntity().getEntity() != player){
//                System.err.println("receivechannel entity didn't equal this player? entity: " + receiveChannel.getEntity().getEntity() + " player " + player);
//                receiveChannel.updateEntity(RadiocraftVoicePlugin.API.fromEntity(player));
//            }
            if(currentlevel != player.level()){
                receiveChannel.flush();
                receiveChannel = null;
                if(!openChannel()) return;
            }
            receiveChannel.updateLocation(getPosInVoiceApiFormat());

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

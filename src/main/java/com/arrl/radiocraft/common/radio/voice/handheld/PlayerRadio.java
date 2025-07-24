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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link PlayerRadio} acts as a container used for the transmission and receiving of signals for VHF handsets.
 */
public class PlayerRadio implements IVoiceTransmitter, IVoiceReceiver, IAntenna {

    private LocationalAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private volatile AntennaNetwork network = null;
    private Level voiceChannelCurrentLevel; //keeps track of the current level, used only to reset LocationalAudioChannel receiveChannel when the player changes dimension

    private volatile boolean canReceive;
    private volatile boolean canTransmit;
    //fun fact, all updates to volatile references are atomic inherently! AtomicReference is used here for readability
    //and to ensure programmers understand they don't need to synchronize on PlayerRadio to access this
    private final AtomicReference<IAntenna.AntennaPos> antennaPos = new AtomicReference<>();
    private volatile int frequency;
    //position and level for voiceChannel;
    private volatile Vec3 voicePosition;
    private volatile Level voiceLevel;

    //used for calculating average amplitude for power meter
    private volatile long runningSampleSum=0; //stores the sum of the square of every sample since last tick
    private volatile long runningSampleCount=0; //number of samples since last tick

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
        synchronized (this) {
            playerRef = new WeakReference<>(player);
            this.tick();
        }

        if(player != null)
            setNetwork(AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID)); // Always swap to a new network in case it was a dimension change.
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
    protected static IVHFHandheldCapability getHandheldCapOrNull(Player player) {
        if (player.getMainHandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            return RadiocraftCapabilities.VHF_HANDHELDS.getCapability(player.getMainHandItem(), null);
        } else if (player.getOffhandItem().getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            return RadiocraftCapabilities.VHF_HANDHELDS.getCapability(player.getOffhandItem(), null);
        }
        return null;
    }

    @Override
    public Vec3 getPos() {
        return this.voicePosition;
    }

    @Override
    public AntennaPos getAntennaPos() {
        return this.antennaPos.get();
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
        return this.canReceive;
    }

    @Override
    public void setReceiving(boolean value) {

    }

    @Override
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, int wavelength, int frequencyKiloHertz, UUID sourcePlayer) {

        AntennaNetwork network;
        synchronized (this) {
            network = this.network;
        }

        if(network != null) {
            //TODO move transmission logic into the AntennaNetwork

            AntennaPos thisPos;
            thisPos = getAntennaPos();
            if(thisPos == null || thisPos.level() == null) return;
            if(!thisPos.level().equals(level.getServerLevel())) return;

            Set<IAntenna> antennas;
            Set<IAntenna> masterSet = network.allAntennas();

            synchronized (masterSet) { //you must synchronize on a SynchronizedSet when iterating, making a local copy to avoid locking the list so things like repeaters don't deadlock
                antennas = new HashSet<>(masterSet);
            }

            for(IAntenna antenna : antennas) {
                if(antenna != this) {
                    AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), wavelength, frequencyKiloHertz, 1.0F, this, sourcePlayer);

                    AntennaPos pos = antenna.getAntennaPos();

                    if(pos == null) continue;

                    double distance = Math.sqrt(thisPos.position().distSqr(pos.position()));
                    packet.setStrength(BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

                    antenna.receiveAudioPacket(packet);
                }
            }
        }
    }

    @Override
    public boolean canTransmitVoice() {
        return this.canTransmit;
    }

    public boolean openChannel() {
        if(RadiocraftVoicePlugin.API == null)
            Radiocraft.LOGGER.error("VoiceChatServerApi is null, cannot open channel.");
//        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        voiceChannelCurrentLevel = this.voiceLevel;
        receiveChannel = RadiocraftVoicePlugin.API.createLocationalAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromServerLevel(voiceChannelCurrentLevel), getPosInVoiceApiFormat());
        if(receiveChannel == null) return false;
        receiveChannel.setDistance(16f);
//        receiveChannel.setCategory(RadiocraftVoicePlugin.handheldRadiosVolumeCategory.getId()); //TODO not currently working
//        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        return true;
    }

    private Position getPosInVoiceApiFormat() {
        Vec3 p = getPos();
        return RadiocraftVoicePlugin.API.createPosition(p.x, p.y, p.z);
    }

    @Override
    public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
//        IVHFHandheldCapability cap = getHandheldCapOrNull(getPlayer());
//        if(cap != null)

        int freq;
        synchronized (this){
            if(!this.canTransmitVoice()) return;
            freq = this.frequency;
        }
        transmitAudioPacket(level, rawAudio, 2, freq, sourcePlayer);
    }

    @Override
    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if (this.isReceiving() && canReceiveVoice()) {
            receive(packet);
        }
    }

    //TODO reconsider where canTransmit and canRecieve should be called from
    private boolean canReceiveVoice() {
        return this.canReceive;
    }

    @Override
    public synchronized void receive(AntennaVoicePacket antennaPacket) {
        if(this.isReceiving()) {
//            System.err.println("Receiving audio length " + antennaPacket.getRawAudio().length + " strength " + antennaPacket.getStrength() + " player " + player.getName() + " " + player.getUUID() + " eye position of player" + player.getEyePosition() + " from " + antennaPacket.getSourcePlayer());

            //if entityChannel can be gotten working again, this is where you check to make sure it's still bound to the player
//            if(receiveChannel.getEntity().getEntity() != player){
//                System.err.println("receivechannel entity didn't equal this player? entity: " + receiveChannel.getEntity().getEntity() + " player " + player);
//                receiveChannel.updateEntity(RadiocraftVoicePlugin.API.fromEntity(player));
//            }


            if (voiceChannelCurrentLevel != this.voiceLevel) {
                if (receiveChannel != null) receiveChannel.flush();
                receiveChannel = null;
                if (!openChannel()) return;
            }else if(receiveChannel == null) {
                if (!openChannel()) return;
            } else {
                receiveChannel.updateLocation(getPosInVoiceApiFormat());
            }

            long runningTotal = 0;
            short[] rawAudio = antennaPacket.getRawAudio();
            for(int i = 0; i < rawAudio.length; i++) {
                short sample = (short) Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength
                runningTotal = sample * sample;
                rawAudio[i] = sample;
            }

            runningSampleCount += rawAudio.length;
            runningSampleSum+=runningTotal;

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

    public synchronized void tick() {
        Player player = getPlayer();
        IVHFHandheldCapability cap = player != null ? getHandheldCapOrNull(player) : null;

        if(cap == null) {
            this.canReceive = false;
            this.canTransmit = false;
            this.voicePosition = null;
            this.voiceLevel = null;
            this.antennaPos.set(null);
        } else {
            this.canTransmit = cap.isPowered() && cap.isPTTDown();
            this.canReceive = cap.isPowered();
            this.frequency = cap.getFrequencyKiloHertz();
            this.voicePosition = player.getEyePosition();
            this.voiceLevel = player.level();
            this.antennaPos.set(new AntennaPos(player.blockPosition(), player.level()));

            cap.setReceiveStrength(runningSampleCount == 0 ? 0f : (float)Math.sqrt((double) runningSampleSum / runningSampleCount));
        }

        runningSampleSum = 0;
        runningSampleCount = 0;
    }
}

package com.arrl.radiocraft.common.radio.voice.handheld;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IVHFHandheldCapability;
import com.arrl.radiocraft.common.capabilities.RadiocraftCapabilities;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.IVoiceReceiver;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.antenna.types.QuarterWaveVerticalAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.RubberDuckyAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.data.RubberDuckyAntennaData;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import com.arrl.radiocraft.common.radio.voice.RadiocraftVoicePlugin;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link PlayerRadio} acts as a container used for the transmission and receiving of signals for VHF handsets.
 */
public class PlayerRadio implements IVoiceTransmitter, IVoiceReceiver, IAntenna {

    private EntityAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private volatile AntennaNetwork network = null;

    private volatile List<SynchronousRadioState> radios = Collections.emptyList();
    //fun fact, all updates to volatile references are atomic inherently! AtomicReference is used here for readability
    //and to ensure programmers understand they don't need to synchronize on PlayerRadio to access this
    private final AtomicReference<IAntenna.AntennaPos> antennaPos = new AtomicReference<>();
    //position and level for voiceChannel;
    private volatile Vec3 voicePosition;
    private volatile Level voiceLevel;

    private boolean isUseHeld;

    public void setUseHeld(boolean useHeld) {
        this.isUseHeld = useHeld;
    }

    @SuppressWarnings("this-escape")
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

    protected enum HandheldLocation{
        HELD,
        HOT_BAR,
        BACKPACK
    }

    /**
     * Ephemeral state for each handheld for each tick.
     * Each tick each radio gets a new SynchronousRadioState instance
     * that only lives until the next tick
     */
    protected static class SynchronousRadioState{

        final ItemStack item;

        final HandheldLocation itemLocation;

        final boolean canReceive;
        final boolean canTransmit;

        final float frequency;

        final float gain;
        final float micGain;

        //used for calculating average amplitude for power meter
        volatile long runningSampleSum=0; //stores the sum of the square of every sample since last tick
        volatile long runningSampleCount=0; //number of samples since last tick

        public SynchronousRadioState(ItemStack item, boolean canReceive, boolean canTransmit, float frequency, HandheldLocation itemLocation, float gain, float micGain){
            this.item = item;
            this.canReceive = canReceive;
            this.canTransmit = canTransmit;
            this.frequency = frequency;
            this.itemLocation = itemLocation;
            this.gain = gain;
            this.micGain = micGain;
        }

        public SynchronousRadioState(ItemStack item, IVHFHandheldCapability cap, HandheldLocation itemLocation) {
            this(item, cap.isPowered(), cap.isPowered() && cap.isPTTDown(), cap.getFrequencyHertz(), itemLocation, cap.getGain(), cap.getMicGain());
        }
    }

    protected List<SynchronousRadioState> genHandheldStates(Player player) {

        Inventory playerInventory = player.getInventory();
        LinkedList<SynchronousRadioState> out = new LinkedList<>();

        //Add offhand first, so it goes before other radios on the hot bar, but after the held item
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
            IVHFHandheldCapability cap = offhand.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
            if(cap != null) out.add(new SynchronousRadioState(offhand, cap, HandheldLocation.HELD));
        }

        for(int i=0; i<playerInventory.getContainerSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if(itemStack.getItem() == RadiocraftItems.VHF_HANDHELD.get()) {
                IVHFHandheldCapability cap = itemStack.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
                if(cap != null) {
                    //if the current radio is the held item, put it before the offhand and other radios
                    //otherwise add the radio to the end of the list, so the order is held, then offhand, then all others
                    if (i == playerInventory.selected) {
                        out.addFirst(new SynchronousRadioState(itemStack, cap.isPowered(), cap.isPowered() && (cap.isPTTDown() || this.isUseHeld), cap.getFrequencyHertz(), HandheldLocation.HELD, cap.getGain(), cap.getMicGain()));
                    } else {
                        out.addLast(new SynchronousRadioState(itemStack, cap, Inventory.isHotbarSlot(i) ? HandheldLocation.HOT_BAR : HandheldLocation.BACKPACK));
                    }
                }
            }
        }
        return out;
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
    public IAntennaType<? extends AntennaData> getType() {
        return new RubberDuckyAntennaType();
    }

    @Override
    public AntennaData getData() {
        return new RubberDuckyAntennaData(0.1);
    }

    @Override
    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {
        // Handheld doesn't have CW capability.
    }

    @Override
    public void receiveCWPacket(AntennaCWPacket packet) {
        // Handheld doesn't have CW capability.
    }

    @Override //TODO get rid of this
    public synchronized boolean isReceiving() {
        for(SynchronousRadioState state : radios) if(state.canReceive) return true;
        return false;
    }

    @Override
    public void setReceiving(boolean value) {

    }

    @Override
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer) {

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
                    AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), band, frequencyHertz, 1.0F, this, sourcePlayer);

                    AntennaPos pos = antenna.getAntennaPos();

                    if(pos == null) continue;

                    double distance = Math.sqrt(thisPos.position().distSqr(pos.position()));
                    packet.setStrength(BandUtils.getBaseStrength(packet.getBand(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

                    antenna.receiveAudioPacket(packet);
                }
            }
        }
    }

    @Override //TODO get rid of this
    public synchronized boolean canTransmitVoice() {
        for(SynchronousRadioState state : radios) if(state.canTransmit) return true;
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean openChannel() {
        if(RadiocraftVoicePlugin.API == null)
            Radiocraft.LOGGER.error("VoiceChatServerApi is null, cannot open channel.");

        // You are supposed to use a _random_ uuid when making any audio channel, but there's a bug
        // where entity audio channels need to use the UUID of the entity they are attached to instead
        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(getPlayer().getUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        if(receiveChannel == null) return false;
        receiveChannel.setDistance(16f);
//        receiveChannel.setCategory(RadiocraftVoicePlugin.handheldRadiosVolumeCategory.getId()); //TODO not currently working
//        receiveChannel = RadiocraftVoicePlugin.API.createEntityAudioChannel(UUID.randomUUID(), RadiocraftVoicePlugin.API.fromEntity(getPlayer()));
        return true;
    }

    @Override
    public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        LinkedList<SynchronousRadioState> currentlyTransmitting = new LinkedList<>();
        synchronized (this){
            for(SynchronousRadioState radio : radios) if(radio.canTransmit) currentlyTransmitting.add(radio);
        }
        for(SynchronousRadioState radio : currentlyTransmitting) {    
            short[] audio = rawAudio.clone();
            for (int i = 0; i < audio.length; i++) {
                audio[i] = (short)Math.round(audio[i] * radio.micGain);
            }
            transmitAudioPacket(level, audio, Band.getBand("2m"), radio.frequency, sourcePlayer);
        }
    }

    @Override
    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if (this.isReceiving()) {
            receive(packet);
        }
    }

    @Override
    public synchronized void receive(AntennaVoicePacket antennaPacket) {
        if(this.isReceiving()) {

            Player player = getPlayer();

            if(receiveChannel == null)
                if (!openChannel()) return;

            if(receiveChannel.getEntity().getEntity() != player){
                receiveChannel.flush();
                receiveChannel.updateEntity(RadiocraftVoicePlugin.API.fromEntity(player));
            }

            float packetFrequency = antennaPacket.getFrequency();
            double packetStrength = antennaPacket.getStrength();

            //TODO make muffled sounding when not on hotbar (via low pass or the like, not just volume reduction)
            boolean isHeld = false;
            boolean shouldRecieve = false;
            float gain = 1.0f;
            for(SynchronousRadioState state : this.radios) {
                boolean inReasonableRange = BandUtils.areFrequenciesEqualWithTolerance(state.frequency, packetFrequency, 1000);
                if (state.canReceive && inReasonableRange) {
                    shouldRecieve = true;
                    if(state.itemLocation == HandheldLocation.HELD) {
                        isHeld = true;
                    }
                }
                gain = state.gain;
            }

            if(!shouldRecieve) return;

            long runningTotal = 0;
            short[] rawAudio = antennaPacket.getRawAudio();
            for(int i = 0; i < rawAudio.length; i++) {
                short sample = (short) Math.round(rawAudio[i] * packetStrength * (isHeld ? 1f : 0.5f) * gain); // Apply appropriate gain for signal strength
                runningTotal = sample * sample;
                rawAudio[i] = sample;
            }

            //TODO rework receiving to be per handheld, when frequency support is added
            for(SynchronousRadioState state : this.radios) {
                boolean inReasonableRange = BandUtils.areFrequenciesEqualWithTolerance(state.frequency, packetFrequency, 1000);
                if(state.canReceive && inReasonableRange) {
                    state.runningSampleCount += rawAudio.length;
                    state.runningSampleSum += runningTotal;
                }
            }

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

    public void tick() {
        Player player = getPlayer();
        List<SynchronousRadioState> radios = player == null ? Collections.emptyList() : genHandheldStates(player);
        List<SynchronousRadioState> lastTickRadios;

        synchronized (this) {
            lastTickRadios = this.radios;
            this.radios = radios;

            if (radios.isEmpty()) {
                this.voicePosition = null;
                this.voiceLevel = null;
                this.antennaPos.set(null);
            } else {
                this.voicePosition = player.getEyePosition();
                this.voiceLevel = player.level();
                this.antennaPos.set(new AntennaPos(player.blockPosition(), player.level()));
            }
        }

        for(SynchronousRadioState state : lastTickRadios) if(state.item != null){
            IVHFHandheldCapability cap = state.item.getCapability(RadiocraftCapabilities.VHF_HANDHELDS);
            if(cap != null) {
                cap.setReceiveStrength(state.runningSampleCount == 0 ? 0f : (float)Math.sqrt((double) state.runningSampleSum / state.runningSampleCount));
            }
        }
    }
}

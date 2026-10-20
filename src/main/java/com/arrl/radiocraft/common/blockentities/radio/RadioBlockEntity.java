package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.blockentities.radio.IBEVoiceReceiver;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.ITogglableBE;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.network.clientbound.CInterferencePacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.rfi.InterferenceLevel;
import com.arrl.radiocraft.common.radio.rfi.PickupZone;
import com.arrl.radiocraft.common.radio.rfi.RedstoneInterference;
import com.arrl.radiocraft.common.radio.rfi.TransmitInterference;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class RadioBlockEntity extends BlockEntity implements ITogglableBE, IVoiceTransmitter, IBEVoiceReceiver, INetworkObjectProvider, MenuProvider {

    protected boolean ssbEnabled = false;
    protected boolean isPTTDown = false; // Used by PTT button packets

    protected Band band; // Band the radio is configured for, usually not changed.
    protected float frequency; // Frequency the radio is currently using (in Hz)
    protected float speakerGain = 1.0F;
    protected float micGain = 1.0F;

    protected final BEVoiceReceiver voiceReceiver; // Acts as a container for voip channel info
    protected double antennaSWR; // Used clientside to calculate volume of static, and serverside for overdraw.
    protected boolean wasPowered; // Used for rendering clientside. The NetworkObject is the one actually controlling this.

    // Last received signal, server side only. Written from the voice thread, read on the game thread.
    protected volatile double lastSignalStrength = 0.0D;
    protected volatile long lastSignalGameTime = -1L;

    // Parrot mode (see setParrotDelay). The queue is filled from the voice thread and drained on the game thread.
    protected volatile int parrotDelayTicks = 0;
    protected volatile long lastParrotEchoGameTime = -1L;
    protected final ConcurrentLinkedQueue<QueuedVoice> parrotVoiceQueue = new ConcurrentLinkedQueue<>();

    protected final AtomicReference<BlockPos> micPos = new AtomicReference<>(); //thread safe position reference, overkill but makes purpose clear

    // Redstone interference (see RedstoneInterference). The server gathers pops during a tick, scales them for the band
    // and config, and sends the result to clients, which keep their own decaying copy for the static volume.
    protected final InterferenceLevel interference = new InterferenceLevel();
    private double pendingInterference = 0.0D;
    private AntennaNetworkObject zoneAntenna = null;
    private int zoneRefreshTicks = 0;
    private static final int ZONE_REFRESH_TICKS = 100; // Antenna wires and coax can change without the antenna object changing.
    // Transmit side: what the current transmission is doing to nearby redstone, null while not transmitting.
    @Nullable
    private TransmitInterference.Targets transmitTargets = null;
    protected volatile long lastTransmitGameTime = -1L; // Last tick anything went out the antenna (voice frame or CW), from any thread.

    public RadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
        super(type, pos, state);
        this.micPos.set(pos);
        this.band = band;
        this.frequency = band == null ? 0 : band.minFrequency();
        this.voiceReceiver = new BEVoiceReceiver(pos.getX(), pos.getY(), pos.getZ());
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if(t instanceof RadioBlockEntity be) {
            RadioNetworkObject networkObject = (RadioNetworkObject)be.getNetworkObject(level, pos);
            boolean powered = networkObject != null && networkObject.isPowered;
            boolean shouldUpdate = false;

            if(be.wasPowered != powered) { // Tracked both ways, so the client also learns when the radio turns off.
                be.wasPowered = powered;
                be.updateIsReceiving(); // Power may have changed under us (network drained, reload), so re-arm or mute the receiver.
                shouldUpdate = true;
            }

            double newSWR = 0.0D; // No power, no SWR (and no static).
            AntennaNetworkObject antenna = null;
            if(powered) {
                List<AntennaNetworkObject> antennas = networkObject.getAntennas();
                if(antennas.size() == 1) {
                    antenna = antennas.get(0);
                    newSWR = antenna.getSWR(be.frequency);
                }
                else if(antennas.size() > 1)
                    newSWR = 10.0D;
            }

            if(newSWR != be.antennaSWR) {
                be.antennaSWR = newSWR;
                shouldUpdate = true; // If SWR has changed, notify the client about it.
            }

            if(shouldUpdate) {
                be.setChanged();
                be.updateBlock();
            }
            if(level instanceof ServerLevel serverLevel) {
                be.tickParrot(serverLevel);
                be.tickInterference(serverLevel, antenna);
            }
            be.additionalTick();
        }
    }

    // -------------------- REDSTONE INTERFERENCE --------------------

    /**
     * Keeps this radio's {@link PickupZone} registered while it is powered with one antenna, and sends the pops gathered
     * this tick to the clients. The zone is rebuilt when the antenna changes and every {@link #ZONE_REFRESH_TICKS}, since
     * wires and coax can change under the same antenna object.
     */
    private void tickInterference(ServerLevel serverLevel, @Nullable AntennaNetworkObject antenna) {
        if(antenna != zoneAntenna || (antenna != null && --zoneRefreshTicks <= 0)) {
            zoneAntenna = antenna;
            zoneRefreshTicks = ZONE_REFRESH_TICKS;
            if(antenna == null)
                RedstoneInterference.unregister(serverLevel, worldPosition);
            else
                RedstoneInterference.register(serverLevel, RedstoneInterference.buildZone(serverLevel, worldPosition, antenna));
        }

        if(pendingInterference > 0.0D) {
            double scaled = pendingInterference * RedstoneInterference.bandFactor(band) * RedstoneInterference.configFactor(antennaSWR);
            pendingInterference = 0.0D;
            if(scaled > 0.0D) {
                interference.add(serverLevel.getGameTime(), scaled);
                CInterferencePacket.sendToTracking(serverLevel, worldPosition, scaled);
            }
        }

        boolean transmitting = antenna != null && isTransmittingNow(serverLevel);
        if(transmitting && transmitTargets == null) {
            PickupZone zone = RedstoneInterference.getZone(serverLevel, worldPosition);
            boolean vertical = antenna.getAntenna() != null && TransmitInterference.VERTICAL_ANTENNAS.contains(antenna.getAntenna().type.getId());
            transmitTargets = zone == null ? TransmitInterference.Targets.EMPTY : TransmitInterference.pick(serverLevel, zone, isQrp(), vertical, antennaSWR);
            TransmitInterference.start(serverLevel, transmitTargets);
        }
        else if(!transmitting && transmitTargets != null)
            stopTransmitInterference(serverLevel);
    }

    private void stopTransmitInterference(ServerLevel serverLevel) {
        if(transmitTargets != null) {
            TransmitInterference.stop(serverLevel, transmitTargets);
            transmitTargets = null;
        }
    }

    /** True while RF is going out: PTT held in SSB, or morse keyed within the last couple of ticks. */
    protected boolean isTransmittingNow(ServerLevel serverLevel) {
        return canTransmitVoice() || (lastTransmitGameTime >= 0 && serverLevel.getGameTime() - lastTransmitGameTime <= 2);
    }

    /** Subclasses call this whenever they put something on the air that PTT does not already cover (CW keying). */
    protected void recordTransmit() {
        if(level != null)
            lastTransmitGameTime = level.getGameTime();
    }

    /** Low-power rigs reach less redstone when transmitting. */
    public boolean isQrp() {
        return false;
    }

    /** What the current transmission is doing to nearby redstone, null while not transmitting. */
    @Nullable
    public TransmitInterference.Targets getTransmitTargets() {
        return transmitTargets;
    }

    /** Called by {@link RedstoneInterference} on the server thread for each redstone switch coupled into this station. */
    public void addInterference(double amount) {
        pendingInterference += amount;
    }

    /** The decaying interference level, 0 to 1, on whichever side this block entity lives. */
    public double getInterference() {
        return level == null ? 0.0D : interference.get(level.getGameTime());
    }

    public InterferenceLevel getInterferenceLevel() {
        return interference;
    }

    // -------------------- PARROT --------------------

    /**
     * Turns the radio into a parrot (simplex repeater): whatever it receives is sent out again through its own antenna
     * {@code delayTicks} later, so one player can hear their own transmission come back. 0 switches it off. Not saved
     * with the world: a {@link com.arrl.radiocraft.common.blockentities.ParrotControllerBlockEntity} next to the radio
     * re-applies its delay every tick, and {@code /rcdebug parrot} sets it directly for testing.
     */
    public void setParrotDelay(int delayTicks) {
        parrotDelayTicks = Math.max(0, delayTicks);
        if(parrotDelayTicks == 0)
            parrotVoiceQueue.clear();
    }

    public int getParrotDelay() {
        return parrotDelayTicks;
    }

    public boolean isParrot() {
        return parrotDelayTicks > 0;
    }

    /** Sends out every queued transmission whose delay has run out. Server tick only. */
    protected void tickParrot(ServerLevel serverLevel) {
        if(parrotVoiceQueue.isEmpty())
            return;
        long now = serverLevel.getGameTime();
        for(QueuedVoice next = parrotVoiceQueue.peek(); next != null && next.dueTick() <= now; next = parrotVoiceQueue.peek()) {
            parrotVoiceQueue.poll();
            acceptVoicePacket(() -> serverLevel, next.audio(), next.sourcePlayer());
            recordParrotEcho(serverLevel);
        }
    }

    /** Remembers that an echo just went out, for the parrot controller's transmit light. */
    protected void recordParrotEcho(ServerLevel serverLevel) {
        lastParrotEchoGameTime = serverLevel.getGameTime();
    }

    /** Game time at which this radio last sent out a parrot echo, -1 if never. */
    public long getLastParrotEchoGameTime() {
        return lastParrotEchoGameTime;
    }

    /** One voice frame waiting to be echoed. */
    protected record QueuedVoice(short[] audio, UUID sourcePlayer, long dueTick) {}

    /**
     * Network objects only live in memory, so after a world reload the radio's object comes back switched off. Put it
     * back the way the block entity last saw it.
     */
    @Override
    public void onNetworkObjectCreated(Level level, BlockPos pos, BENetworkObject networkObject) {
        if(wasPowered && networkObject instanceof RadioNetworkObject radio && radio.canPowerOn())
            radio.isPowered = true;
    }

    // Override this for any additional ticks needed like CWSendBuffers
    protected void additionalTick() {}

    // -------------------- POWER IMPLEMENTATION --------------------

    @Override
    public void toggle() {
        if(!level.isClientSide()) {
            if(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) {
                if(networkObject.isPowered)
                    networkObject.isPowered = false;
                else if(networkObject.canPowerOn())
                    networkObject.isPowered = true;

                updateIsReceiving();
                updateBlock();
            }
        }
    }

    public void overdraw() {}

    // -------------------- VOICE/RADIO IMPLEMENTATION --------------------

    @Override
    public BEVoiceReceiver getVoiceReceiver() {
        return voiceReceiver;
    }

    public boolean getSSBEnabled() {
        return ssbEnabled;
    }

    public void setSSBEnabled(boolean value) {
        if(ssbEnabled != value) {
            ssbEnabled = value;
            updateIsReceiving();
            updateBlock();
            setChanged();
        }
    }

    public void updateIsReceiving() {
        if(!level.isClientSide() && getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) {
            if(canTransmitVoice()) {
                networkObject.setTransmitting(true);
                getVoiceReceiver().setReceiving(false);
            }
            else {
                networkObject.setTransmitting(false);
                getVoiceReceiver().setReceiving(ssbEnabled && networkObject.isPowered); // A radio that is off hears nothing.
            }
        }
    }

    /**
     * Server side entry point for a voice transmission that reached this radio's antenna on its frequency. Called from
     * the voice chat thread. Records the signal (so the server, debug tools and tests can see what arrived) and hands
     * the audio to the {@link BEVoiceReceiver} for playback.
     *
     * @return True if the radio was listening and took the packet.
     */
    public boolean receiveVoice(AntennaVoicePacket packet) {
        if(!getVoiceReceiver().isReceiving())
            return false;
        recordSignal(packet.getStrength());
        if(isParrot()) // Queue the audio as it arrived, before the receiver scales it, so the echo carries one path's loss, not two.
            parrotVoiceQueue.add(new QueuedVoice(packet.getRawAudio().clone(), packet.getSourcePlayer(), lastSignalGameTime + parrotDelayTicks));
        getVoiceReceiver().receive(packet);
        return true;
    }

    /** Remembers the strength and game time of the last signal that reached this radio. */
    protected void recordSignal(double strength) {
        lastSignalStrength = strength;
        lastSignalGameTime = level == null ? 0L : level.getGameTime();
    }

    /** Strength (0-1) of the last voice or CW signal this radio received, 0 if it never received one. */
    public double getLastSignalStrength() {
        return lastSignalStrength;
    }

    /** Game time at which the last signal was received, -1 if none. */
    public long getLastSignalGameTime() {
        return lastSignalGameTime;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public float getSpeakerGain() {
        return speakerGain;
    }

    public void setSpeakerGain(float speakerGain) {
        this.speakerGain = Mth.clamp(speakerGain, 0.0F, 2.0F);
        setChanged();
        updateBlock();
    }

    public float getMicGain() {
        return micGain;
    }

    public void setMicGain(float micGain) {
        this.micGain = Mth.clamp(micGain, 0.0F, 2.0F);
        setChanged();
        updateBlock();
    }

    public Band getBand() {
        return band;
    }

    public void setBand(Band band) {
        this.band = band;
        updateIsReceiving();
        updateBlock();
    }

    public boolean isPTTDown() {
        return isPTTDown;
    }

    public void setPTTDown(boolean value) {
        if(isPTTDown != value) {
            isPTTDown = value;
            updateIsReceiving();
            updateBlock();
        }
    }

    /**
     * Move the current frequency by the specified number of steps as defined in {@link RadiocraftServerConfig}.
     *
     * @param stepCount The number of steps to increment by.
     */
    public void updateFrequency(int stepCount) {
        int step = RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
        float min = band.minFrequency();
        float max = (band.maxFrequency() - band.minFrequency()) / step * step + min; // This calc looks weird, but it's integer division, throws away remainder to ensure the freq doesn't do a "half step" to max.

        frequency = Mth.clamp(frequency + step * stepCount, min, max);
        setChanged();
        updateBlock();
    }

    @Override
    public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
        if(antennas.size() == 1)
            antennas.get(0).transmitAudioPacket(level, rawAudio, band, frequency, sourcePlayer);
        else if(antennas.size() > 1)
            overdraw();
    }

    @Override
    public boolean canTransmitVoice() {
        if(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject)
            return ssbEnabled && networkObject.isPowered && isPTTDown;
        return false;
    }

    @Override
    public Vec3 getPos() {
        return this.micPos.get().getCenter();
    }

    /** SWR the server last measured on the connected antenna, 0 when off or without an antenna. */
    public double getAntennaSWR() {
        return antennaSWR;
    }

    public double getStaticVolume() {
        if(antennaSWR <= 0.01D)
            return 0.0D;
        else {
            return SWRHelper.getEfficiencyMultiplier(antennaSWR);
        }
    }

    public boolean shouldPlayStatic() {
        return getSSBEnabled();
    }

    // -------------------- BE & SYNC IMPLEMENTATION --------------------

    /**
     * Writes the radio's state. Used for both the world save ({@link #saveAdditional}) and the client sync tag
     * ({@link #getUpdateTag}). Subclasses add their own fields by overriding this and calling super.
     */
    protected void setupSaveTag(CompoundTag nbt) {
        nbt.putBoolean("ssbEnabled", ssbEnabled);
        if(band != null)
            nbt.putString("band", band.name());
        nbt.putFloat("frequency", frequency);
        nbt.putFloat("speakerGain", speakerGain);
        nbt.putFloat("micGain", micGain);
        nbt.putDouble("antennaSWR", antennaSWR);
        nbt.putBoolean("wasPowered", wasPowered);
    }

    /**
     * Reads the state written by {@link #setupSaveTag}. Used for both world load and client sync. Missing keys keep
     * their current values, so a tag from an older save or a partial update is safe.
     */
    protected void readSaveTag(CompoundTag nbt) {
        ssbEnabled = nbt.getBoolean("ssbEnabled");
        if(nbt.contains("band")) {
            Band saved = Band.getBand(nbt.getString("band"));
            if(saved != null)
                band = saved;
        }
        if(nbt.contains("frequency"))
            frequency = nbt.getFloat("frequency");
        speakerGain = nbt.contains("speakerGain") ? nbt.getFloat("speakerGain") : 1.0F;
        micGain = nbt.contains("micGain") ? nbt.getFloat("micGain") : 1.0F;
        antennaSWR = nbt.getDouble("antennaSWR");
        wasPowered = nbt.getBoolean("wasPowered");

        if(band != null) { // Reset the frequency if the saved one is outside the band or not on a step.
            int step = RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
            if(frequency > band.maxFrequency() || frequency < band.minFrequency() || (frequency - band.minFrequency()) % step != 0)
                frequency = band.minFrequency();
        }
    }

    /**
     * Override this method if you need other sound instances-- for example {@link RadioMorseSoundInstance}. Only called
     * clientside.
     */
    protected void setupSoundInstances() {}

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(nbt, registries);
        setupSaveTag(nbt);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(nbt, registries);
        readSaveTag(nbt);
    }

    /** Sent to clients on chunk load and whenever {@link #updateBlock} fires; read back through {@link #loadAdditional}. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level.isClientSide())
            setupSoundInstances();
        else {
            // If on server, register self to listen for voice packets & notify client of loaded data.
            VoiceTransmitters.addListener(level, this);
            getNetworkObject(level, worldPosition); // This forces the network object to get initialised.
            updateIsReceiving(); // Re-arm voice receive for a radio that was saved switched on in SSB.
            updateBlock();
        }
    }

    @Override
    public void setRemoved() {
        if(!level.isClientSide()) {
            VoiceTransmitters.removeListener(level, this); // Stop listening when removed.
            RedstoneInterference.unregister(level, worldPosition);
            zoneAntenna = null;
            if(level instanceof ServerLevel serverLevel)
                stopTransmitInterference(serverLevel);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if(!level.isClientSide()) {
            VoiceTransmitters.removeListener(level, this); // Stop listening when chunk unloads.
            RedstoneInterference.unregister(level, worldPosition);
            zoneAntenna = null;
            if(level instanceof ServerLevel serverLevel)
                stopTransmitInterference(serverLevel);
        }
        super.onChunkUnloaded();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ContainerData getDataSlots() {
        if(level.isClientSide)
            return new SimpleContainerData(1);

        RadioNetworkObject networkObject = (RadioNetworkObject)IBENetworks.getObject(level, worldPosition);
        return networkObject == null ? new SimpleContainerData(1) : new ContainerData() {
            @Override
            public int get(int index) {
                return networkObject.isPowered ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {} // This doesn't need a setter-- use packets instead. NetworkObject isn't on client.

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    public boolean wasPowered() {
        return wasPowered;
    }

    protected void updateBlock() {
        if(level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

}

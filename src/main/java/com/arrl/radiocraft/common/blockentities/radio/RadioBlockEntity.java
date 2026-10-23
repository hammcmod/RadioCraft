package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.blockentities.radio.IBEVoiceReceiver;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.client.blockentity.RadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
import com.arrl.radiocraft.common.blockentities.ITogglableBE;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import com.arrl.radiocraft.common.radio.crystal.CrystalQuality;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.network.clientbound.CInterferencePacket;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaDataPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.voice.Afsk;
import com.arrl.radiocraft.common.radio.voice.Rtty;
import com.arrl.radiocraft.common.radio.rfi.InterferenceLevel;
import com.arrl.radiocraft.common.radio.rfi.PickupZone;
import com.arrl.radiocraft.common.radio.rfi.RedstoneInterference;
import com.arrl.radiocraft.common.radio.rfi.TransmitInterference;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.SolarEvent.SolarEventInstance;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import com.arrl.radiocraft.common.radio.antenna.BareConnectorAntenna;
import com.arrl.radiocraft.common.radio.voice.Ctcss;
import com.arrl.radiocraft.common.radio.voice.Detuning;
import com.arrl.radiocraft.common.radio.voice.RepeaterAudio;
import com.arrl.radiocraft.common.radio.voice.ReceiverMix;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class RadioBlockEntity extends BlockEntity implements ITogglableBE, IVoiceTransmitter, IBEVoiceReceiver, INetworkObjectProvider, MenuProvider {

    protected boolean ssbEnabled = true; // A fresh radio comes up in SSB, so it plays static and hears voice as soon as it is powered.
    protected boolean isPTTDown = false; // Set by PTT button packets; synced to clients so the receiver sounds can go quiet while transmitting.

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
    /** How long the S-meter (comparator output) keeps showing a signal after its last frame, like a meter's needle falling back. */
    public static final int METER_HOLD_TICKS = 20;
    private int lastMeterReading = 0; // What the comparators next to us last saw, so they are only told about changes.

    // Parrot mode (see setParrotDelay). The queue is filled from the voice thread and drained on the game thread.
    protected volatile int parrotDelayTicks = 0;
    protected volatile long lastParrotEchoGameTime = -1L;
    protected final ConcurrentLinkedQueue<QueuedVoice> parrotVoiceQueue = new ConcurrentLinkedQueue<>();

    protected final AtomicReference<BlockPos> micPos = new AtomicReference<>(); //thread safe position reference, overkill but makes purpose clear

    // Redstone interference (see RedstoneInterference). The server gathers pops during a tick, scales them for the band
    // and config, and sends the result to clients, which keep their own decaying copy for the static volume.
    protected final InterferenceLevel interference = new InterferenceLevel();
    private double pendingInterference = 0.0D; // Redstone pops, still to be scaled for band and config.
    private double pendingDirectInterference = 0.0D; // Already scaled noise from other sources (lightning).
    private AntennaNetworkObject zoneAntenna = null;
    private int zoneRefreshTicks = 0;
    private static final int ZONE_REFRESH_TICKS = 100; // Antenna wires and coax can change without the antenna object changing.
    // Transmit side: what the current transmission is doing to nearby redstone, null while not transmitting.
    @Nullable
    private TransmitInterference.Targets transmitTargets = null;
    protected volatile long lastTransmitGameTime = -1L; // Last tick anything went out the antenna (voice frame or CW), from any thread.
    // The antenna socket while nothing is on the coax: a few blocks of range (server only, see BareConnectorAntenna).
    @Nullable
    protected volatile BareConnectorAntenna bareConnector = null;

    public RadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
        super(type, pos, state);
        this.micPos.set(pos);
        this.band = band;
        this.frequency = band == null ? 0 : band.minFrequency();
        this.voiceReceiver = new BEVoiceReceiver(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Once a second, starts any of the radio's looping sounds that the client's sound engine dropped. */
    public static <T extends BlockEntity> void clientTick(Level level, BlockPos pos, BlockState state, T t) {
        if(t instanceof RadioBlockEntity be && level.getGameTime() % 20 == 0)
            RadioBlockEntityClientHandler.keepSoundsPlaying(be);
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
            if(!level.isClientSide)
                be.tickBareConnector(powered && networkObject.getAntennas().isEmpty());
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
            if(be.tickSyncedSignal(level.getGameTime()))
                shouldUpdate = true; // The client ducks its static under the signal.

            if(shouldUpdate) {
                be.setChanged();
                be.updateBlock();
            }
            if(level instanceof ServerLevel serverLevel) {
                be.tickParrot(serverLevel);
                be.tickInterference(serverLevel, antenna);
                be.tickMeter(serverLevel);
                be.tickData(serverLevel, powered);
            }
            be.additionalTick();
        }
    }

    // -------------------- BARE CONNECTOR (NO ANTENNA) --------------------

    /** Keeps the bare connector on the antenna network of this radio's band while it is wanted, and off it otherwise. */
    private void tickBareConnector(boolean wanted) {
        if(wanted) {
            if(bareConnector == null)
                bareConnector = new BareConnectorAntenna(this);
            bareConnector.register(band);
        }
        else
            dropBareConnector();
    }

    private void dropBareConnector() {
        BareConnectorAntenna bare = bareConnector;
        if(bare != null) {
            bare.unregister();
            bareConnector = null;
        }
    }

    /** The bare connector, while the radio is on with nothing on its coax; null otherwise. */
    @Nullable
    public BareConnectorAntenna getBareConnector() {
        return bareConnector;
    }

    // -------------------- DATA (DIGITAL INTERFACE) --------------------

    private final ArrayDeque<TncPacket> dataQueue = new ArrayDeque<>(); // Server thread only.
    private TncPacket sendingData = null;
    private List<short[]> dataFrames = List.of();
    private int dataFrame = 0;
    private UUID dataVoiceId = null;
    private final double[] rttyPhase = new double[1]; // Keeps RTTY tones continuous from one packet to the next.
    private long lastRttyGameTime = -100L;

    /**
     * Queues a packet from a digital interface: the radio sends its burst of tones (heard by any radio on the
     * frequency, like voice) and then the packet itself. Nothing is queued while the radio is off.
     *
     * @return True if the packet will go out.
     */
    public boolean sendData(TncPacket packet) {
        if(level == null || level.isClientSide || !(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) || !networkObject.isPowered)
            return false;
        dataQueue.add(packet);
        return true;
    }

    /** True if the radio is switched on and has the power it needs (server side). */
    public boolean isPowered() {
        return level != null && getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject && networkObject.isPowered;
    }

    /** True while a packet burst is going out or waiting to. */
    public boolean isSendingData() {
        return sendingData != null || !dataQueue.isEmpty();
    }

    /** Plays the packet in hand out frame by frame (2.5 frames of 20 ms per tick) and sends it once the tones are done. */
    private void tickData(ServerLevel serverLevel, boolean powered) {
        if(!powered) {
            dataQueue.clear();
            sendingData = null;
            return;
        }
        if(sendingData == null) {
            sendingData = dataQueue.poll();
            if(sendingData == null)
                return;
            if(sendingData.kind() == TncPacket.Kind.RTTY) // Straight on from the last characters, or a lead-in after a pause.
                dataFrames = Rtty.frames(sendingData.text(), serverLevel.getGameTime() - lastRttyGameTime > 2, rttyPhase);
            else if(sendingData.kind() == TncPacket.Kind.FILE) {
                transmitData(serverLevel, sendingData.header()); // Receivers learn a file is coming and how long it takes.
                dataFrames = FileTransfer.audio(sendingData.item(), serverLevel, sendingData.id());
            }
            else
                dataFrames = Afsk.burst(sendingData.airBytes(), sendingData.id());
            dataFrame = 0;
        }
        if(dataVoiceId == null)
            dataVoiceId = UUID.nameUUIDFromBytes(("radiocraft-tnc-" + worldPosition.asLong()).getBytes(StandardCharsets.UTF_8));
        int frames = serverLevel.getGameTime() % 2 == 0 ? 3 : 2;
        for(int i = 0; i < frames && dataFrame < dataFrames.size(); i++) {
            transmitVoice(() -> serverLevel, dataFrames.get(dataFrame++), getTunedFrequency(), dataVoiceId, 0.0F);
            recordTransmit();
        }
        if(dataFrame >= dataFrames.size()) {
            if(sendingData.kind() == TncPacket.Kind.RTTY)
                lastRttyGameTime = serverLevel.getGameTime();
            transmitData(serverLevel, sendingData);
            sendingData = null;
        }
    }

    /** Puts a packet on the air on the radio's own frequency (packet is simplex), the way voice goes out. */
    protected void transmitData(ServerLevel serverLevel, TncPacket packet) {
        if(!(IBENetworks.getObject(serverLevel, worldPosition) instanceof RadioNetworkObject networkObject))
            return;
        List<AntennaNetworkObject> antennas = networkObject.getAntennas();
        if(antennas.size() == 1)
            antennas.get(0).transmitDataPacket(serverLevel, packet, band, getTunedFrequency());
        else if(antennas.size() > 1)
            overdraw();
        else {
            BareConnectorAntenna bare = bareConnector;
            if(bare != null)
                bare.transmitDataPacket(serverLevel, packet, band, getTunedFrequency());
        }
    }

    /**
     * A data packet reached this radio's antenna. It decodes if the radio is on, not transmitting, tuned to it (a
     * modem cannot copy tones shifted by a detuned SSB receiver) and it is far enough above the noise
     * ({@link RadiocraftServerConfig#TNC_MIN_SNR}); then every digital interface next to the radio gets it. RTTY
     * still prints a little under that, with more and more characters garbled, down to half of it.
     *
     * @return True if the packet decoded.
     */
    public boolean receiveData(AntennaDataPacket packet) {
        if(!(level instanceof ServerLevel serverLevel) || !(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) || !networkObject.isPowered)
            return false;
        if(isTransmittingNow(serverLevel) || sendingData != null)
            return false;
        Detuning.Effect effect = getDetuning(packet.getFrequency());
        if(!effect.audible() || effect.pitchShiftHz() != 0.0D || effect.splatter() > 0.0D)
            return false;
        double strength = packet.getStrength() * effect.strength();
        if(isSkipPath(packet))
            strength *= ReceiverMix.fading(level.getGameTime() / 20.0D, pathSeed(packet), RadiocraftServerConfig.SKIP_FADING_DEPTH.get());
        double quality = strength / copyThreshold(getNoiseFloor());
        TncPacket payload = packet.getPayload();
        if(payload.kind() == TncPacket.Kind.RTTY && quality >= RTTY_GARBLE_FLOOR && quality < 1.0D)
            payload = payload.withText(Rtty.garble(payload.text(), (1.0D - quality) / (1.0D - RTTY_GARBLE_FLOOR), new java.util.Random(level.getRandom().nextLong())));
        else if(!decodes(strength, getNoiseFloor()))
            return false;
        recordSignal(strength);
        for(RadioInterfaceBlockEntity radioInterface : getRadioInterfaces())
            radioInterface.receive(this, payload);
        return true;
    }

    /** RTTY below this share of the copy threshold is lost; between it and the threshold it prints garbled. */
    public static final double RTTY_GARBLE_FLOOR = 0.5D;

    /** Weakest signal that copies cleanly over {@code noise}. */
    public static double copyThreshold(double noise) {
        return Math.max(RadiocraftServerConfig.TNC_MIN_SIGNAL.get(), noise * RadiocraftServerConfig.TNC_MIN_SNR.get());
    }

    /** True if a packet at {@code strength} copies over {@code noise}: no weaker than the noise times the minimum SNR. */
    public static boolean decodes(double strength, double noise) {
        return strength > 0.0D && strength >= copyThreshold(noise);
    }

    /** Radio interfaces on any side of this radio. */
    public List<RadioInterfaceBlockEntity> getRadioInterfaces() {
        if(level == null)
            return List.of();
        List<RadioInterfaceBlockEntity> found = new java.util.ArrayList<>();
        for(Direction direction : Direction.values()) {
            BlockPos at = worldPosition.relative(direction);
            if(level.isLoaded(at) && level.getBlockEntity(at) instanceof RadioInterfaceBlockEntity radioInterface)
                found.add(radioInterface);
        }
        return found;
    }

    // -------------------- S-METER (COMPARATOR OUTPUT) --------------------

    /**
     * The strength of the signal being received right now, 0 to 15, the way a comparator reads it: a signal meter on
     * the front panel. It follows the last voice or CW frame for {@link #METER_HOLD_TICKS} and then falls back to 0.
     */
    public int getMeterReading() {
        if(level == null || lastSignalGameTime < 0 || level.getGameTime() - lastSignalGameTime > METER_HOLD_TICKS)
            return 0;
        return Mth.clamp(Mth.ceil(lastSignalStrength * 15.0D), 1, 15);
    }

    /** Comparators only re-read a block when it tells them to, so do that whenever the meter moves. */
    private void tickMeter(ServerLevel serverLevel) {
        int reading = getMeterReading();
        if(reading != lastMeterReading) {
            lastMeterReading = reading;
            serverLevel.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
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

        if(pendingInterference > 0.0D || pendingDirectInterference > 0.0D) {
            double scaled = pendingInterference * RedstoneInterference.bandFactor(band) * RedstoneInterference.configFactor(antennaSWR) + pendingDirectInterference;
            pendingInterference = 0.0D;
            pendingDirectInterference = 0.0D;
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

    /**
     * Adds noise that has already been scaled by its source (a lightning crash, see
     * {@link com.arrl.radiocraft.common.radio.rfi.LightningStatic}): it skips the redstone band and config factors
     * but goes out to the clients in the same packet. Server thread only.
     */
    public void addDirectInterference(double amount) {
        pendingDirectInterference += Math.max(0.0D, amount);
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

    /** Ticks of quiet after the last echoed frame before the parrot counts the over as finished and beeps. */
    public static final int PARROT_BEEP_GAP_TICKS = 3;
    private final ArrayDeque<short[]> parrotBeep = new ArrayDeque<>();
    private boolean parrotBeepPending = false;
    private UUID parrotVoiceId = null;

    /**
     * Sends out every queued transmission whose delay has run out, and once an echoed voice over has finished, a
     * courtesy beep like a repeater's. Server tick only.
     */
    protected void tickParrot(ServerLevel serverLevel) {
        long now = serverLevel.getGameTime();
        boolean echoed = false;
        for(QueuedVoice next = parrotVoiceQueue.peek(); next != null && next.dueTick() <= now; next = parrotVoiceQueue.peek()) {
            parrotVoiceQueue.poll();
            acceptVoicePacket(() -> serverLevel, next.audio(), next.sourcePlayer());
            recordParrotEcho(serverLevel);
            echoed = true;
        }
        if(echoed)
            parrotBeepPending = true;
        else if(parrotBeepPending && parrotVoiceQueue.isEmpty() && now - lastParrotEchoGameTime >= PARROT_BEEP_GAP_TICKS) {
            parrotBeepPending = false;
            if(isParrot())
                parrotBeep.addAll(RepeaterAudio.courtesyBeep());
        }
        if(parrotBeep.isEmpty())
            return;
        if(parrotVoiceId == null)
            parrotVoiceId = UUID.nameUUIDFromBytes(("radiocraft-parrot-" + worldPosition.asLong()).getBytes(StandardCharsets.UTF_8));
        int frames = now % 2 == 0 ? 3 : 2; // 2.5 frames of 20 ms per 50 ms tick.
        for(int i = 0; i < frames && !parrotBeep.isEmpty(); i++) {
            acceptVoicePacket(() -> serverLevel, parrotBeep.poll(), parrotVoiceId);
            recordParrotEcho(serverLevel);
        }
    }

    /** True while the parrot has a courtesy beep still to send, for tests. */
    public boolean isParrotBeeping() {
        return !parrotBeep.isEmpty();
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
        if(wasPowered && networkObject instanceof RadioNetworkObject radio)
            radio.setWantsOn(true); // The switch was on; whether it runs again depends on the power, as before.
    }

    // Override this for any additional ticks needed like CWSendBuffers
    protected void additionalTick() {}

    // -------------------- POWER IMPLEMENTATION --------------------

    @Override
    public void toggle() {
        if(!level.isClientSide()) {
            if(getNetworkObject(level, worldPosition) instanceof RadioNetworkObject networkObject) {
                networkObject.setWantsOn(!networkObject.wantsOn); // Settles isPowered at once from the energy on hand.
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
     * Server side entry point for a voice transmission that reached this radio's antenna, on its frequency or near it
     * ({@link Detuning}). Called from the voice chat thread. A signal the radio can hear is recorded (so the server,
     * debug tools and tests can see what arrived) and handed to the {@link BEVoiceReceiver} for playback; one that is
     * only close enough to splatter (FM) is remembered and roughens what the radio hears on its own frequency.
     *
     * @return True if the radio was listening and the packet is heard.
     */
    public boolean receiveVoice(AntennaVoicePacket packet) {
        if(!isListening())
            return false;
        Detuning.Effect effect = getDetuning(packet.getFrequency());
        if(effect.isNone())
            return false;
        boolean skip = isSkipPath(packet);
        double strength = packet.getStrength();
        if(skip && level != null) // Skip signals swell and fade (QSB); line of sight holds steady.
            strength *= ReceiverMix.fading(level.getGameTime() / 20.0D, pathSeed(packet), RadiocraftServerConfig.SKIP_FADING_DEPTH.get());
        if(!effect.audible()) { // The squelch stays shut; the splatter waits for a signal on frequency.
            adjacentQrm = effect.qrm() * strength;
            adjacentQrmGameTime = level == null ? 0L : level.getGameTime();
            return false;
        }
        if(!Ctcss.opens(getToneSquelch(), packet.getCtcssTone()))
            return false; // Tone squelch: not our tone, the speaker stays quiet.
        strength *= effect.strength();
        recordSignal(strength);
        if(isParrot()) // Queue the audio as it arrived, before the receiver scales it, so the echo carries one path's loss, not two.
            parrotVoiceQueue.add(new QueuedVoice(packet.getRawAudio().clone(), packet.getSourcePlayer(), lastSignalGameTime + parrotDelayTicks));
        double rasp = skip && band != null && band.isVhf() ? 1.0D : 0.0D; // A VHF signal beyond line of sight came off the aurora.
        getVoiceReceiver().receive(packet, ReceiverMix.voiceLevel(strength, getNoiseFloor()), rasp,
                effect.pitchShiftHz(), effect.splatter(), Detuning.splatterOn(1.0D, getAdjacentQrm(), strength));
        return true;
    }

    /**
     * True while the receiver takes signals in. Normally that is when it plays them (switched on, voice mode, not
     * transmitting); a repeater listens whenever it is on, and its speaker is a separate switch.
     */
    protected boolean isListening() {
        return getVoiceReceiver().isReceiving();
    }

    /** Ticks a splattering neighbour is remembered for (it sends a frame every 20 ms, so it is still talking). */
    public static final int ADJACENT_QRM_HOLD_TICKS = 2;

    // Splatter from a signal a few kHz off (FM), already scaled by its strength. Written on the voice chat thread.
    private volatile double adjacentQrm = 0.0D;
    private volatile long adjacentQrmGameTime = -1L;

    /** Splatter (scaled by the neighbour's strength) a signal just off frequency is putting on this radio, 0 if none lately. */
    public double getAdjacentQrm() {
        long now = level == null ? 0L : level.getGameTime();
        return adjacentQrmGameTime >= 0 && now - adjacentQrmGameTime <= ADJACENT_QRM_HOLD_TICKS ? adjacentQrm : 0.0D;
    }

    /** Game time a signal just off frequency last splattered onto this radio, -1 if never. */
    public long getAdjacentQrmGameTime() {
        return adjacentQrmGameTime;
    }

    /** How this radio's receiver takes a signal on {@code txHz}: as SSB, or as FM for {@link #isFM() FM} radios. */
    public Detuning.Effect getDetuning(float txHz) {
        if(isFM())
            return Detuning.fm(getTunedFrequency(), txHz, band != null && band.isVhf() ? RadiocraftServerConfig.VHF_FREQUENCY_STEP.get() : 0);
        return Detuning.ssb(getTunedFrequency(), txHz, isLowerSideband());
    }

    /** True if the radio receives voice as FM (VHF radios); SSB otherwise. */
    public boolean isFM() {
        return false;
    }

    /** SSB on bands below 10 MHz (80 and 40m) is lower sideband, above it upper, as hams use it. */
    public boolean isLowerSideband() {
        return band != null && band.minFrequency() < 10_000_000;
    }

    /** True if the packet came from further away than line of sight on this band: by skip (or, on VHF, the aurora). */
    protected boolean isSkipPath(IAntennaPacket packet) {
        if(band == null || packet.getSource() == null || packet.getSource().getAntennaPos() == null)
            return false;
        return packet.getSource().getAntennaPos().position().distSqr(worldPosition) > (double)band.losRange() * band.losRange();
    }

    /** A number for the path between the sender and this radio, so each path fades on its own. */
    private long pathSeed(IAntennaPacket packet) {
        return packet.getSource().getAntennaPos().position().asLong() * 31L + worldPosition.asLong();
    }

    /**
     * The noise this radio's receiver hears, 0 to 1: on HF the solar noise floor plus redstone and lightning noise and
     * any aurora overhead, on VHF the fixed receiver noise ({@link RadiocraftServerConfig#VHF_NOISE_FLOOR}). The client
     * plays the same sum as static ({@link com.arrl.radiocraft.common.sounds.RadioStaticSoundInstance}).
     */
    public double getNoiseFloor() {
        if(level == null)
            return 0.0D;
        if(band != null && band.isVhf())
            return getCrystalEffect().applyNoise(RadiocraftServerConfig.VHF_NOISE_FLOOR.get());
        SolarEventInstance event = SolarEventManager.getEvent(level);
        double noise = (event == null ? 0.0D : event.getEvent().getNoise()) + getInterference();
        if(Aurora.affects(level, worldPosition))
            noise += RadiocraftServerConfig.AURORA_STATIC.get();
        return getCrystalEffect().applyNoise(Math.min(1.0D, noise));
    }

    // -------------------- CRYSTAL --------------------

    /** The crystal the radio was built with: each quality is its own block. */
    public CrystalQuality getCrystalQuality() {
        return getBlockState().getBlock() instanceof RadioBlock radio ? radio.getCrystalQuality() : CrystalQuality.GOOD;
    }

    /**
     * The crystal as the radio behaves: QRP rigs are designed around poor crystals, so they hear and tune like a good
     * one whatever they were built with.
     */
    public CrystalQuality getCrystalEffect() {
        return isQrp() ? CrystalQuality.GOOD : getCrystalQuality();
    }

    /** Hz a poor crystal has the radio off its dial frequency right now, 0 for any other crystal. */
    public double getDriftHz() {
        return level == null ? 0.0D : getCrystalEffect().driftHz(level.getGameTime(), worldPosition.asLong());
    }

    /** Where the radio really is: the dial frequency plus any crystal drift. It listens and transmits here. */
    public float getTunedFrequency() {
        return (float)(frequency + getDriftHz());
    }

    // -------------------- RECEIVED SIGNAL (CLIENT SYNC) --------------------

    /** Ticks the synced signal holds after the last frame, so the static comes back up gently between words. */
    public static final int SIGNAL_SYNC_HOLD_TICKS = 6;

    /**
     * Signal strength the client uses to duck the static under a received signal ({@link ReceiverMix#noiseDuck}),
     * rounded to 1/20 so it is only resent when it really moves.
     */
    protected double syncedSignal = 0.0D;

    public double getSyncedSignal() {
        return syncedSignal;
    }

    private boolean tickSyncedSignal(long gameTime) {
        double current = lastSignalGameTime >= 0 && gameTime - lastSignalGameTime <= SIGNAL_SYNC_HOLD_TICKS ? lastSignalStrength : 0.0D;
        double rounded = Math.round(current * 20.0D) / 20.0D;
        if(current > 0.0D && rounded == 0.0D)
            rounded = 0.05D; // A faint signal still counts.
        if(rounded == syncedSignal)
            return false;
        syncedSignal = rounded;
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
     * Switches the radio to another band (radios with a band switch): the frequency goes to the bottom of the new band,
     * and the antenna match is re-measured on the next tick.
     */
    protected void changeBand(Band newBand) {
        band = newBand;
        frequency = newBand.minFrequency();
        if(level != null && !level.isClientSide())
            updateIsReceiving();
        updateBlock();
        setChanged();
    }

    /**
     * Move the current frequency by the specified number of steps as defined in {@link RadiocraftServerConfig}.
     *
     * @param stepCount The number of steps to increment by.
     */
    public void updateFrequency(int stepCount) {
        frequency = snapToStep(band, frequency, getFrequencyStep(), stepCount);
        setChanged();
        updateBlock();
    }

    /** Hz the frequency dial moves per click: the HF step, or the VHF channel step on VHF radios. */
    public int getFrequencyStep() {
        return RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
    }

    /**
     * {@code frequency} moved {@code stepCount} steps and put back on the band's step grid, inside the band. Counted in
     * whole steps from the bottom of the band, because a float cannot hold every step exactly at 2m (144.001 MHz is
     * stored as 144.001008): adding steps to the float and checking it against the grid would never land on it.
     */
    public static float snapToStep(Band band, float frequency, int step, int stepCount) {
        long maxSteps = (long)((band.maxFrequency() - band.minFrequency()) / step); // Whole steps only, so the top never takes a half step.
        long steps = Math.round(((double)frequency - band.minFrequency()) / step) + stepCount;
        steps = Math.max(0L, Math.min(maxSteps, steps));
        return (float)(band.minFrequency() + (double)steps * step);
    }

    @Override
    public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        transmitVoice(level, rawAudio, (float)(getTransmitFrequency() + getDriftHz()), sourcePlayer, getTransmitTone());
    }

    /**
     * Puts one voice frame on the air on {@code txHz}: out of the antenna, or out of the bare connector (a few blocks)
     * when nothing is on the coax. More than one antenna overdraws the radio instead.
     */
    protected void transmitVoice(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, float txHz, UUID sourcePlayer, float ctcssTone) {
        if(!(IBENetworks.getObject(this.level, worldPosition) instanceof RadioNetworkObject networkObject))
            return;
        List<AntennaNetworkObject> antennas = networkObject.getAntennas();
        if(antennas.size() == 1)
            antennas.get(0).transmitAudioPacket(level, rawAudio, band, txHz, sourcePlayer, ctcssTone);
        else if(antennas.size() > 1)
            overdraw();
        else {
            BareConnectorAntenna bare = bareConnector;
            if(bare != null)
                bare.transmitAudioPacket(level, rawAudio, band, txHz, sourcePlayer, ctcssTone);
        }
    }

    /** Where the radio transmits: its frequency, or with a repeater offset on VHF radios. */
    public float getTransmitFrequency() {
        return frequency;
    }

    /** CTCSS tone the radio sends under its voice, 0 for none. */
    public float getTransmitTone() {
        return 0.0F;
    }

    /** CTCSS tone the receiver needs to open its squelch, 0 for none (it opens for anything). */
    public float getToneSquelch() {
        return 0.0F;
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

    /**
     * True while the receiver side of the radio is muted because it is transmitting: a rig cannot hear while PTT is
     * down. Clientside this follows the synced PTT flag, so every player near the radio hears it go quiet.
     */
    public boolean isReceiverMuted() {
        return isPTTDown;
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
        nbt.putBoolean("pttDown", isPTTDown);
        nbt.putDouble("signal", syncedSignal);
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
        isPTTDown = nbt.getBoolean("pttDown");
        syncedSignal = nbt.getDouble("signal");

        if(band != null) { // Reset the frequency if the saved one is outside the band, and put it back on a step.
            if(frequency > band.maxFrequency() || frequency < band.minFrequency())
                frequency = band.minFrequency();
            else
                frequency = snapToStep(band, frequency, getFrequencyStep(), 0);
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
            isPTTDown = false; // Nobody has the GUI open on a freshly loaded radio, whatever a crash mid-transmission saved.
            updateIsReceiving(); // Re-arm voice receive for a radio that was saved switched on in SSB.
            updateBlock();
        }
    }

    @Override
    public void setRemoved() {
        if(!level.isClientSide()) {
            VoiceTransmitters.removeListener(level, this); // Stop listening when removed.
            dropBareConnector();
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
            dropBareConnector();
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

    /** Menu data slot: the power switch, 1 when the player has switched the radio on. */
    public static final int DATA_SWITCH = 0;
    /** Menu data slot: what the front panel shows, a {@code RadioNetworkObject.PHASE_*} value. */
    public static final int DATA_DISPLAY_PHASE = 1;
    public static final int DATA_COUNT = 2;

    public ContainerData getDataSlots() {
        if(level.isClientSide)
            return new SimpleContainerData(DATA_COUNT);

        RadioNetworkObject networkObject = (RadioNetworkObject)IBENetworks.getObject(level, worldPosition);
        return networkObject == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case DATA_SWITCH -> networkObject.wantsOn ? 1 : 0;
                    case DATA_DISPLAY_PHASE -> networkObject.getDisplayPhase();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {} // This doesn't need a setter-- use packets instead. NetworkObject isn't on client.

            @Override
            public int getCount() {
                return DATA_COUNT;
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

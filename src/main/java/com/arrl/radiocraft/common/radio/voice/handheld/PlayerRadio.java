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
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link PlayerRadio} acts as a container used for the transmission and receiving of signals for VHF handsets.
 */
public class PlayerRadio implements IVoiceTransmitter, IVoiceReceiver, IAntenna {

    private static final AudioProfiler PROFILER = new AudioProfiler();

    /**
     * Lightweight audio performance tracker
     */
    private static class AudioProfiler {
        private final AtomicLong totalTimeNanos = new AtomicLong(0);
        private final AtomicLong maxTimeNanos = new AtomicLong(0);
        private final AtomicInteger sampleCount = new AtomicInteger(0);
        private final AtomicLong lastReportTime = new AtomicLong(System.currentTimeMillis());

        public void record(double timeMs) {
            long timeNanos = (long)(timeMs * 1_000_000);

            totalTimeNanos.addAndGet(timeNanos);
            sampleCount.incrementAndGet();

            // Update max (simple compare-and-swap)
            long currentMax = maxTimeNanos.get();
            while (timeNanos > currentMax && !maxTimeNanos.compareAndSet(currentMax, timeNanos)) {
                currentMax = maxTimeNanos.get();
            }

            // Check if it's time to report (every 10 seconds)
            long currentTime = System.currentTimeMillis();
            long lastReport = lastReportTime.get();
            if (currentTime - lastReport >= 3_000 && lastReportTime.compareAndSet(lastReport, currentTime)) {
                report();
            }
        }

        private void report() {
            int samples = sampleCount.getAndSet(0);
            long totalNanos = totalTimeNanos.getAndSet(0);
            long maxNanos = maxTimeNanos.getAndSet(0);

            if (samples == 0) return;

            double avgMs = (totalNanos / 1_000_000.0) / samples;
            double maxMs = maxNanos / 1_000_000.0;

            String status = maxMs > 5.0 ? "WARNING" : "OK";

            Radiocraft.LOGGER.info(String.format("Audio Pipeline Health: %s | Processed: %d packets | Avg: %.3fms | Max: %.3fms",
                    status, samples, avgMs, maxMs));

            if (maxMs > 10.0) {
                Radiocraft.LOGGER.warn("Audio processing is taking a long time. This may cause stuttering.");
            }
        }
    }

    private EntityAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private volatile AntennaNetwork network = null;

    private volatile List<SynchronousRadioState> radios = Collections.emptyList();
    //fun fact, all updates to volatile references are atomic inherently! AtomicReference is used here for readability
    //and to ensure programmers understand they don't need to synchronize on PlayerRadio to access this
    private final AtomicReference<AntennaPos> antennaPos = new AtomicReference<>();
    //position and level for voiceChannel;
    private volatile Vec3 voicePosition;
    private volatile Level voiceLevel;

    private boolean isUseHeld;

    public void setUseHeld(boolean useHeld) {
        this.isUseHeld = useHeld;
    }

    public PlayerRadio(Player player) {
        setPlayer(player);
        initializeNoiseSamples();
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

        final int frequency;

        //used for calculating average amplitude for power meter
        volatile long runningSampleSum=0; //stores the sum of the square of every sample since last tick
        volatile long runningSampleCount=0; //number of samples since last tick

        public SynchronousRadioState(ItemStack item, boolean canReceive, boolean canTransmit, int frequency, HandheldLocation itemLocation){
            this.item = item;
            this.canReceive = canReceive;
            this.canTransmit = canTransmit;
            this.frequency = frequency;
            this.itemLocation = itemLocation;
        }

        public SynchronousRadioState(ItemStack item, IVHFHandheldCapability cap, HandheldLocation itemLocation) {
            this(item, cap.isPowered(), cap.isPowered() && cap.isPTTDown(), cap.getFrequencyKiloHertz(), itemLocation);
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
                        out.addFirst(new SynchronousRadioState(itemStack, cap.isPowered(), cap.isPowered() && (cap.isPTTDown() || this.isUseHeld), cap.getFrequencyKiloHertz(), HandheldLocation.HELD));
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
    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, int wavelength, int frequencyKiloHertz) {
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

            short[] audio = rawAudio.clone();

            long startTime = System.currentTimeMillis();
            // Apply narrow-band FM filtering (300-3000 Hz)
            audio = applyNarrowBandFMFilter(audio);
            long endTime = System.currentTimeMillis();

            PROFILER.record(endTime - startTime);

            for(IAntenna antenna : antennas) {
                if(antenna != this) {
                    AntennaVoicePacket packet = new AntennaVoicePacket(level, audio.clone(), wavelength, frequencyKiloHertz, 1.0F, this, sourcePlayer);

                    AntennaPos pos = antenna.getAntennaPos();

                    if(pos == null) continue;

                    double distance = Math.sqrt(thisPos.position().distSqr(pos.position()));
                    packet.setStrength(BandUtils.getBaseStrength(packet.getWavelength(), distance, 1.0F, 0.0F, packet.getLevel().isDay()));

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
        for(SynchronousRadioState radio : currentlyTransmitting) transmitAudioPacket(level, rawAudio, 2, radio.frequency, sourcePlayer);
    }

    @Override
    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if (this.isReceiving()) {
            receive(packet);
        }
    }

    public void setNetwork(AntennaNetwork network) {
        if(this.network != null)
            this.network.removeAntenna(this);
        network.addAntenna(this);
        this.network = network;
    }

    /*

    Note that pretty much everything about the audio processing here needs to go as quickly as possible.
    If you delay this function's completion by even a few milliseconds,
        the audio begins to make chattering/buffering sounds

     */
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

            int packetFrequency = antennaPacket.getFrequency();
            double packetStrength = antennaPacket.getStrength();

            // rawAudio is a 960 sample 48kHz 20ms audio buffer of PCM audio
            //TODO make muffled sounding when not on hotbar (via low pass or the like, not just volume reduction)
            boolean isHeld = false;
            boolean shouldRecieve = false;
            for(SynchronousRadioState state : this.radios) {
                if (state.canReceive && state.frequency == packetFrequency) {
                    shouldRecieve = true;
                    if(state.itemLocation == HandheldLocation.HELD) {
                        isHeld = true;
                    }
                }
            }

            if(!shouldRecieve) return;

            long runningTotal = 0;
            short[] rawAudio = antennaPacket.getRawAudio();
            for(int i = 0; i < rawAudio.length; i++) {
                short sample = (short) Math.round(rawAudio[i] * packetStrength * (isHeld ? 1f : 0.5f)); // Apply appropriate gain for signal strength
                runningTotal = sample * sample;
                rawAudio[i] = sample;
            }

            //TODO rework receiving to be per handheld, when frequency support is added
            for(SynchronousRadioState state : this.radios) if(state.canReceive && state.frequency == packetFrequency) {
                state.runningSampleCount += rawAudio.length;
                state.runningSampleSum += runningTotal;
            }

            byte[] opusAudio = RadiocraftVoicePlugin.encodingManager.getOrCreate(antennaPacket.getSourcePlayer()).getEncoder().encode(rawAudio);
            receiveChannel.send(opusAudio);
        }
    }


    /**
     * Apply narrow-band FM filtering to simulate 2m ham radio audio characteristics.
     * Filters audio to 300-3000 Hz range using cascaded filters for sharper rolloff.
     * Assumes 48kHz sample rate (common for voice chat systems).
     */
    private short[] applyNarrowBandFMFilter(short[] rawAudio) {
        if (rawAudio == null || rawAudio.length == 0) {
            return rawAudio;
        }

        // Assume 48kHz sample rate (adjust if different)
        final double SAMPLE_RATE = 48000.0;
        final double LOW_FREQ = 300.0;   // High-pass cutoff
        final double HIGH_FREQ = 3000.0; // Low-pass cutoff

        // Filter configuration - increase stages for steeper rolloff
        final int HIGH_PASS_STAGES = 6;  // ~36dB/octave rolloff
        final int LOW_PASS_STAGES = 6;   // ~36dB/octave rolloff

        short[] filteredAudio = rawAudio.clone();

        filteredAudio = applyCascadedHighPassFilter(filteredAudio, LOW_FREQ, SAMPLE_RATE, HIGH_PASS_STAGES);
        filteredAudio = applyCascadedLowPassFilter(filteredAudio, HIGH_FREQ, SAMPLE_RATE, LOW_PASS_STAGES);
        filteredAudio = applyPreEmphasisDeEmphasis(filteredAudio, SAMPLE_RATE);
        filteredAudio = applyRadioCompression(filteredAudio);
        filteredAudio = addSubtleNoise(filteredAudio);

        return filteredAudio;
    }

    /**
     * Calculate high-pass filter coefficient
     */
    private static double calculateHighPassAlpha(double cutoffFreq, double sampleRate) {
        double rc = 1.0 / (2.0 * Math.PI * cutoffFreq);
        double dt = 1.0 / sampleRate;
        return rc / (rc + dt);
    }

    /**
     * Calculate low-pass filter coefficient
     */
    private static double calculateLowPassAlpha(double cutoffFreq, double sampleRate) {
        double rc = 1.0 / (2.0 * Math.PI * cutoffFreq);
        double dt = 1.0 / sampleRate;
        return dt / (rc + dt);
    }


    /**
     * Apply cascaded high-pass filters for sharper rolloff
     */
    private short[] applyCascadedHighPassFilter(short[] audio, double cutoffFreq, double sampleRate, int stages) {
        short[] result = audio.clone();
        double alpha = calculateHighPassAlpha(cutoffFreq, sampleRate);

        for (int stage = 0; stage < stages; stage++) {
            double prev = 0;
            double output = 0;

            for (int i = 0; i < result.length; i++) {
                double input = result[i];
                output = alpha * (output + input - prev);
                prev = input;
                result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(output)));
            }
        }
        return result;
    }


    /**
     * Apply cascaded low-pass filters for sharper rolloff
     */
    private short[] applyCascadedLowPassFilter(short[] audio, double cutoffFreq, double sampleRate, int stages) {
        short[] result = audio.clone();
        double alpha = calculateLowPassAlpha(cutoffFreq, sampleRate);

        for (int stage = 0; stage < stages; stage++) {
            double output = 0;

            for (int i = 0; i < result.length; i++) {
                double input = result[i];
                output = output + alpha * (input - output);
                result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(output)));
            }
        }
        return result;
    }


    /**
     * Apply pre-emphasis/de-emphasis characteristics typical of FM radio
     */
    private short[] applyPreEmphasisDeEmphasis(short[] audio, double sampleRate) {
        // Simple high-frequency emphasis followed by de-emphasis
        double tau = 75e-6; // 75 microsecond time constant (standard for FM)
        double alpha = 1.0 / (1.0 + tau * sampleRate);

        short[] result = new short[audio.length];
        double prev = 0;
        double output = 0;

        // Pre-emphasis (high-frequency boost)
        for (int i = 0; i < audio.length; i++) {
            double input = audio[i];
            double emphasized = input - alpha * prev;
            prev = input;
            result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(emphasized)));
        }

        // De-emphasis (high-frequency rolloff)
        for (int i = 0; i < result.length; i++) {
            double input = result[i];
            output = output + alpha * (input - output);
            result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(output)));
        }

        return result;
    }

    /**
     * Apply radio-style compression and limiting
     */
    private short[] applyRadioCompression(short[] audio) {
        short[] result = new short[audio.length];

        for (int i = 0; i < audio.length; i++) {
            double sample = audio[i];
            double normalizedSample = sample / 32768.0;

            // Apply soft compression using a curve
            double compressedSample;
            if (Math.abs(normalizedSample) > 0.5) {
                // Compress signals above 50% level
                double sign = Math.signum(normalizedSample);
                double magnitude = Math.abs(normalizedSample);
                compressedSample = sign * (0.5 + 0.5 * Math.tanh((magnitude - 0.5) * 3.0));
            } else {
                compressedSample = normalizedSample;
            }

            // Apply hard limiting at 90% to prevent clipping
            compressedSample = Math.max(-0.9, Math.min(0.9, compressedSample));

            result[i] = (short) Math.round(compressedSample * 32768.0);
        }

        return result;
    }

    private static final int EXPECTED_BUFFER_SIZE = 960; // 20ms at 48kHz
    private static final int NOISE_SAMPLES_COUNT = 10;
    private static double[][] preGeneratedNoise = null;

    /**
     * Initialize pre-generated noise samples on first use
     */
    private static void initializeNoiseSamples() {
        if (preGeneratedNoise != null) return;
        preGeneratedNoise = new double[NOISE_SAMPLES_COUNT][];
        for (int i = 0; i < NOISE_SAMPLES_COUNT; i++) {
            preGeneratedNoise[i] = generateFilteredNoise(EXPECTED_BUFFER_SIZE, 48000.0);
        }
    }

    /**
     * Add subtle background noise typical of radio communications
     * Optimized for 960-sample buffers (20ms at 48kHz)
     */
    private short[] addSubtleNoise(short[] audio) {
        // Validate expected buffer size
        if (audio.length != EXPECTED_BUFFER_SIZE) {
            Radiocraft.LOGGER.error(String.format("Unexpected audio buffer size: %d (expected %d). Noise generation will be slower.",
                    audio.length, EXPECTED_BUFFER_SIZE));
            // Fall back to dynamic generation for unexpected sizes
            return addSubtleNoiseFallback(audio);
        }

        // Initialize noise samples if needed
        initializeNoiseSamples();

        short[] result = new short[audio.length];
        double noiseLevel = 30.0; // Reduced level for filtered noise

        // Select random pre-generated noise sample
        int noiseIndex = Radiocraft.RANDOM.nextInt(NOISE_SAMPLES_COUNT);
        double[] noiseBuffer = preGeneratedNoise[noiseIndex];

        // Apply noise (optimized for exact size match)
        for (int i = 0; i < audio.length; i++) {
            double noise = noiseBuffer[i] * noiseLevel;
            double sample = audio[i] + noise;
            result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
        }

        return result;
    }

    /**
     * Fallback noise generation for unexpected buffer sizes
     */
    private short[] addSubtleNoiseFallback(short[] audio) {
        short[] result = new short[audio.length];
        double noiseLevel = 30.0;

        // Generate noise dynamically for non-standard buffer sizes
        double[] noiseBuffer = generateFilteredNoise(audio.length, 48000.0);

        for (int i = 0; i < audio.length; i++) {
            double noise = noiseBuffer[i] * noiseLevel;
            double sample = audio[i] + noise;
            result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
        }

        return result;
    }

    /**
     * Generate filtered noise with characteristics typical of radio background noise
     * Concentrates energy in lower frequencies (pink noise tendency)
     */
    private static double[] generateFilteredNoise(int length, double sampleRate) {
        double[] noise = new double[length];

        // Generate white noise first
        for (int i = 0; i < length; i++) {
            noise[i] = Radiocraft.RANDOM.nextGaussian();
        }


        // Apply simple pink noise filter (1/f rolloff)
        // This concentrates more energy in lower frequencies
        double[] filtered = new double[length];
        double b0 = 0.99886, b1 = -0.99886, a1 = -0.99772;
        double x1 = 0, y1 = 0;

        for (int i = 0; i < length; i++) {
            double x0 = noise[i];
            double y0 = b0 * x0 + b1 * x1 + a1 * y1;
            filtered[i] = y0;
            x1 = x0;
            y1 = y0;
        }

        // Apply additional low-pass filtering to further concentrate in lower frequencies
        // This simulates the characteristic "hiss" of radio background noise
        double cutoff = 1500.0; // Roll off above 1.5kHz
        double alpha = calculateLowPassAlpha(cutoff, sampleRate);
        double output = 0;

        for (int i = 0; i < length; i++) {
            output = output + alpha * (filtered[i] - output);
            filtered[i] = output;
        }

        return filtered;
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

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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


    private LocationalAudioChannel receiveChannel = null;
    private WeakReference<Player> playerRef = null; // Use a weak ref here, so it isn't able to permanently load the entity.
    private AntennaNetwork network = null;
    private Level currentlevel; //keeps track of the current level, used only to reset LocationalAudioChannel receiveChannel when the player changes dimension

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

        Player old = playerRef == null ? null : playerRef.get();
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
    public void receive(AntennaVoicePacket antennaPacket) {
        if(isReceiving()) {
            Player player = getPlayer();
            if(player == null){
                Radiocraft.LOGGER.error("receiving but player is null?");
                return;
            }

            if(receiveChannel == null)
                if(!openChannel()) return; //if the channel cannot be opened, return early

            //if entityChannel can be gotten working again, this is where you check to make sure it's still bound to the player
            //most likely redundant with the onPlayerCloned hook in PlayerRadioManager calling set player
//            if(receiveChannel.getEntity().getEntity() != player){
//                Radiocraft.LOGGER.info("receivechannel entity didn't equal this player? entity: " + receiveChannel.getEntity().getEntity() + " player " + player);
//                receiveChannel.updateEntity(RadiocraftVoicePlugin.API.fromEntity(player));
//            }
            if(currentlevel != player.level()){
                receiveChannel.flush();
                receiveChannel = null;
                if(!openChannel()) return;
            }
            receiveChannel.updateLocation(getPosInVoiceApiFormat());

            // rawAudio is a 960 sample 48kHz 20ms audio buffer of PCM audio
            short[] rawAudio = antennaPacket.getRawAudio();

            long startTime = System.currentTimeMillis();
            // Apply narrow-band FM filtering (300-3000 Hz)
            rawAudio = applyNarrowBandFMFilter(rawAudio);
            long endTime = System.currentTimeMillis();

            PROFILER.record(endTime - startTime);

            for(int i = 0; i < rawAudio.length; i++)
                rawAudio[i] = (short)Math.round(rawAudio[i] * antennaPacket.getStrength()); // Apply appropriate gain for signal strength

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
}

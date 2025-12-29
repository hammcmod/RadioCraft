package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.blockentities.radio.IBEVoiceReceiver;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.ITogglableBE;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public abstract class RadioBlockEntity extends BlockEntity implements ITogglableBE, IVoiceTransmitter, IBEVoiceReceiver, INetworkObjectProvider, MenuProvider {

    protected boolean ssbEnabled = false;
    protected boolean isPTTDown = false; // Used by PTT button packets

    protected Band band; // Band the radio is configured for, usually not changed.
    protected float frequency; // Frequency the radio is currently using (in Hz)
    protected volatile float gain = 1.0F;
    protected volatile float micGain = 1.0F;

    protected final BEVoiceReceiver voiceReceiver; // Acts as a container for voip channel info
    protected double antennaSWR; // Used clientside to calculate volume of static, and serverside for overdraw.
    protected boolean wasPowered; // Used for rendering clientside. The NetworkObject is the one actually controlling this.

    protected final AtomicReference<BlockPos> micPos = new AtomicReference<>(); //thread safe position reference, overkill but makes purpose clear
    @Nullable
    private BENetworkObject cachedNetworkObject;

    public RadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
        super(type, pos, state);
        this.micPos.set(pos);
        this.band = band;
        this.frequency = band == null ? 0 : band.minFrequency();
        this.voiceReceiver = new BEVoiceReceiver(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public BENetworkObject getNetworkObject(Level level, BlockPos pos) {
        if(cachedNetworkObject != null)
            return cachedNetworkObject;
        cachedNetworkObject = INetworkObjectProvider.super.getNetworkObject(level, pos);
        return cachedNetworkObject;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if(t instanceof RadioBlockEntity be) {
            RadioNetworkObject networkObject = (RadioNetworkObject)be.getNetworkObject(level, pos);
            if(networkObject != null && networkObject.isPowered) {
                boolean shouldUpdate = false;

                if(be.wasPowered != networkObject.isPowered) {
                    be.wasPowered = networkObject.isPowered;
                    shouldUpdate = true;
                }

                List<AntennaNetworkObject> antennas = networkObject.getAntennas();

                double newSWR = 0.0D;
                if(antennas.size() == 1)
                    newSWR = antennas.get(0).getSWR(be.frequency);
                else if(antennas.size() > 1)
                    newSWR = 10.0D;

                if(newSWR != be.antennaSWR) {
                    be.antennaSWR = newSWR;
                    shouldUpdate = true; // If SWR has changed, notify the client about it.
                }

                if(shouldUpdate)
                    be.updateBlock();
            }
            be.additionalTick();
        }
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
            boolean powered = networkObject.isPowered;
            boolean canTransmit = canTransmitVoice();
            if(canTransmit) {
                networkObject.setTransmitting(true);
                getVoiceReceiver().setReceiving(false);
            }
            else {
                networkObject.setTransmitting(false);
                getVoiceReceiver().setReceiving(powered && ssbEnabled);
            }
        }
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
        setChanged();
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = clampGain(gain);
        setChanged();
    }

    public float getMicGain() {
        return micGain;
    }

    public void setMicGain(float micGain) {
        this.micGain = clampMicGain(micGain);
        setChanged();
    }

    private float clampGain(float gain) {
        float maxGain = RadiocraftServerConfig.HANDHELD_MAX_GAIN.get().floatValue();
        return Mth.clamp(gain, 0.0F, maxGain);
    }

    private float clampMicGain(float micGain) {
        float maxMicGain = RadiocraftServerConfig.HANDHELD_MAX_MIC_GAIN.get().floatValue();
        return Mth.clamp(micGain, 0.0F, maxMicGain);
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
            if(!isPTTDown)
                resetMicPos();
        }
    }

    public void setMicPos(BlockPos pos) {
        if(pos != null)
            micPos.set(pos);
    }

    public void resetMicPos() {
        micPos.set(worldPosition);
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
    }

    @Override
    public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        RadioNetworkObject networkObject = (RadioNetworkObject)getNetworkObject(this.level, worldPosition);
        if(networkObject == null)
            return;
        List<AntennaNetworkObject> antennas = networkObject.getAntennas();
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
     * Use this to save data on radios instead of link BlockEntity#saveAdditional(CompoundTag) as this will also
     * be called by link BlockEntity#getUpdateTag() and don't want to save caps on block updates.
     *
     * TODO probably needs rewrite for changes to codec based serialization
     */
    protected void setupSaveTag(CompoundTag nbt) {
        nbt.putBoolean("ssbEnabled", ssbEnabled);
        nbt.putString("name", band.name());
        nbt.putFloat("frequency", frequency);
        nbt.putFloat("gain", gain);
        nbt.putFloat("micGain", micGain);
        nbt.putDouble("antennaSWR", antennaSWR);
        nbt.putBoolean("wasPowered", wasPowered);
    }

    /**
     * Use this to read data on radios instead of link BlockEntity#load(CompoundTag) as this will also TODO fix link
     * be called by link BlockEntity#handleUpdateTag(CompoundTag) and don't want to write two load methods.
     */
    protected void readSaveTag(CompoundTag nbt) {
        if(nbt.contains("ssbEnabled"))
            ssbEnabled = nbt.getBoolean("ssbEnabled");
        if(nbt.contains("name")) {
            Band loadedBand = Band.getBand(nbt.getString("name"));
            if(loadedBand != null)
                band = loadedBand;
        }
        if(nbt.contains("frequency"))
            frequency = nbt.getFloat("frequency");
        if(nbt.contains("gain"))
            gain = clampGain(nbt.getFloat("gain"));
        if(nbt.contains("micGain"))
            micGain = clampMicGain(nbt.getFloat("micGain"));
        if(nbt.contains("antennaSWR"))
            antennaSWR = nbt.getDouble("antennaSWR");
        if(nbt.contains("wasPowered"))
            wasPowered = nbt.getBoolean("wasPowered");

        if(band != null) {
            if(frequency > band.maxFrequency() || frequency < band.minFrequency())
                frequency = band.minFrequency();
        }
    }

    /**
     * Override this method if you need other sound instances-- for example {@link RadioMorseSoundInstance}. Only called
     * clientside.
     */
    protected void setupSoundInstances() {}

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        setupSaveTag(nbt);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        readSaveTag(nbt);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
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
            updateBlock();
        }
    }

    @Override
    public void setRemoved() {
        if(!level.isClientSide())
            VoiceTransmitters.removeListener(level, this); // Stop listening when removed.
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if(!level.isClientSide())
            VoiceTransmitters.removeListener(level, this); // Stop listening when chunk unloads.
        super.onChunkUnloaded();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /*
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        handleUpdateTag(nbt);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        setupSaveTag(nbt);
        return nbt;
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        readSaveTag(nbt);
    }
*/
    public ContainerData getDataSlots() {
        if(level.isClientSide)
            return new SimpleContainerData(1);

        return new ContainerData() {
            @Override
            public int get(int index) {
                if(index != 0)
                    return 0;
                RadioNetworkObject networkObject = (RadioNetworkObject)getNetworkObject(level, worldPosition);
                return networkObject != null && networkObject.isPowered ? 1 : 0;
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

    public void syncToClient() {
        updateBlock();
    }

}

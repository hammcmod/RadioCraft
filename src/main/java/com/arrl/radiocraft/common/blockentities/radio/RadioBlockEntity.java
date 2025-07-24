package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.blockentities.radio.IBEVoiceReceiver;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.blockentities.ITogglableBE;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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

    protected int wavelength; // Wavelength the frequency is currently on, usually not changed.
    protected int frequency; // Frequency the radio is currently using (in kHz)

    protected final BEVoiceReceiver voiceReceiver; // Acts as a container for voip channel info
    protected double antennaSWR; // Used clientside to calculate volume of static, and serverside for overdraw.
    protected boolean wasPowered; // Used for rendering clientside. The NetworkObject is the one actually controlling this.

    protected final AtomicReference<BlockPos> micPos = new AtomicReference<>(); //thread safe position reference, overkill but makes purpose clear

    public RadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, int wavelength) {
        super(type, pos, state);
        this.micPos.set(pos);
        this.wavelength = wavelength;
        Band band = RadiocraftData.BANDS.getValue(wavelength);
        this.frequency = band == null ? 0 : band.minFrequency();
        this.voiceReceiver = new BEVoiceReceiver(pos.getX(), pos.getY(), pos.getZ());
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
                    newSWR = antennas.get(0).getSWR(be.wavelength);
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
            if(canTransmitVoice()) {
                networkObject.setTransmitting(true);
                getVoiceReceiver().setReceiving(false);
            }
            else {
                networkObject.setTransmitting(false);
                if(ssbEnabled)
                    getVoiceReceiver().setReceiving(true);
            }
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getWavelength() {
        return wavelength;
    }

    public void setWavelength(int wavelength) {
        this.wavelength = wavelength;
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
        Band band = RadiocraftData.BANDS.getValue(wavelength);
        int step = RadiocraftServerConfig.HF_FREQUENCY_STEP.get();
        int min = band.minFrequency();
        int max = (band.maxFrequency() - band.minFrequency()) / step * step + min; // This calc looks weird, but it's integer division, throws away remainder to ensure the freq doesn't do a "half step" to max.

        frequency = Mth.clamp(frequency + step * stepCount, min, max);
        setChanged();
    }

    @Override
    public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        List<AntennaNetworkObject> antennas = ((RadioNetworkObject)IBENetworks.getObject(this.level, worldPosition)).getAntennas();
        if(antennas.size() == 1)
            antennas.get(0).transmitAudioPacket(level, rawAudio, wavelength, frequency, sourcePlayer);
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
        nbt.putInt("wavelength", wavelength);
        nbt.putInt("frequency", frequency);
        nbt.putDouble("antennaSWR", antennaSWR);
        nbt.putBoolean("wasPowered", wasPowered);
    }

    /**
     * Use this to read data on radios instead of link BlockEntity#load(CompoundTag) as this will also TODO fix link
     * be called by link BlockEntity#handleUpdateTag(CompoundTag) and don't want to write two load methods.
     */
    protected void readSaveTag(CompoundTag nbt) {
        ssbEnabled = nbt.getBoolean("ssbEnabled");
        wavelength = nbt.getInt("wavelength");
        frequency = nbt.getInt("frequency");
        antennaSWR = nbt.getDouble("antennaSWR");
        wasPowered = nbt.getBoolean("wasPowered");
    }

    /**
     * Override this method if you need other sound instances-- for example {@link RadioMorseSoundInstance}. Only called
     * clientside.
     */
    protected void setupSoundInstances() {}

    /*
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        setupSaveTag(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        readSaveTag(nbt);
        Band band = RadiocraftData.BANDS.getValue(wavelength);
        if(frequency > band.maxFrequency() || frequency < band.minFrequency() || (frequency - band.minFrequency()) % RadiocraftServerConfig.HF_FREQUENCY_STEP.get() != 0)
            frequency = band.minFrequency(); // Reset frequency if the saved one was either out of bands or not aligned to the correct step size.
    }

     */

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

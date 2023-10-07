package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceReceiver;
import com.arrl.radiocraft.api.blockentities.radio.IVoiceTransmitter;
import com.arrl.radiocraft.client.blockentity.RadioBlockEntityClientHandler;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.Radio;
import com.arrl.radiocraft.common.radio.VoiceTransmitters;
import com.arrl.radiocraft.common.sounds.RadioMorseSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class RadioBlockEntity extends AbstractPowerBlockEntity implements ITogglableBE, IVoiceTransmitter, IVoiceReceiver {

    protected final List<BENetwork.BENetworkEntry> antennas = Collections.synchronizedList(new ArrayList<>());

    protected boolean isPowered = false;
    protected boolean ssbEnabled = false;
    protected boolean isPTTDown = false; // Used by PTT button packets

    protected int wavelength; // Wavelength the frequency is currently on, usually not changed.
    protected int frequency; // Frequency the radio is currently using (in kHz)

    protected final Radio radio; // Acts as a container for voip channel info
    protected final int receiveUsePower;
    protected final int transmitUsePower;

    protected final ContainerData fields = new ContainerData() {

        @Override
        public int get(int index) {
			return switch(index) {
				case 0 -> frequency;
				case 1 -> wavelength;
				default -> 0;
			};
        }

        @Override
        public void set(int index, int value) {
            switch(index) {
                case 0:
                    frequency = value;
                case 1:
                    wavelength = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

    };

    public RadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, int receiveUsePower, int transmitUsePower, int wavelength) {
        super(type, pos, state, transmitUsePower, transmitUsePower);
        this.receiveUsePower = receiveUsePower;
        this.transmitUsePower = transmitUsePower;
        this.wavelength = wavelength;
        this.frequency = RadiocraftData.BANDS.getValue(wavelength).minFrequency();
        this.radio = new Radio(pos.getX(), pos.getY(), pos.getZ());
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if(t instanceof RadioBlockEntity be && be.isPowered) {
            if(!level.isClientSide) {
                if(!be.tryConsumePower(be.getPowerConsumption(), false)) // Turns off if it can't pull enough power for receiving.
                    be.powerOff();
            }
            be.additionalTick();
        }
    }

    // Override this for any additional ticks needed like CWSendBuffers
    protected void additionalTick() {

    }

    // -------------------- POWER IMPLEMENTATION --------------------

    /**
     * Called when radio is turned on via the UI
     */
    public void powerOn() {
        if(tryConsumePower(getReceiveUsePower(), true)) {
            if(ssbEnabled)
                getRadio().setReceiving(true);
        }
    }

    /**
     * Called when the radio is turned off via the UI or has insufficient power
     */
    public void powerOff() {
        getRadio().setReceiving(false);
    }

    @Override
    public void toggle() {
        isPowered = !isPowered;
        if(!level.isClientSide) {
            if(isPowered)
                powerOn();
            else
                powerOff();
            updateBlock();
            setChanged();
        }
    }

    public boolean isPowered() {
        return isPowered;
    }

    public int getReceiveUsePower() {
        return receiveUsePower;
    }

    public int getTransmitUsePower() {
        return transmitUsePower;
    }

    /**
     * @return The power which needs to be consumed in this tick.
     */
    public int getPowerConsumption() {
        return ssbEnabled ? getTransmitUsePower() : getReceiveUsePower();
    }

    public void overdraw() {

    }

    // -------------------- VOICE/RADIO IMPLEMENTATION --------------------

    @Override
    public Radio getRadio() {
        return radio;
    }

    public boolean getSSBEnabled() {
        return ssbEnabled;
    }

    public void setSSBEnabled(boolean value) {
        if(ssbEnabled != value) {
            ssbEnabled = value;
            updateBlock();
            setChanged();
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public boolean isPTTDown() {
        return isPTTDown;
    }

    public void setPTTDown(boolean value) {
        if(isPTTDown != value) {
            isPTTDown = value;
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
        int step = RadiocraftServerConfig.FREQUENCY_STEP.get();
        int min = band.minFrequency();
        int max = (band.maxFrequency() - band.minFrequency()) / step * step + min; // This calc looks weird, but it's integer division, throws away remainder to ensure the freq doesn't do a "half step" to max.

        frequency = Mth.clamp(frequency + step * stepCount, min, max);
        setChanged();
    }

    @Override
    public void acceptVoicePacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        if(antennas.size() == 1)
            ((AntennaBlockEntity)antennas.get(0).getNetworkItem()).transmitAudioPacket(level, rawAudio, wavelength, frequency, sourcePlayer);
        else if(antennas.size() > 1)
            overdraw();
    }

    @Override
    public boolean canTransmitVoice() {
        return ssbEnabled && isPowered && isPTTDown;
    }

    @Override
    public Vec3 getPos() {
        return getBlockPos().getCenter();
    }

    // -------------------- BE NETWORKS IMPLEMENTATION --------------------

    @Override
    public void networkUpdated(BENetwork network) {
        super.networkUpdated(network);

        antennas.clear(); // Recalculate ALL antennas because unable to tell if one was added or removed.
        for(Set<BENetwork> side : networks.values()) {
            for(BENetwork _network : side) {
                if(!(_network instanceof PowerNetwork)) {
                    for(BENetwork.BENetworkEntry entry : _network.getConnections()) {
                        if(entry.getNetworkItem() instanceof AntennaBlockEntity)
                            antennas.add(entry);
                    }
                }
            }
        }
    }

    // -------------------- BE & SYNC IMPLEMENTATION --------------------

    /**
     * Use this to save data on radios instead of {@link BlockEntity#saveAdditional(CompoundTag)} as this will also
     * be called by {@link BlockEntity#getUpdateTag()} and don't want to save caps on block updates.
     */
    protected void setupSaveTag(CompoundTag nbt) {
        nbt.putBoolean("isPowered", isPowered);
        nbt.putBoolean("ssbEnabled", ssbEnabled);
        nbt.putInt("wavelength", wavelength);
        nbt.putInt("frequency", frequency);
    }

    /**
     * Use this to read data on radios instead of {@link BlockEntity#load(CompoundTag)} as this will also
     * be called by {@link BlockEntity#handleUpdateTag(CompoundTag)} and don't want to write two load methods.
     */
    protected void readSaveTag(CompoundTag nbt) {
        isPowered = nbt.getBoolean("isPowered");
        ssbEnabled = nbt.getBoolean("ssbEnabled");
        wavelength = nbt.getInt("wavelength");
        frequency = nbt.getInt("frequency");
    }

    /**
     * Override this method if you need other sound instances-- for example {@link RadioMorseSoundInstance}. Only called
     * clientside.
     */
    protected void setupSoundInstances() {
        RadioBlockEntityClientHandler.startStaticSoundInstance(this);
    }

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
        if(frequency > band.maxFrequency() || frequency < band.minFrequency() || (frequency - band.minFrequency()) % RadiocraftServerConfig.FREQUENCY_STEP.get() != 0)
            frequency = band.minFrequency(); // Reset frequency if the saved one was either out of bands or not aligned to the correct step size.
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level.isClientSide())
            setupSoundInstances();
        else {
            // If on server, register self to listen for voice packets & notify client of loaded data.
            VoiceTransmitters.addListener(level, this);
            updateBlock();
        }
    }

    @Override
    public void setRemoved() {
        if(!level.isClientSide)
            VoiceTransmitters.removeListener(level, this); // Stop listening when removed.
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if(!level.isClientSide)
            VoiceTransmitters.removeListener(level, this); // Stop listening when chunk unloads.
        super.onChunkUnloaded();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

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

    protected void updateBlock() {
        if(level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            // Flag of 2 (0010) causes update to be sent to client, but no actual block updates.
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

}

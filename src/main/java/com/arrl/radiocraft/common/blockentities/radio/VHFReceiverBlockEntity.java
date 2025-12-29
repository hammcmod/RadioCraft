package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class VHFReceiverBlockEntity extends VHFRadioBlockEntity {

    private static final int CHANNEL_COUNT = 6;
    private static final float DEFAULT_CHANNEL_FREQUENCY = 146_520_000.0F;
    private static final EmptyAntennaData IDEAL_DATA = new EmptyAntennaData();

    private final ReceiverAntenna idealAntenna;
    private final float[] channelFrequencies = new float[CHANNEL_COUNT];
    private final float[] channelGains = new float[CHANNEL_COUNT];
    private int selectedChannel = 0;

    public VHFReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.VHF_RECEIVER.get(), pos, state, Band.getBand("2m"));
        this.idealAntenna = new ReceiverAntenna(this);
        float defaultFrequency = clampFrequency(DEFAULT_CHANNEL_FREQUENCY);
        setFrequency(defaultFrequency);
        for(int i = 0; i < CHANNEL_COUNT; i++) {
            channelFrequencies[i] = defaultFrequency;
            channelGains[i] = 1.0F;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.vhf_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new VHFReceiverMenu(id, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level != null && !level.isClientSide()) {
            idealAntenna.syncPosition(worldPosition, level);
            AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).addAntenna(idealAntenna);
            voiceReceiver.setDistanceFromConfig();
        }
    }

    @Override
    public void setRemoved() {
        if(level != null && !level.isClientSide())
            AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).removeAntenna(idealAntenna);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if(level != null && !level.isClientSide())
            AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID).removeAntenna(idealAntenna);
        super.onChunkUnloaded();
    }

    @Override
    public boolean canTransmitVoice() {
        return false;
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.VHF_RECEIVER_TICK.get(), CommonConfig.VHF_RECEIVER_TICK.get()) {
            @Override
            public boolean canPowerOn() {
                return true;
            }

            @Override
            public void tick(Level level, BlockPos pos) {
                // VHF receiver ignores energy for now.
            }
        };
    }

    public int getSelectedChannel() {
        return selectedChannel;
    }

    public float getChannelFrequency(int channel) {
        return isValidChannel(channel) ? channelFrequencies[channel] : frequency;
    }

    public float getChannelGain(int channel) {
        return isValidChannel(channel) ? channelGains[channel] : gain;
    }

    public void selectChannel(int channel) {
        if(!isValidChannel(channel))
            return;
        selectedChannel = channel;
        setFrequencyClamped(channelFrequencies[channel]);
        setGain(channelGains[channel]);
        syncToClient();
    }

    public void adjustChannelFrequency(int stepCount) {
        if(!isValidChannel(selectedChannel))
            return;
        int step = RadiocraftServerConfig.VHF_FREQUENCY_STEP.get();
        setChannelFrequency(selectedChannel, channelFrequencies[selectedChannel] + step * stepCount);
        syncToClient();
    }

    public void setChannelFrequency(int channel, float frequencyHertz) {
        if(!isValidChannel(channel))
            return;
        float clamped = clampFrequency(frequencyHertz);
        channelFrequencies[channel] = clamped;
        setFrequency(clamped);
    }

    public void setChannelGain(int channel, float gain) {
        if(!isValidChannel(channel))
            return;
        setGain(gain);
        channelGains[channel] = getGain();
    }

    public void applyChannelSettings(int channel, float frequencyHertz, float gain) {
        if(!isValidChannel(channel))
            return;
        selectedChannel = channel;
        setChannelFrequency(channel, frequencyHertz);
        setChannelGain(channel, gain);
        syncToClient();
    }

    public void applyChannelGain(float gain) {
        if(!isValidChannel(selectedChannel))
            return;
        setChannelGain(selectedChannel, gain);
        syncToClient();
    }

    public void applyChannelFrequency(float frequencyHertz) {
        if(!isValidChannel(selectedChannel))
            return;
        setChannelFrequency(selectedChannel, frequencyHertz);
        syncToClient();
    }

    private boolean isValidChannel(int channel) {
        return channel >= 0 && channel < CHANNEL_COUNT;
    }

    private float clampFrequency(float frequencyHertz) {
        if(band == null)
            return frequencyHertz;
        return Mth.clamp(frequencyHertz, band.minFrequency(), band.maxFrequency());
    }

    private void setFrequencyClamped(float frequencyHertz) {
        setFrequency(clampFrequency(frequencyHertz));
    }

    private float clampGainValue(float gain) {
        float maxGain = RadiocraftServerConfig.HANDHELD_MAX_GAIN.get().floatValue();
        return Mth.clamp(gain, 0.0F, maxGain);
    }

    @Override
    protected void setupSaveTag(net.minecraft.nbt.CompoundTag nbt) {
        super.setupSaveTag(nbt);
        nbt.putInt("selectedChannel", selectedChannel);
        for(int i = 0; i < CHANNEL_COUNT; i++) {
            nbt.putFloat("channelFreq" + i, channelFrequencies[i]);
            nbt.putFloat("channelGain" + i, channelGains[i]);
        }
    }

    @Override
    protected void readSaveTag(net.minecraft.nbt.CompoundTag nbt) {
        super.readSaveTag(nbt);
        selectedChannel = Mth.clamp(nbt.getInt("selectedChannel"), 0, CHANNEL_COUNT - 1);
        float defaultFrequency = clampFrequency(DEFAULT_CHANNEL_FREQUENCY);
        float defaultGain = gain;
        for(int i = 0; i < CHANNEL_COUNT; i++) {
            String freqKey = "channelFreq" + i;
            String gainKey = "channelGain" + i;
            channelFrequencies[i] = clampFrequency(nbt.contains(freqKey) ? nbt.getFloat(freqKey) : defaultFrequency);
            channelGains[i] = clampGainValue(nbt.contains(gainKey) ? nbt.getFloat(gainKey) : defaultGain);
        }
        frequency = channelFrequencies[selectedChannel];
        gain = channelGains[selectedChannel];
    }

    private void onReceivePacket(AntennaVoicePacket packet) {
        if(!getVoiceReceiver().isReceiving())
            return;
        packet.setStrength(packet.getStrength() * getGain());
        getVoiceReceiver().receive(packet);
    }

    private static final class ReceiverAntenna implements IAntenna {

        private static final IAntennaType<EmptyAntennaData> IDEAL_TYPE = new IdealAntennaType();
        private final AtomicReference<AntennaPos> pos = new AtomicReference<>();
        private final VHFReceiverBlockEntity owner;

        private ReceiverAntenna(VHFReceiverBlockEntity owner) {
            this.owner = owner;
        }

        private void syncPosition(BlockPos pos, Level level) {
            this.pos.set(new AntennaPos(pos, level));
        }

        @Override
        public void transmitAudioPacket(de.maxhenkel.voicechat.api.ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, java.util.UUID sourcePlayer) {
            // VHF receiver is receive-only.
        }

        @Override
        public void receiveAudioPacket(AntennaVoicePacket packet) {
            VHFReceiverBlockEntity radio = owner;
            if(radio == null || radio.isRemoved())
                return;

            AntennaPos origin = pos.get();
            if(origin == null || origin.level() == null || !origin.level().equals(packet.getLevel()))
                return;

            if(!BandUtils.areFrequenciesEqualWithTolerance(radio.getFrequency(), packet.getFrequency(), 1000))
                return;

            radio.onReceivePacket(packet);
        }

        @Override
        public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {
            // CW not supported on VHF receiver.
        }

        @Override
        public void receiveCWPacket(AntennaCWPacket packet) {
            // CW not supported on VHF receiver.
        }

        @Override
        public AntennaPos getAntennaPos() {
            return pos.get();
        }

        @Override
        public IAntennaType<? extends AntennaData> getType() {
            return IDEAL_TYPE;
        }

        @Override
        public AntennaData getData() {
            return IDEAL_DATA;
        }

        @Override
        public Player getPlayer() {
            return null;
        }
    }

    private static final class IdealAntennaType implements IAntennaType<EmptyAntennaData> {

        private static final ResourceLocation ID = Radiocraft.id("ideal_vhf_receiver");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public com.arrl.radiocraft.common.radio.antenna.StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
            return null;
        }

        @Override
        public double getTransmitEfficiency(com.arrl.radiocraft.api.antenna.IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
            return 1.0D;
        }

        @Override
        public double getReceiveEfficiency(com.arrl.radiocraft.api.antenna.IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
            return packet.getStrength();
        }

        @Override
        public double getSWR(EmptyAntennaData data, float frequencyHertz) {
            return 1.0D;
        }

        @Override
        public EmptyAntennaData getDefaultData() {
            return IDEAL_DATA;
        }
    }

}

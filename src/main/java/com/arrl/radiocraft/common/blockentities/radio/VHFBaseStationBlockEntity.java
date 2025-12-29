package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.IAntenna;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class VHFBaseStationBlockEntity extends VHFRadioBlockEntity {

    private static final EmptyAntennaData IDEAL_DATA = new EmptyAntennaData();

    private final BaseStationAntenna idealAntenna;
    private int receiveIndicatorTicks = 0;
    private float receiveIndicatorStrength = 0.0F;

    public VHFBaseStationBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.VHF_BASE_STATION.get(), pos, state, Band.getBand("2m"));
        this.idealAntenna = new BaseStationAntenna(this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.vhf_base_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new VHFBaseStationMenu(id, this);
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
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.VHF_BASE_STATION_RECEIVE_TICK.get(), CommonConfig.VHF_BASE_STATION_TRANSMIT_TICK.get()) {
            @Override
            public boolean canPowerOn() {
                return true;
            }

            @Override
            public void tick(Level level, BlockPos pos) {
                // Base station ignores energy for now.
            }
        };
    }

    @Override
    protected void additionalTick() {
        if(level != null && !level.isClientSide() && receiveIndicatorTicks > 0) {
            receiveIndicatorTicks--;
            if(receiveIndicatorTicks == 0)
                syncToClient();
        }
    }

    public boolean isReceivingSignal() {
        return receiveIndicatorTicks > 0;
    }

    public float getReceiveIndicatorStrength() {
        return receiveIndicatorStrength;
    }

    @Override
    public void acceptVoicePacket(ServerLevel level, short[] rawAudio, UUID sourcePlayer) {
        short[] audio = rawAudio;
        float micGain = getMicGain();
        if(micGain != 1.0F) {
            audio = rawAudio.clone();
            for(int i = 0; i < audio.length; i++) {
                audio[i] = (short)Math.round(audio[i] * micGain);
            }
        }
        idealAntenna.transmitAudioPacket(level, audio, band, frequency, sourcePlayer);
    }

    public void adjustFrequency(int stepCount) {
        int step = RadiocraftServerConfig.VHF_FREQUENCY_STEP.get();
        setFrequencyClamped(frequency + step * stepCount);
    }

    public void setFrequencyClamped(float frequencyHertz) {
        if(band == null) {
            setFrequency(frequencyHertz);
            return;
        }
        float clamped = Mth.clamp(frequencyHertz, band.minFrequency(), band.maxFrequency());
        setFrequency(clamped);
    }

    public void applySettings(float frequencyHertz, float gain, float micGain) {
        setFrequencyClamped(frequencyHertz);
        setGain(gain);
        setMicGain(micGain);
        syncToClient();
    }

    private static final class BaseStationAntenna implements IAntenna {

        private static final IAntennaType<EmptyAntennaData> IDEAL_TYPE = new IdealAntennaType();
        private final AtomicReference<AntennaPos> pos = new AtomicReference<>();
        private final VHFBaseStationBlockEntity owner;

        private BaseStationAntenna(VHFBaseStationBlockEntity owner) {
            this.owner = owner;
        }

        private void syncPosition(BlockPos pos, Level level) {
            this.pos.set(new AntennaPos(pos, level));
        }

        @Override
        public void transmitAudioPacket(ServerLevel level, short[] rawAudio, Band band, float frequencyHertz, UUID sourcePlayer) {
            AntennaNetwork network = AntennaNetworkManager.getNetwork(AntennaNetworkManager.VHF_ID);
            if(network == null)
                return;

            AntennaPos origin = pos.get();
            if(origin == null || origin.level() == null || !origin.level().equals(level.getServerLevel()))
                return;

            Set<IAntenna> antennas;
            Set<IAntenna> masterSet = network.allAntennas();
            synchronized (masterSet) {
                antennas = new HashSet<>(masterSet);
            }

            for(IAntenna antenna : antennas) {
                if(antenna == this)
                    continue;

                AntennaPos destination = antenna.getAntennaPos();
                if(destination == null || destination.level() == null || !destination.level().equals(origin.level()))
                    continue;

                AntennaVoicePacket packet = new AntennaVoicePacket(level, rawAudio.clone(), band, frequencyHertz, 1.0F, this, sourcePlayer);
                double distance = Math.sqrt(origin.position().distSqr(destination.position()));
                packet.setStrength(BandUtils.getBaseStrength(packet.getBand(), distance, 1.0D, 0.0D, packet.getLevel().isDay()));
                antenna.receiveAudioPacket(packet);
            }
        }

        @Override
        public void receiveAudioPacket(AntennaVoicePacket packet) {
            VHFBaseStationBlockEntity radio = owner;
            if(radio == null || radio.isRemoved())
                return;

            AntennaPos origin = pos.get();
            if(origin == null || origin.level() == null || !origin.level().equals(packet.getLevel()))
                return;

            if(!BandUtils.areFrequenciesEqualWithTolerance(radio.getFrequency(), packet.getFrequency(), 1000))
                return;

            radio.onReceivePacket(packet);
            packet.setStrength(packet.getStrength() * radio.getGain());
            radio.getVoiceReceiver().receive(packet);
        }

        @Override
        public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequencyHertz) {
            // CW not supported on VHF base station yet.
        }

        @Override
        public void receiveCWPacket(AntennaCWPacket packet) {
            // CW not supported on VHF base station yet.
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

        private static final ResourceLocation ID = Radiocraft.id("ideal_base_station");

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

    private void onReceivePacket(AntennaVoicePacket packet) {
        receiveIndicatorStrength = (float)Math.min(1.0D, Math.max(0.0D, packet.getStrength()));
        if(receiveIndicatorTicks <= 0) {
            receiveIndicatorTicks = 6;
            syncToClient();
        } else {
            receiveIndicatorTicks = 6;
        }
    }

    @Override
    protected void setupSaveTag(net.minecraft.nbt.CompoundTag nbt) {
        super.setupSaveTag(nbt);
        nbt.putInt("receiveIndicatorTicks", receiveIndicatorTicks);
        nbt.putFloat("receiveIndicatorStrength", receiveIndicatorStrength);
    }

    @Override
    protected void readSaveTag(net.minecraft.nbt.CompoundTag nbt) {
        super.readSaveTag(nbt);
        if(nbt.contains("receiveIndicatorTicks"))
            receiveIndicatorTicks = nbt.getInt("receiveIndicatorTicks");
        if(nbt.contains("receiveIndicatorStrength"))
            receiveIndicatorStrength = nbt.getFloat("receiveIndicatorStrength");
    }

}

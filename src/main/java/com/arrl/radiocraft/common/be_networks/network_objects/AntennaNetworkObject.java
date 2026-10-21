package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public class AntennaNetworkObject extends BENetworkObject implements ICoaxNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("antenna");

    protected final Set<RadioNetworkObject> radios = Collections.synchronizedSet(new HashSet<>());
    /** Antenna tuners sharing a coax run with this antenna; one is enough, more do not stack. */
    protected final Set<AntennaTunerNetworkObject> tuners = Collections.synchronizedSet(new HashSet<>());
    protected StaticAntenna<?> antenna = null;
    protected ResourceLocation networkId;

    public AntennaNetworkObject(Level level, BlockPos pos, ResourceLocation networkId) {
        super(level, pos);
        this.networkId = networkId;
    }

    /** Sends voice out of the antenna. Nothing goes out while the balun has no recognized antenna yet. */
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, Band band, float frequency, UUID sourcePlayer) {
        transmitAudioPacket(level, rawAudio, band, frequency, sourcePlayer, 0.0F);
    }

    /** As above, with a CTCSS tone under the voice (0 for none). */
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, Band band, float frequency, UUID sourcePlayer, float ctcssTone) {
        StaticAntenna<?> antenna = this.antenna; // Read once: voice arrives on Simple Voice Chat's thread.
        if(antenna != null)
            antenna.transmitAudioPacket(level, rawAudio, band, frequency, sourcePlayer, ctcssTone);
    }

    public void receiveAudioPacket(AntennaVoicePacket packet) {
        if(radios.size() == 1) {
            BlockPos checkPos = radios.iterator().next().getPos();
            if(level.isLoaded(checkPos)) {
                if(level.getChunkAt(checkPos).getBlockEntity(checkPos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof RadioBlockEntity radio) {
                    radio.receiveVoice(packet); // The radio decides whether it is close enough in frequency (Detuning).
                }
            }
        }
        else if(radios.size() > 1)
            overdraw(level);
    }

    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequency) {
        if(antenna != null)
            antenna.transmitCWPacket(level, buffers, band, frequency);
    }

    public void receiveCWPacket(AntennaCWPacket packet) {
        if(radios.size() == 1) {
            if(level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(radios.iterator().next().getPos());
                if(be instanceof HFRadioBlockEntity radio) {
                    if(BandUtils.areFrequenciesEqualWithTolerance(radio.getFrequency(), packet.getFrequency(), 1000.0F)) // Only receive if listening to correct frequency.
                        radio.receiveCW(packet);
                }
            }
        }
        else if(radios.size() > 1)
            overdraw(level);
    }

    public void overdraw(Level level) {
        for(BENetworkObject obj : radios) {
            if(level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(obj.getPos());
                if(be instanceof RadioBlockEntity radio)
                    radio.overdraw();
            }
        }
    }

    @Override
    public void onNetworkUpdateAdd(BENetwork network, BENetworkObject object) {
        track(object, true);
    }

    @Override
    public void onNetworkUpdateRemove(BENetwork network, BENetworkObject object) {
        track(object, false);
    }

    @Override
    public void onNetworkAdd(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects())
                track(object, true);
        }
    }

    @Override
    public void onNetworkRemove(BENetwork network) {
        if(network.getType() == BENetwork.COAXIAL_TYPE) {
            for(BENetworkObject object : network.getNetworkObjects())
                track(object, false);
        }
    }

    private void track(BENetworkObject object, boolean add) {
        if(object instanceof RadioNetworkObject radio) {
            if(add) radios.add(radio); else radios.remove(radio);
        }
        else if(object instanceof AntennaTunerNetworkObject tuner) {
            if(add) tuners.add(tuner); else tuners.remove(tuner);
        }
    }

    // -------------------- MATCHING --------------------

    /** True when an antenna tuner shares this antenna's coax run and tuners are enabled in the server config. */
    public boolean hasTuner() {
        return !tuners.isEmpty() && RadiocraftServerConfig.ANTENNA_TUNER_ENABLED.get();
    }

    /** True when more than one radio shares the coax: the station is overdrawn and no match is possible. */
    public boolean isOverdrawn() {
        return radios.size() > 1;
    }

    /**
     * The SWR the antenna itself presents at a frequency, before any tuner: 0 without a recognized antenna,
     * {@link SWRHelper#OVERDRAW_SWR} when more than one radio shares the coax.
     */
    public double getRawSWR(float frequencyHertz) {
        if(isOverdrawn())
            return SWRHelper.OVERDRAW_SWR;
        return antenna == null ? 0 : antenna.getSWR(frequencyHertz);
    }

    /**
     * The SWR a radio on this coax sees at a frequency. With a tuner on the run this is 1.0 for any antenna the tuner
     * can match (see {@link SWRHelper#tunedSWR}); otherwise it is {@link #getRawSWR}. This is what the radio reports,
     * what its static volume follows and what the redstone interference code treats as the station's match.
     */
    public double getSWR(float frequencyHertz) {
        double raw = getRawSWR(frequencyHertz);
        if(isOverdrawn() || !hasTuner())
            return raw;
        return SWRHelper.tunedSWR(raw, RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get());
    }

    /**
     * Fraction of the transmit power that makes it to the antenna at a frequency: the plain mismatch loss, or the
     * tuner's reduced loss when one is on the run (see {@link SWRHelper#tunedEfficiency}).
     */
    public double getMatchEfficiency(float frequencyHertz) {
        double raw = getRawSWR(frequencyHertz);
        if(isOverdrawn() || !hasTuner())
            return SWRHelper.getEfficiencyMultiplier(raw);
        return SWRHelper.tunedEfficiency(raw, RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get(), RadiocraftServerConfig.ANTENNA_TUNER_LOSS.get());
    }

    public StaticAntenna<?> getAntenna() {
        return antenna;
    }

    public void setAntenna(StaticAntenna<?> antenna) {
        AntennaNetwork network = AntennaNetworkManager.getNetwork(networkId);

        if(this.antenna != null)
            network.removeAntenna(this.antenna);

        this.antenna = antenna;
        network.addAntenna(this.antenna);
        antenna.setNetwork(network);
    }

    /** No antenna is recognized at the balun any more: take the old one off the air. */
    public void clearAntenna() {
        if(antenna != null) {
            AntennaNetwork network = AntennaNetworkManager.getNetwork(networkId);
            if(network != null)
                network.removeAntenna(antenna);
            antenna = null;
        }
    }

    /** The block is gone: take the antenna off the air so transmissions stop being routed to this position. */
    @Override
    public void discard() {
        super.discard();
        clearAntenna();
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        if(antenna != null) {
            nbt.putString("antennaType", antenna.type.getId().toString());
            nbt.put("antennaData", antenna.serializeNBT(null));
        }
        nbt.putString("networkId", networkId.toString());
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        if(nbt.contains("antennaType")) {
            IAntennaType<?> type = AntennaTypes.getType(ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, nbt.getString("antennaType")));
            if(type != null) {
                antenna = new StaticAntenna<>(type, pos, level);
                antenna.deserializeNBT(null, nbt.getCompound("antennaData"));
            }
        }
        networkId = ResourceLocation.fromNamespaceAndPath(Radiocraft.MOD_ID, nbt.getString("networkId"));
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}

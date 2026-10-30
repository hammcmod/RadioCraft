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
import com.arrl.radiocraft.common.radio.DuplexerMath;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.antenna.AntennaCWPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaDataPacket;
import com.arrl.radiocraft.common.radio.antenna.AntennaNetwork;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.arrl.radiocraft.common.radio.morse.CWBuffer;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.arrl.radiocraft.common.blockentities.DirectionFinderModuleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AntennaNetworkObject extends BENetworkObject implements ICoaxNetworkObject {

    public static final ResourceLocation TYPE = Radiocraft.id("antenna");

    protected final Set<RadioNetworkObject> radios = Collections.synchronizedSet(new HashSet<>());
    /** Antenna tuners (manual or automatic) sharing a coax run with this antenna; one is enough, more do not stack. */
    protected final Set<ITuner> tuners = Collections.synchronizedSet(new HashSet<>());
    /** Duplexers on the run (a repeater's); their cavities filter what goes out and what comes in. */
    protected final Set<DuplexerNetworkObject> duplexers = Collections.synchronizedSet(new HashSet<>());
    /** Receive-only splitters on the run: every radio behind one hears the antenna, and none can transmit through it. */
    protected final Set<AntennaSplitterNetworkObject> splitters = Collections.synchronizedSet(new HashSet<>());
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
        transmitAudioPacket(level, rawAudio, band, frequency, sourcePlayer, ctcssTone, 1.0F);
    }

    /** As above, from a transmitter {@code power} times as strong as a standard one. */
    public void transmitAudioPacket(ServerLevel level, short[] rawAudio, Band band, float frequency, UUID sourcePlayer, float ctcssTone, float power) {
        StaticAntenna<?> antenna = this.antenna; // Read once: voice arrives on Simple Voice Chat's thread.
        if(antenna != null && !isSplit())
            antenna.transmitAudioPacket(level, rawAudio, band, frequency, sourcePlayer, ctcssTone, power);
    }

    public void receiveAudioPacket(AntennaVoicePacket packet) {
        packet.setStrength(packet.getStrength() * receivePass(packet.getFrequency()) * splitShare());
        for(BlockPos checkPos : receivers()) {
            if(level.isLoaded(checkPos)) {
                BlockEntity be = level.getChunkAt(checkPos).getBlockEntity(checkPos, LevelChunk.EntityCreationType.IMMEDIATE);
                if(be instanceof RadioBlockEntity radio) {
                    radio.receiveVoice(packet); // The radio decides whether it is close enough in frequency (Detuning).
                }
                else if(be instanceof DirectionFinderModuleBlockEntity module)
                    module.hear(packet);
            }
        }
        if(isOverdrawn())
            overdraw(level);
    }

    /** True when a receive-only splitter is on the run: nothing transmits through this antenna. */
    public boolean isSplit() {
        return !splitters.isEmpty();
    }

    /**
     * What each radio behind a splitter gets of the signal: 1/sqrt(n) for n radios (3 dB lost each time the signal is
     * split in two), all of it with no splitter.
     */
    public double splitShare() {
        int n = radios.size();
        return isSplit() && n > 1 ? 1.0D / Math.sqrt(n) : 1.0D;
    }

    /** The radios that hear this antenna: the one on the coax, or every one behind a splitter; none when overdrawn. */
    private List<BlockPos> receivers() {
        if(radios.isEmpty() || isOverdrawn())
            return List.of();
        synchronized(radios) {
            return radios.stream().map(BENetworkObject::getPos).toList();
        }
    }

    public void transmitCWPacket(net.minecraft.server.level.ServerLevel level, Collection<CWBuffer> buffers, Band band, float frequency) {
        if(antenna != null && !isSplit())
            antenna.transmitCWPacket(level, buffers, band, frequency);
    }

    public void receiveCWPacket(AntennaCWPacket packet) {
        for(BlockPos checkPos : receivers()) {
            if(level.isLoaded(checkPos)) {
                BlockEntity be = level.getBlockEntity(checkPos);
                if(be instanceof HFRadioBlockEntity radio) {
                    if(BandUtils.areFrequenciesEqualWithTolerance(radio.getTunedFrequency(), packet.getFrequency(), 1000.0F)) // Only receive if listening to correct frequency.
                        radio.receiveCW(packet);
                }
                else if(be instanceof DirectionFinderModuleBlockEntity module)
                    module.hear(packet);
            }
        }
        if(isOverdrawn())
            overdraw(level);
    }

    /** Sends a digital interface's packet out of the antenna, like voice. */
    public void transmitDataPacket(net.minecraft.server.level.ServerLevel level, TncPacket payload, Band band, float frequency) {
        StaticAntenna<?> antenna = this.antenna;
        if(antenna != null && !isSplit())
            antenna.transmitDataPacket(level, payload, band, frequency);
    }

    /** Hands a data packet to the radio on this coax, which decides whether it decodes it. */
    public void receiveDataPacket(AntennaDataPacket packet) {
        packet.setStrength(packet.getStrength() * receivePass(packet.getFrequency()) * splitShare());
        for(BlockPos checkPos : receivers()) {
            if(level.isLoaded(checkPos) && level.getBlockEntity(checkPos) instanceof RadioBlockEntity radio)
                radio.receiveData(packet);
            else if(level.isLoaded(checkPos) && level.getBlockEntity(checkPos) instanceof DirectionFinderModuleBlockEntity module)
                module.hear(packet);
        }
        if(isOverdrawn())
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
        else if(object instanceof ITuner tuner) {
            if(add) tuners.add(tuner); else tuners.remove(tuner);
        }
        else if(object instanceof DuplexerNetworkObject duplexer) {
            if(add) duplexers.add(duplexer); else duplexers.remove(duplexer);
        }
        else if(object instanceof AntennaSplitterNetworkObject splitter) {
            if(add) splitters.add(splitter); else splitters.remove(splitter);
        }
    }

    // -------------------- MATCHING --------------------

    /** True when an antenna tuner shares this antenna's coax run and tuners are enabled in the server config. */
    public boolean hasTuner() {
        return getTuner() != null;
    }

    /**
     * True when more than one radio shares the coax with no splitter: the station is overdrawn and no match is
     * possible. Behind a receive-only splitter they all listen.
     */
    public boolean isOverdrawn() {
        return radios.size() > 1 && !isSplit();
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

    /** The tuner on the run, null without one (or with tuners turned off in the config). */
    @Nullable
    public ITuner getTuner() {
        if(!RadiocraftServerConfig.ANTENNA_TUNER_ENABLED.get())
            return null;
        synchronized(tuners) {
            return tuners.isEmpty() ? null : tuners.iterator().next();
        }
    }

    /** The duplexer on the run, null without one. */
    @Nullable
    public DuplexerNetworkObject getDuplexer() {
        synchronized(duplexers) {
            return duplexers.isEmpty() ? null : duplexers.iterator().next();
        }
    }

    /** The match through the tuner on the run, or the antenna as it is without one (or when overdrawn). */
    private ITuner.Match match(float frequencyHertz) {
        double raw = getRawSWR(frequencyHertz);
        ITuner tuner = isOverdrawn() ? null : getTuner();
        return tuner == null ? ITuner.Match.bypass(raw) : tuner.match(this, raw, frequencyHertz);
    }

    /**
     * The SWR a radio on this coax sees at a frequency: the antenna's own ({@link #getRawSWR}), through the tuner on
     * the run if there is one (how well the manual tuner's dials are set, or what the automatic tuner managed), and
     * through a duplexer's transmit cavities. This is what the radio reports, what its static volume follows, what can
     * blow it and what the redstone interference code treats as the station's match.
     */
    public double getSWR(float frequencyHertz) {
        double swr = match(frequencyHertz).swr();
        DuplexerNetworkObject duplexer = getDuplexer();
        return duplexer == null ? swr : DuplexerMath.combine(swr, duplexer.swr(frequencyHertz));
    }

    /**
     * Fraction of the transmit power that makes it to the antenna at a frequency: the mismatch loss through any tuner
     * (see {@link ITuner.Match}), times what a duplexer's transmit cavities let through.
     */
    public double getMatchEfficiency(float frequencyHertz) {
        double efficiency = match(frequencyHertz).efficiency();
        DuplexerNetworkObject duplexer = getDuplexer();
        return duplexer == null ? efficiency : efficiency * duplexer.txPass(frequencyHertz);
    }

    /** True when an automatic tuner on the run has given up on the antenna: the radio shows TUNE FAIL. */
    public boolean isTuneFailed() {
        ITuner tuner = getTuner();
        return tuner != null && tuner.isTuneFailed();
    }

    /** What a duplexer's receive cavities let through of a signal at {@code frequencyHertz}; 1 without one. */
    private double receivePass(float frequencyHertz) {
        DuplexerNetworkObject duplexer = getDuplexer();
        return duplexer == null ? 1.0D : duplexer.rxPass(frequencyHertz);
    }

    public StaticAntenna<?> getAntenna() {
        return antenna;
    }

    /** How often (ticks) the antenna checks it is still where transmissions reach it. */
    private static final int CHECK_TICKS = 40;

    /**
     * Keeps the antenna on the air: it must be on the antenna network its block belongs to (a VHF antenna on VHF) and in
     * that network's set, or nothing transmitted reaches it and its radio goes deaf without a word. Puts it right if
     * not; cheap, every couple of seconds.
     */
    @Override
    public void tick(Level level, BlockPos pos) {
        if((level.getGameTime() + pos.asLong()) % CHECK_TICKS != 0L)
            return;
        if(level.getBlockEntity(pos) instanceof com.arrl.radiocraft.common.blockentities.AntennaBlockEntity be
                && be.getClass() == com.arrl.radiocraft.common.blockentities.AntennaBlockEntity.class) {
            ResourceLocation wanted = com.arrl.radiocraft.common.blockentities.AntennaBlockEntity.networkFor(be.getBlockState());
            if(!wanted.equals(networkId)) {
                Radiocraft.LOGGER.warn("Antenna at {} was on the {} antenna network; moving it to {}", pos.toShortString(), networkId, wanted);
                setNetworkId(wanted);
            }
        }
        StaticAntenna<?> current = antenna;
        AntennaNetwork network = AntennaNetworkManager.getNetwork(networkId);
        if(current != null && network != null && !network.allAntennas().contains(current)) {
            Radiocraft.LOGGER.warn("Antenna at {} had dropped off the {} antenna network; putting it back", pos.toShortString(), networkId);
            current.setNetwork(network);
        }
    }

    /** One line for /rcdebug: which antenna network, whether the antenna is on it, and the radios on its coax. */
    public String diagnose() {
        StaticAntenna<?> current = antenna;
        AntennaNetwork network = AntennaNetworkManager.getNetwork(networkId);
        boolean onAir = current != null && network != null && network.allAntennas().contains(current);
        StringBuilder radiosText = new StringBuilder();
        for(RadioNetworkObject radio : getRadios())
            radiosText.append(radiosText.isEmpty() ? "" : ", ").append(radio.getType().getPath()).append('@').append(radio.getPos().toShortString());
        return "antennaNetwork=" + networkId + " onTheAir=" + onAir + " radiosOnCoax=" + getRadios().size()
                + (radiosText.isEmpty() ? "" : " (" + radiosText + ")") + (getRadios().size() > 1 ? " OVERDRAWN: nothing is heard" : "");
    }

    public ResourceLocation getNetworkId() {
        return networkId;
    }

    /** Moves this antenna to another antenna network (HF or VHF), taking its antenna, if any, along. */
    public void setNetworkId(ResourceLocation id) {
        if(id.equals(networkId))
            return;
        networkId = id;
        if(antenna != null)
            antenna.setNetwork(AntennaNetworkManager.getNetwork(id));
    }

    /** The radios on this antenna's coax. */
    public List<RadioNetworkObject> getRadios() {
        synchronized(radios) {
            return List.copyOf(radios);
        }
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

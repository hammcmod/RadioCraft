package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.SWRHelper;
import com.arrl.radiocraft.common.radio.TunerMath;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The automatic antenna tuner on a coax run. It runs on power ({@code auto_tuner_power_use} a tick); unpowered it is
 * just a piece of coax. When the radio on its run moves to a new band or {@link TunerMath#RETUNE_HZ} or more, or the
 * antenna changes, it tunes: the relays click faster and faster for a second ({@link TunerMath#TUNE_TICKS}) while the
 * radio sees {@link TunerMath#TUNING_SWR}, then the match is perfect. An antenna past the tuner's range
 * ({@code antenna_tuner_max_swr}) keeps it clicking slowly for {@link TunerMath#FAIL_SEARCH_TICKS} until it gives up:
 * the radio shows TUNE FAIL and sees the antenna as it is, and may still transmit at its own risk.
 */
public class AutoTunerNetworkObject extends PowerNetworkObject implements ICoaxPassThrough, ITuner {

    public static final ResourceLocation TYPE = Radiocraft.id("auto_tuner");

    public enum State { IDLE, TUNING, SEARCHING, TUNED, FAILED }

    private static final int STORAGE = 100;

    private volatile boolean powered = false;
    private volatile State state = State.IDLE;
    private int stateTicks = 0;
    private float tunedHz = 0.0F;
    private String tunedBand = null;
    private long tunedAntenna = Long.MIN_VALUE;

    public AutoTunerNetworkObject(Level level, BlockPos pos) {
        super(level, pos, STORAGE, STORAGE, STORAGE);
    }

    @Override
    public boolean isIndirectConsumer() {
        return true;
    }

    @Override
    public boolean isDirectConsumer() {
        return false;
    }

    public State getState() {
        return state;
    }

    public boolean isPowered() {
        return powered;
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        if(level.isClientSide)
            return;
        powered = !RadiocraftServerConfig.RADIO_POWER_REQUIRED.get() || tryConsumeEnergy(RadiocraftServerConfig.AUTO_TUNER_POWER_USE.get(), false);
        if(!powered) {
            state = State.IDLE; // Forgets its match: it tunes again when the power comes back.
            tunedBand = null;
            return;
        }

        RadioBlockEntity radio = poweredRadio(level);
        List<AntennaNetworkObject> antennas = getAntennas();
        AntennaNetworkObject antenna = antennas.size() == 1 ? antennas.getFirst() : null;
        if(radio == null || antenna == null || antenna.getAntenna() == null)
            return; // Nothing to tune: keep the last match for when it comes back.

        float hz = radio.getSwrFrequency();
        Band band = Band.getBand(hz);
        String bandName = band == null ? "" : band.name();
        long antennaId = antenna.getPos().asLong() * 31L + antenna.getAntenna().type.getId().hashCode();
        double raw = antenna.getRawSWR(hz);
        boolean inRange = raw <= RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get();

        if(state == State.IDLE || antennaId != tunedAntenna || TunerMath.needsRetune(tunedHz, tunedBand, hz, bandName)) {
            tunedHz = hz;
            tunedBand = bandName;
            tunedAntenna = antennaId;
            state = inRange ? State.TUNING : State.SEARCHING;
            stateTicks = 0;
        }

        switch(state) {
            case TUNING, SEARCHING -> {
                boolean failing = state == State.SEARCHING;
                if(TunerMath.clicksAt(stateTicks, failing))
                    level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.6F, failing ? 0.9F : 1.1F + 0.02F * stateTicks);
                stateTicks++;
                if(stateTicks >= (failing ? TunerMath.FAIL_SEARCH_TICKS : TunerMath.TUNE_TICKS))
                    state = failing ? State.FAILED : State.TUNED;
            }
            case TUNED -> {
                if(!inRange) // Drifted out of range without a big enough move to retune: it cannot hold the match.
                    state = State.FAILED;
            }
            default -> {}
        }
    }

    /** The first switched-on radio on this tuner's coax, null if none. */
    private RadioBlockEntity poweredRadio(Level level) {
        for(RadioNetworkObject candidate : getRadios()) {
            if(candidate.isPowered && level.getBlockEntity(candidate.getPos()) instanceof RadioBlockEntity radio)
                return radio;
        }
        return null;
    }

    @Override
    public Match match(AntennaNetworkObject antenna, double rawSwr, float frequencyHertz) {
        if(!powered || rawSwr <= 0.0D)
            return Match.bypass(rawSwr);
        return switch(state) {
            case TUNING, SEARCHING -> new Match(TunerMath.TUNING_SWR, SWRHelper.getEfficiencyMultiplier(TunerMath.TUNING_SWR));
            case TUNED -> rawSwr <= RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get()
                    ? new Match(1.0D, TunerMath.efficiency(rawSwr, 1.0D, RadiocraftServerConfig.ANTENNA_TUNER_LOSS.get()))
                    : Match.bypass(rawSwr);
            default -> Match.bypass(rawSwr);
        };
    }

    @Override
    public boolean isTuneFailed() {
        return powered && state == State.FAILED;
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putString("state", state.name());
        nbt.putFloat("tunedHz", tunedHz);
        if(tunedBand != null)
            nbt.putString("tunedBand", tunedBand);
        nbt.putLong("tunedAntenna", tunedAntenna);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        try {
            state = State.valueOf(nbt.getString("state"));
        }
        catch(IllegalArgumentException e) {
            state = State.IDLE;
        }
        if(state == State.TUNING || state == State.SEARCHING)
            state = State.IDLE; // Saved mid-search: start over.
        tunedHz = nbt.getFloat("tunedHz");
        tunedBand = nbt.contains("tunedBand") ? nbt.getString("tunedBand") : null;
        tunedAntenna = nbt.contains("tunedAntenna") ? nbt.getLong("tunedAntenna") : Long.MIN_VALUE;
    }

    /** A tuner placed beside a radio joins it without coax; coax from its other sides goes on to the antenna. */
    @Override
    public boolean linksDirectlyTo(BENetworkObject other) {
        return other instanceof RadioNetworkObject;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

}

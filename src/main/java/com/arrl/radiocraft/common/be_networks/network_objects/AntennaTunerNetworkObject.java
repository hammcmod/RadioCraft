package com.arrl.radiocraft.common.be_networks.network_objects;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxPassThrough;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.TunerMath;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The manual antenna tuner on a coax run. Its three dials (inductance and two capacitors, 0 to
 * {@link TunerMath#DIAL_MAX}) each have a reading that matches the antenna on the run at the radio's frequency
 * ({@link #targets}); the closer they are, the lower the SWR the radio sees ({@link TunerMath#manualSWR}). The wire walk
 * passes through it ({@link ICoaxPassThrough}), which keeps the radio and the antenna on one network.
 */
public class AntennaTunerNetworkObject extends BENetworkObject implements ICoaxPassThrough, ITuner {

    public static final ResourceLocation TYPE = Radiocraft.id("antenna_tuner");

    private volatile int[] dials = new int[3]; // Replaced whole on every change: read from the voice thread.

    public AntennaTunerNetworkObject(Level level, BlockPos pos) {
        super(level, pos);
    }

    public int getDial(int index) {
        return dials[index];
    }

    /** Turns one dial, kept between 0 and {@link TunerMath#DIAL_MAX}. */
    public void setDial(int index, int value) {
        if(index < 0 || index >= 3)
            return;
        int[] next = dials.clone();
        next[index] = Math.max(0, Math.min(TunerMath.DIAL_MAX, value));
        dials = next;
    }

    /**
     * The dial readings that match {@code antenna} at {@code frequencyHertz}: they depend on where the antenna is, what
     * kind it is, how far off resonance it is (in half SWR steps), the band, and drift a little across the band.
     */
    public static int[] targets(AntennaNetworkObject antenna, double rawSwr, float frequencyHertz) {
        StaticAntenna<?> a = antenna.getAntenna();
        Band band = Band.getBand(frequencyHertz);
        long seed = TunerMath.seed(antenna.getPos().asLong(), a == null ? 0L : a.type.getId().hashCode(),
                band == null ? 0L : band.name().hashCode(), Math.round(rawSwr * 2.0D));
        double fraction = band == null ? 0.5D : (frequencyHertz - band.minFrequency()) / (band.maxFrequency() - band.minFrequency());
        return TunerMath.targets(seed, fraction);
    }

    @Override
    public Match match(AntennaNetworkObject antenna, double rawSwr, float frequencyHertz) {
        double seen = TunerMath.manualSWR(rawSwr, RadiocraftServerConfig.ANTENNA_TUNER_MAX_SWR.get(), dials, targets(antenna, rawSwr, frequencyHertz));
        return new Match(seen, TunerMath.efficiency(rawSwr, seen, RadiocraftServerConfig.ANTENNA_TUNER_LOSS.get()));
    }

    @Override
    public void save(CompoundTag nbt) {
        super.save(nbt);
        nbt.putIntArray("dials", dials);
    }

    @Override
    public void load(IBENetworks cap, CompoundTag nbt) {
        super.load(cap, nbt);
        int[] saved = nbt.getIntArray("dials");
        if(saved.length == 3)
            dials = saved;
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

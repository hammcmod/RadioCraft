package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.NonDirectionalAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * A commercial 2m vertical, built like a J-pole (matched across 2m, poor elsewhere, the same reach) but with more gain,
 * and DC grounded: a lightning strike on it does damage only a quarter as often ({@link #LIGHTNING_DAMAGE_FACTOR}).
 * The Ringo has 10% more gain than a J-pole, the two-bay folded dipole 25% more.
 */
public class CommercialVHFAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    /** A grounded antenna's chance of damage from a strike, as a share of an ordinary antenna's. */
    public static final double LIGHTNING_DAMAGE_FACTOR = 0.25D;
    public static final double RINGO_GAIN = 1.10D;
    public static final double FOLDED_DIPOLE_GAIN = 1.25D;

    private final Supplier<? extends Block> block;

    /** @param gain linear gain over a J-pole, receiving and transmitting alike */
    public CommercialVHFAntennaType(String id, Supplier<? extends Block> block, double gain) {
        super(Radiocraft.id(id), toDbi(gain), toDbi(gain), 1.0D, 0.0D);
        this.block = block;
    }

    public static CommercialVHFAntennaType ringo() {
        return new CommercialVHFAntennaType("ringo", com.arrl.radiocraft.common.init.RadiocraftBlocks.RINGO_ANTENNA, RINGO_GAIN);
    }

    public static CommercialVHFAntennaType foldedDipole() {
        return new CommercialVHFAntennaType("folded_dipole", com.arrl.radiocraft.common.init.RadiocraftBlocks.FOLDED_DIPOLE_ANTENNA, FOLDED_DIPOLE_GAIN);
    }

    private static double toDbi(double linear) {
        return 10.0D * Math.log10(linear);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(block.get()) ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    protected double modifyDistanceForTransmit(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, double distance) {
        return distance / 1.3D; // As a J-pole.
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        Band band = Band.getBand(frequencyHertz);
        return band != null && band.isVhf() ? 1.0D : 10.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }

    @Override
    public double lightningDamageFactor() {
        return LIGHTNING_DAMAGE_FACTOR;
    }
}

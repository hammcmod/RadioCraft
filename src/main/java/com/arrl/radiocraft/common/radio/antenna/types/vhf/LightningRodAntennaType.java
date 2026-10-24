package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.NonDirectionalAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A vanilla lightning rod with coax run to it (maintainer, 2026-10-24): a very basic VHF antenna, SWR 6 and a quarter
 * of a J-pole's range. See {@link com.arrl.radiocraft.common.radio.antenna.LightningRodAntennas} for how the rod, which
 * has no block entity, gets onto the coax.
 */
public class LightningRodAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    /** The J-pole reaches 1.3 times as far as the band's range; the rod a quarter of that. */
    public static final double RANGE_SCALE = 1.3D / 4.0D;
    public static final double VHF_SWR = 6.0D;

    public LightningRodAntennaType() {
        super(Radiocraft.id("lightning_rod"), 0.0D, 0.0D, 1.0D, 0.0D);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.LIGHTNING_ROD) ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    protected double modifyDistanceForTransmit(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, double distance) {
        return distance / RANGE_SCALE;
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        Band band = Band.getBand(frequencyHertz);
        return band != null && band.isVhf() ? VHF_SWR : 10.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }
}

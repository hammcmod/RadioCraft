package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blocks.antennas.FiveBtvAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hustler 5-BTV (maintainer's model, 2026-10-23): a trap vertical seven blocks tall, matched on every HF band and
 * on VHF. On HF it puts its power out low, towards the horizon: line of sight reaches only {@link #HF_LOS_SCALE} as far,
 * and skip reaches {@link #HF_SKIP_SCALE} times as far. VHF is a plain vertical.
 */
public class FiveBtvAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    public static final double SWR = 1.3D;
    public static final double HF_LOS_SCALE = 0.5D;
    public static final double HF_SKIP_SCALE = 1.25D;

    public FiveBtvAntennaType() {
        super(Radiocraft.id("five_btv"), 0.0D, 0.0D, 1.0D, 0.8D);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(RadiocraftBlocks.FIVE_BTV_ANTENNA.get()) && state.getValue(FiveBtvAntennaBlock.SEGMENT) == 0
                ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    protected double getLosRangeScale(Band band) {
        return band != null && !band.isVhf() ? HF_LOS_SCALE : 1.0D;
    }

    @Override
    protected double getMaxSkipScale(Band band) {
        return band != null && !band.isVhf() ? HF_SKIP_SCALE : 1.0D;
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        return Band.getBand(frequencyHertz) != null ? SWR : 10.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }
}

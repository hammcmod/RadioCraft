package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.blocks.antennas.DoubleVHFAntennaBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The ionosonde's sounding antenna (maintainer's model, 2026-10-23). It is made for the ionospheric processor, which
 * reads the sky through it; on a radio it is a poor antenna. It hears VHF well, HF only weakly, and transmits badly
 * everywhere: the SWR is {@link #VHF_SWR} on VHF and {@link #HF_SWR} on HF, so most of a transmitter's power comes
 * back.
 */
public class IonosondeAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    public static final double VHF_SWR = 3.0D;
    public static final double HF_SWR = 8.0D;
    /** Share of a signal it hears on HF; VHF is heard in full. */
    public static final double HF_RECEIVE = 0.35D;

    public IonosondeAntennaType() {
        super(Radiocraft.id("ionosonde"), 0.0D, -3.0D, 1.0D, 0.3D);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(RadiocraftBlocks.IONOSONDE.get()) && state.getValue(DoubleVHFAntennaBlock.HALF) == DoubleBlockHalf.LOWER
                ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        double share = packet.getBand() != null && packet.getBand().isVhf() ? 1.0D : HF_RECEIVE;
        return getReceiveGainLinear() * packet.getStrength() * share;
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        Band band = Band.getBand(frequencyHertz);
        if(band == null)
            return 10.0D;
        return band.isVhf() ? VHF_SWR : HF_SWR;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }
}

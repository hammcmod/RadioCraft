package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.NonDirectionalAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SlimJimAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    public SlimJimAntennaType() {
        super(Radiocraft.id("slim_jim"), 1.0D, 1.0D, 1.0D, 0.0D);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(RadiocraftBlocks.SLIM_JIM_ANTENNA.get()) ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
        ServerLevel level = packet.getLevel();
        if(level.isThundering())
            return 0.0D;

        double distance = Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(destination)) / 1.2D;
        return BandUtils.getBaseStrength(packet.getWavelength(), isCW ? distance / 1.5D : distance, 1.0F, 0.0F, level.isDay());
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        return packet.getLevel().isThundering() ? 0.0D : 1.0D;
    }

    @Override
    public double getSWR(EmptyAntennaData data, int wavelength) {
        return wavelength == 2 ? 1.0D : 10.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }

}

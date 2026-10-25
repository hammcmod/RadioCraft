package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.weather.WeatherSources;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.NonDirectionalAntennaType;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SlimJimAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    public SlimJimAntennaType() {
        super(Radiocraft.id("slim_jim"), 0.0D, 0.0D, 1.0D, 0.0D);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(RadiocraftBlocks.SLIM_JIM_ANTENNA.get()) ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
        ServerLevel level = packet.getLevel();
        if(WeatherSources.isThundering(level, packet.getSource().getAntennaPos().position()))
            return 0.0D;

        return super.getTransmitEfficiency(packet, data, destination, isCW);
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        if(WeatherSources.isThundering(packet.getLevel(), pos))
            return 0.0D;

        return getReceiveGainLinear() * packet.getStrength();
    }

    @Override
    protected double modifyDistanceForTransmit(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, double distance) {
        return distance / 1.2D;
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        Band band = Band.getBand(frequencyHertz); // A fresh Band from the config each call: compare by band, not identity.
        return band != null && band.isVhf() ? 1.0D : 10.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }

}

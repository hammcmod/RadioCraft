package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.DirectionalAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.data.YagiAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class YagiAntennaType extends DirectionalAntennaType<YagiAntennaData> {

    public YagiAntennaType() {
        super(Radiocraft.id("yagi"), 0.0D, 0.0D, 1.0D, 0.0D);
    }

    @Override
    public StaticAntenna<YagiAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(RadiocraftBlocks.YAGI_ANTENNA.get()) ? new StaticAntenna<>(this, pos, level) : null;
    }

    @Override
    public double getDirectionalEfficiency(YagiAntennaData data, BlockPos from, BlockPos to) {
        BlockPos offset = to.subtract(from);
        Vec2 dir = new Vec2(offset.getX(), offset.getZ()).normalized();
        Vec3i normal = data.getFacing().getNormal();
        double f = -((new Vec2(normal.getX(), normal.getZ()).dot(dir) - 1.0D) / 2.0D); // 0 when same direction, 1 when opposite.

        if(f < 0.25D)
            return Mth.lerp(f / 0.25D, 1.0D, 0.1D); // 100% performance on facing side, scaled linearly to 45 degree threshold.
        else
            return f * 0.1D; // 10% performance when on sides or rear.
    }

    @Override
    protected double modifyDistanceForTransmit(IAntennaPacket packet, YagiAntennaData data, BlockPos destination, double distance) {
        return distance / 2.0D;
    }

    @Override
    public double getSWR(YagiAntennaData data, float frequencyHertz) {
        return Band.getBand(frequencyHertz) == Band.getBand("2m") ? 1.0D : 10.0D;
    }

    @Override
    public YagiAntennaData getDefaultData() {
        return new YagiAntennaData(Direction.NORTH);
    }

}

package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class WideBandReceiveAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

    protected WideBandReceiveAntennaType(double receiveGainDbi, double transmitGainDbi, double los, double skip) {
        super(Radiocraft.id("wide_band_receive"), receiveGainDbi, transmitGainDbi, los, skip);
    }

    @Override
    public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        if(!level.getBlockState(pos).is(RadiocraftBlocks.ANTENNA_POLE.get()))
            return null;
        if(!level.getBlockState(pos.above()).is(RadiocraftBlocks.ANTENNA_POLE.get()))
            return null; // Do not match if antenna does not have 2 poles.

        Axis first = validArm(level, pos.above(2));
        if(first == null)
            return null; // Do not match if there is no valid first arm.

        Axis second = validArm(level, pos.above(3));
        if(second == null || first == second)
            return null; // Do not match if the second arm is invalid or not perpendicular to first.

        return new StaticAntenna<>(this, pos, level);
    }

    public Axis validArm(Level level, BlockPos pos) {
        if(!level.getBlockState(pos).is(Blocks.IRON_BARS))
            return null; // Not valid if the center is not an iron bar.

        Axis axis = level.getBlockState(pos.offset(-1, 0, 0)).is(Blocks.IRON_BARS) ? Axis.X : Axis.Z;

        if(axis == Axis.X) {
            if(level.getBlockState(pos.offset(0, 0, -1)).is(Blocks.IRON_BARS) ||
                    level.getBlockState(pos.offset(0, 0, 1)).is(Blocks.IRON_BARS))
                return null; // Not valid if bars are attached on both axis.

            if(level.getBlockState(pos.offset(1, 0, 0)).is(Blocks.IRON_BARS))
                return Axis.X; // Only need to check X+ as X- was checked to find the axis.
        }
        else {
            if(level.getBlockState(pos.offset(1, 0, 0)).is(Blocks.IRON_BARS))
                return null; // Not valid if bars are attached on both axis. Only need to check X+ as X- was checked to find the axis.

            if(level.getBlockState(pos.offset(0, 0, -1)).is(Blocks.IRON_BARS) ||
                    level.getBlockState(pos.offset(0, 0, 1)).is(Blocks.IRON_BARS))
                return Axis.Z; // Valid if both Z axis blocks are bars.
        }

        return null;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
        return 0.0D;
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        double f = switch(packet.getBand().name()) {
            case "2m" -> 0.7D;
            case "10m" -> 0.5D;
            case "20m" -> 0.3D;
            case "40m", "80m" -> 0.2D;
            default -> 0.0D;
        };
        return getReceiveGainLinear() * packet.getStrength() * f;
    }

    @Override
    public double getSWR(EmptyAntennaData data, float frequencyHertz) {
        return 1.0;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }
}

package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.SatelliteDishAntennaData;
import com.arrl.radiocraft.common.radio.antenna.types.DirectionalAntennaType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

/**
 * Satellite Dish antenna type - directional VHF antenna with moderate gain.
 * Provides improved signal strength when pointed at target, decorative only for now.
 */
public class SatelliteDishAntennaType extends DirectionalAntennaType<SatelliteDishAntennaData> {

    public SatelliteDishAntennaType() {
        // Base parameters: id, vertical efficiency, omnidirectional efficiency, gain, noise
        super(Radiocraft.id("satellite_dish"), 0.0D, 0.0D, 1.5D, 0.0D);
    }

    @Override
    public StaticAntenna<SatelliteDishAntennaData> match(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(RadiocraftBlocks.SATELLITE_DISH.get()) 
            ? new StaticAntenna<>(this, pos, level) 
            : null;
    }

    @Override
    public double getDirectionalEfficiency(SatelliteDishAntennaData data, BlockPos from, BlockPos to) {
        BlockPos offset = to.subtract(from);
        Vec2 dir = new Vec2(offset.getX(), offset.getZ()).normalized();
        Vec3i normal = data.getFacing().getNormal();
        // Calculate dot product: -1 = opposite, 0 = perpendicular, 1 = same direction
        // Convert to efficiency: 0 when same direction (facing), 1 when opposite
        double f = -((new Vec2(normal.getX(), normal.getZ()).dot(dir) - 1.0D) / 2.0D);

        if (f < 0.20D) {
            // 100% performance when facing target, scaled linearly to ~40 degree threshold
            return Mth.lerp(f / 0.20D, 1.0D, 0.15D);
        } else {
            // 15% performance on sides or rear
            return f * 0.15D;
        }
    }

    @Override
    protected double modifyDistanceForTransmit(IAntennaPacket packet, SatelliteDishAntennaData data, BlockPos destination, double distance) {
        // Improved range: reduce effective distance by 35% (better than Yagi's 50%)
        return distance / 1.5D;
    }

    @Override
    public double getSWR(SatelliteDishAntennaData data, float frequencyHertz) {
        // Optimized for 2m VHF band (144-148 MHz)
        return Band.getBand(frequencyHertz) == Band.getBand("2m") ? 1.0D : 10.0D;
    }

    @Override
    public SatelliteDishAntennaData getDefaultData() {
        return new SatelliteDishAntennaData(Direction.NORTH);
    }
}

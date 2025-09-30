package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.data.RubberDuckyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class RubberDuckyAntennaType extends NonDirectionalAntennaType<RubberDuckyAntennaData> {

    public RubberDuckyAntennaType() {
        // We might need to workshop these numbers but rubber ducky antennas are pretty awful at much of anything.
        // Typical datasheets cite roughly -10 to -2 dBi; -3 dBi is a reasonable starting point until playtesting suggests otherwise.
        super(Radiocraft.id("rubber_ducky"), -3.01D, -3.01D, 1.0, 1.0D);
    }

    // This is a special type of antenna that does not have a physical in-world placement.
    @Override
    public StaticAntenna<RubberDuckyAntennaData> match(Level level, BlockPos pos) {
        return null;
    }

    @Override
    public double getSWR(RubberDuckyAntennaData data, float frequencyHertz) {
        return (int)Math.abs(Math.round(BandUtils.getWavelengthMetersFromFrequencyHertz(frequencyHertz) / 4.0D));
    }

    @Override
    public RubberDuckyAntennaData getDefaultData() {
        return new RubberDuckyAntennaData(0.1);
    }
}

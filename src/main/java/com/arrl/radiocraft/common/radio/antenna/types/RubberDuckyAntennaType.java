package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.init.RadiocraftData;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.RubberDuckyAntennaData;
import com.arrl.radiocraft.common.radio.antenna.data.S1pSmith;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicBoolean;

public class RubberDuckyAntennaType extends NonDirectionalAntennaType<RubberDuckyAntennaData> {

    private static final ResourceLocation VHF_PROFILE_ID = Radiocraft.id("rubber_ducky/vhf");
    private static final ResourceLocation UHF_PROFILE_ID = Radiocraft.id("rubber_ducky/uhf");
    private static final AtomicBoolean PROFILE_WARNING_EMITTED = new AtomicBoolean(false);

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
        double freq = frequencyHertz;
        S1pSmith vhfProfile = RadiocraftData.ANTENNA_PROFILES.getProfile(VHF_PROFILE_ID);
        S1pSmith uhfProfile = RadiocraftData.ANTENNA_PROFILES.getProfile(UHF_PROFILE_ID);

        S1pSmith profile = selectProfile(freq, vhfProfile, uhfProfile);
        if (profile != null) {
            if (freq < profile.getMinFreqHz() || freq > profile.getMaxFreqHz()) {
                return Double.POSITIVE_INFINITY;
            }
            double swr = profile.vswrAt(freq);
            return Double.isFinite(swr) ? swr : Double.POSITIVE_INFINITY;
        }

        if (vhfProfile == null || uhfProfile == null) {
            logMissingProfileOnce();
            return legacyApproximation(freq);
        }

        return Double.POSITIVE_INFINITY;
    }

    @Override
    public RubberDuckyAntennaData getDefaultData() {
        return new RubberDuckyAntennaData(0.155);
    }

    private static S1pSmith selectProfile(double frequencyHz, S1pSmith vhf, S1pSmith uhf) {
        if (vhf != null && frequencyHz >= vhf.getMinFreqHz() && frequencyHz <= vhf.getMaxFreqHz()) {
            return vhf;
        }
        if (uhf != null && frequencyHz >= uhf.getMinFreqHz() && frequencyHz <= uhf.getMaxFreqHz()) {
            return uhf;
        }
        return null;
    }

    private static void logMissingProfileOnce() {
        if (PROFILE_WARNING_EMITTED.compareAndSet(false, true)) {
            Radiocraft.LOGGER.warn("Rubber Ducky antenna profiles not fully loaded; falling back to legacy approximation.");
        }
    }

    private static double legacyApproximation(double frequencyHz) {
        return 2 * Math.pow(10, -13) * Math.pow(frequencyHz, 2)
                - 5.84 * Math.pow(10, -5) * frequencyHz
                + 4264.4;
    }
}

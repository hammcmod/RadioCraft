package com.arrl.radiocraft.common.radio.crystal;

import com.arrl.radiocraft.RadiocraftServerConfig;

/**
 * How good the quartz crystal a radio was built with is. Each crystal radio comes in a pure, good and poor version
 * (separate blocks and items). Good is what every radio is tuned for; pure is a little quieter, poor a little noisier
 * and it drifts off frequency ({@link CrystalDrift}). QRP radios are built around poor crystals and take none of the
 * penalty.
 */
public enum CrystalQuality {
    POOR("poor", "Poor"),
    GOOD("good", "Good"),
    PURE("pure", "Pure");

    private final String id;
    private final String label;

    CrystalQuality(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** Used in ids: {@code crystal_40m_poor}, {@code hf_radio_40m_pure}. */
    public String getId() {
        return id;
    }

    /** English name, for the language provider. */
    public String getLabel() {
        return label;
    }

    /**
     * Suffix for the id of a radio built with this crystal: none for good, so the good radios keep the ids they had
     * before crystals had qualities.
     */
    public String radioSuffix() {
        return this == GOOD ? "" : "_" + id;
    }

    /** English prefix for the name of a radio built with this crystal: "Premium 40m Radio", "40m Radio", "Cheap 40m Radio". */
    public String radioPrefix() {
        return switch(this) {
            case PURE -> "Premium ";
            case GOOD -> "";
            case POOR -> "Cheap ";
        };
    }

    /** {@code noise} (0 to 1) as a receiver built with this crystal hears it. */
    public double applyNoise(double noise) {
        return switch(this) {
            case PURE -> noise * RadiocraftServerConfig.CRYSTAL_PURE_NOISE_FACTOR.get();
            case GOOD -> noise;
            case POOR -> Math.min(1.0D, noise + RadiocraftServerConfig.CRYSTAL_POOR_EXTRA_NOISE.get());
        };
    }

    /** Hz the radio is off its dial frequency at {@code gameTime}; only poor crystals drift. */
    public double driftHz(long gameTime, long seed) {
        return this == POOR ? CrystalDrift.offsetHz(gameTime, seed, RadiocraftServerConfig.CRYSTAL_POOR_DRIFT_HZ.get()) : 0.0D;
    }
}

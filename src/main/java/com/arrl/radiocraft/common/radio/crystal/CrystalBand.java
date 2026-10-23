package com.arrl.radiocraft.common.radio.crystal;

/**
 * The bands a quartz crystal can be cut for. Each radio needs the crystal for its band; VHF radios (handhelds, base
 * stations, repeaters) share the VHF crystal.
 */
public enum CrystalBand {
    BAND_80M("80m", "3.5 MHz (80m)"),
    BAND_40M("40m", "7 MHz (40m)"),
    BAND_20M("20m", "14 MHz (20m)"),
    BAND_10M("10m", "28 MHz (10m)"),
    VHF("vhf", "144 MHz (VHF)");

    private final String id;
    private final String label;

    CrystalBand(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** Used in item ids: {@code crystal_40m_poor}, {@code encased_crystal_40m_poor}. */
    public String getId() {
        return id;
    }

    /** English name of the cut, for the language provider. */
    public String getLabel() {
        return label;
    }
}

package com.arrl.radiocraft.compat;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum AntennaInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final double SPEED_OF_LIGHT_METERS_PER_SECOND = 299_792_458.0D;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if(!data.contains("antennaMatched"))
            return;

        if(!data.getBoolean("antennaMatched")) {
            tooltip.add(Component.literal("Antenna Match: None"));
            return;
        }

        tooltip.add(Component.literal("Antenna Type: " + data.getString("antennaType")));

        int dimensionCount = data.getInt("antennaDimensionCount");
        for(int i = 0; i < dimensionCount; i++)
            tooltip.add(Component.literal(data.getString("antennaDimension_" + i)));

        if(data.contains("antennaResonanceMhz"))
            tooltip.add(Component.literal(String.format("Approx Resonance: %.3f MHz", data.getDouble("antennaResonanceMhz"))));

        int swrCount = data.getInt("antennaSwrCount");
        for(int i = 0; i < swrCount; i++)
            tooltip.add(Component.literal(data.getString("antennaSwr_" + i)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        StaticAntenna<?> antenna = AntennaTypes.match(accessor.getLevel(), accessor.getPosition());
        data.putBoolean("antennaMatched", antenna != null);
        if(antenna == null)
            return;

        data.putString("antennaType", antenna.getType().getId().getPath());
        appendDimensions(data, antenna.getData());
        appendResonance(data, antenna.getData());
        appendSwr(data, antenna);
    }

    private static void appendDimensions(CompoundTag data, AntennaData antennaData) {
        int index = 0;
        if(antennaData instanceof EndFedAntennaData endFed) {
            data.putString("antennaDimension_" + index++, String.format("Length: %.2f m", endFed.getLength()));
        }
        else if(antennaData instanceof QuarterWaveVerticalAntennaData vertical) {
            data.putString("antennaDimension_" + index++, "Height: " + vertical.getHeight() + " m");
        }
        else if(antennaData instanceof DipoleAntennaData dipole) {
            data.putString("antennaDimension_" + index++, String.format("Arm 1: %.2f m", dipole.getArmLength1()));
            data.putString("antennaDimension_" + index++, String.format("Arm 2: %.2f m", dipole.getArmLength2()));
            data.putString("antennaDimension_" + index++, String.format("Total: %.2f m", dipole.getArmLength1() + dipole.getArmLength2()));
        }
        else if(antennaData instanceof HorizontalQuadLoopAntennaData loop) {
            data.putString("antennaDimension_" + index++, "Side: " + loop.getSideLength() + " m");
            data.putString("antennaDimension_" + index++, "Perimeter: " + loop.getSideLength() * 4 + " m");
        }
        else if(antennaData instanceof VerticalQuadLoopAntennaData loop) {
            data.putString("antennaDimension_" + index++, "Side: " + loop.getSideLength() + " m");
            data.putString("antennaDimension_" + index++, "Perimeter: " + loop.getSideLength() * 4 + " m");
            data.putString("antennaDimension_" + index++, "Plane: " + (loop.getXAxis() ? "X" : "Z") + " axis");
        }
        data.putInt("antennaDimensionCount", index);
    }

    private static void appendResonance(CompoundTag data, AntennaData antennaData) {
        double wavelengthMeters = 0.0D;
        if(antennaData instanceof EndFedAntennaData endFed)
            wavelengthMeters = endFed.getLength() * 4.0D;
        else if(antennaData instanceof QuarterWaveVerticalAntennaData vertical)
            wavelengthMeters = vertical.getHeight() * 4.0D;
        else if(antennaData instanceof DipoleAntennaData dipole)
            wavelengthMeters = ((dipole.getArmLength1() + dipole.getArmLength2()) / 2.0D) * 4.0D;
        else if(antennaData instanceof HorizontalQuadLoopAntennaData loop)
            wavelengthMeters = loop.getSideLength() * 4.0D;
        else if(antennaData instanceof VerticalQuadLoopAntennaData loop)
            wavelengthMeters = loop.getSideLength() * 4.0D;

        if(wavelengthMeters > 0.0D)
            data.putDouble("antennaResonanceMhz", SPEED_OF_LIGHT_METERS_PER_SECOND / wavelengthMeters / 1_000_000.0D);
    }

    private static void appendSwr(CompoundTag data, StaticAntenna<?> antenna) {
        List<Band> bands = Band.getDefaults().stream()
                .filter(band -> !band.name().equals("2m"))
                .toList();

        int index = 0;
        for(Band band : bands) {
            float centerFrequency = (band.minFrequency() + band.maxFrequency()) / 2.0F;
            data.putString("antennaSwr_" + index++, String.format("SWR %s: %.1f", band.name(), antenna.getSWR(centerFrequency)));
        }
        data.putInt("antennaSwrCount", index);
    }

    @Override
    public ResourceLocation getUid() {
        return Radiocraft.id("antenna_info");
    }
}

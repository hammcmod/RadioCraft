package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class JPoleAntennaType implements IAntennaType<EmptyAntennaData> {

    private final ResourceLocation id = Radiocraft.location("j_pole");

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public BEAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return null;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos destination, boolean isCW) {
        double distance = Math.sqrt(packet.getSource().getPos().distSqr(destination)) / 1.3D;
        return BandUtils.getBaseStrength(packet.getWavelength(), isCW ? distance / 1.5D : distance, 1.0F, 0.0F, packet.getLevel().isDay());
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        return 1.0D;
    }

    @Override
    public double getSWR(EmptyAntennaData data, int wavelength) {
        return 1.0D;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }

}

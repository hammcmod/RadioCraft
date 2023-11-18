package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public abstract class NonDirectionalAntennaType<T extends AntennaData> implements IAntennaType<T> {

    private final ResourceLocation id;
    private final double los;
    private final double skip;
    private final double receive;
    private final double transmit;

    protected NonDirectionalAntennaType(ResourceLocation id, double receive, double transmit, double los, double skip) {
        this.id = id;
        this.los = los;
        this.skip = skip;
        this.receive = receive;
        this.transmit = transmit;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, T data, BlockPos destination, boolean isCW) {
        double distance = Math.sqrt(packet.getSource().getPos().distSqr(destination));
        return transmit * BandUtils.getBaseStrength(packet.getWavelength(), isCW ? distance / 1.5D : distance, los, skip, packet.getLevel().isDay());
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, T data, BlockPos pos) {
        return receive * packet.getStrength();
    }

}

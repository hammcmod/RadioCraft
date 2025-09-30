package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public abstract class NonDirectionalAntennaType<T extends AntennaData> implements IAntennaType<T> {

    private static final double CW_RANGE_MULTIPLIER = 1.5D;

    private final ResourceLocation id;
    private final double los;
    private final double skip;
    private final double receiveGainDbi;
    private final double transmitGainDbi;
    private final double receiveGainLinear;
    private final double transmitGainLinear;

    protected NonDirectionalAntennaType(ResourceLocation id, double receiveGainDbi, double transmitGainDbi, double los, double skip) {
        this.id = id;
        this.los = los;
        this.skip = skip;
        this.receiveGainDbi = receiveGainDbi;
        this.transmitGainDbi = transmitGainDbi;
        this.receiveGainLinear = dbiToLinear(receiveGainDbi);
        this.transmitGainLinear = dbiToLinear(transmitGainDbi);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, T data, BlockPos destination, boolean isCW) {
        double distance = computeDistance(packet, destination);
        double adjustedDistance = modifyDistanceForTransmit(packet, data, destination, distance);
        double propagation = getPropagationStrength(packet, adjustedDistance, isCW);
        return propagation * getTransmitGainLinear();
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, T data, BlockPos pos) {
        return getReceiveGainLinear() * packet.getStrength();
    }

    protected double computeDistance(IAntennaPacket packet, BlockPos destination) {
        return Math.sqrt(packet.getSource().getAntennaPos().position().distSqr(destination));
    }

    protected double modifyDistanceForTransmit(IAntennaPacket packet, T data, BlockPos destination, double distance) {
        return distance;
    }

    protected double getPropagationStrength(IAntennaPacket packet, double distance, boolean isCW) {
        double effectiveDistance = isCW ? distance / CW_RANGE_MULTIPLIER : distance;
        return BandUtils.getBaseStrength(packet.getBand(), effectiveDistance, getLosEfficiency(), getSkipEfficiency(), packet.getLevel().isDay());
    }

    protected double getLosEfficiency() {
        return los;
    }

    protected double getSkipEfficiency() {
        return skip;
    }

    public double getReceiveGainDbi() {
        return receiveGainDbi;
    }

    public double getTransmitGainDbi() {
        return transmitGainDbi;
    }

    public double getReceiveGainLinear() {
        return receiveGainLinear;
    }

    public double getTransmitGainLinear() {
        return transmitGainLinear;
    }

    private static double dbiToLinear(double dbi) {
        return Math.pow(10.0D, dbi / 10.0D);
    }

}

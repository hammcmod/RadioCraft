package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.common.radio.solar.Aurora;
import com.arrl.radiocraft.common.radio.solar.SolarEventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

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
        double propagation = getPropagationStrength(packet, destination, adjustedDistance, isCW);
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

    protected double getPropagationStrength(IAntennaPacket packet, BlockPos destination, double distance, boolean isCW) {
        double effectiveDistance = isCW ? distance / CW_RANGE_MULTIPLIER : distance;
        Level level = packet.getLevel();
        BlockPos source = packet.getSource().getAntennaPos().position();
        // Solar weather only touches the sky wave: a poor or closed band weakens skip, line of sight is unchanged. An
        // aurora over either end costs HF a condition step, and lets VHF skip between two stations under it.
        double skip = getSkipEfficiency() * SolarEventManager.getCondition(level, packet.getBand(), source, destination).getSkipMultiplier();
        double strength = BandUtils.getBaseStrength(packet.getBand(), effectiveDistance, getLosEfficiency(), skip, level.isDay(),
                getLosRangeScale(packet.getBand()), getMaxSkipScale(packet.getBand()));
        return Math.max(strength, Aurora.vhfStrength(level, packet.getBand(), source, destination, effectiveDistance));
    }

    /** How far line of sight reaches on {@code band} with this antenna, as a share of the band's range. */
    protected double getLosRangeScale(Band band) {
        return 1.0D;
    }

    /** How far skip reaches on {@code band} with this antenna, as a share of the band's furthest skip. */
    protected double getMaxSkipScale(Band band) {
        return 1.0D;
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

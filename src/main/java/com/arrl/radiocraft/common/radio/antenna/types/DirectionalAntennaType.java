package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.common.radio.antenna.data.AntennaData;
import com.arrl.radiocraft.compat.sable.Sublevels;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public abstract class DirectionalAntennaType<T extends AntennaData> extends NonDirectionalAntennaType<T> {

    protected DirectionalAntennaType(ResourceLocation id, double receiveGainDbi, double transmitGainDbi, double los, double skip) {
        super(id, receiveGainDbi, transmitGainDbi, los, skip);
    }

    @Override
    public double getTransmitEfficiency(IAntennaPacket packet, T data, BlockPos destination, boolean isCW) {
        double base = super.getTransmitEfficiency(packet, data, destination, isCW);
        BlockPos from = packet.getSource().getAntennaPos().position();
        // The far end as seen from this antenna's own block grid, so the pattern turns with a Sable ship it is on.
        return base * getDirectionalEfficiency(data, from, Sublevels.inFrameOf(packet.getLevel(), from, destination));
    }

    @Override
    public double getReceiveEfficiency(IAntennaPacket packet, T data, BlockPos pos) {
        double base = super.getReceiveEfficiency(packet, data, pos);
        BlockPos from = Sublevels.inFrameOf(packet.getLevel(), pos, packet.getSource().getAntennaPos().position());
        return base * getDirectionalEfficiency(data, from, pos);
    }

    /**
     * Get the efficiency of a packet being sent from or arriving to pos, from/to otherpos.
     *
     * @param data Data object belonging to the sending/receiving antenna.
     * @param from The position of the sending antenna.
     * @param to The position of the receiving antenna.
     */
    public abstract double getDirectionalEfficiency(T data, BlockPos from, BlockPos to);

}

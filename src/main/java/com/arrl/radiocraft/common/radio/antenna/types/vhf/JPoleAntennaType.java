package com.arrl.radiocraft.common.radio.antenna.types.vhf;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.antenna.IAntennaPacket;
import com.arrl.radiocraft.api.antenna.IAntennaType;
import com.arrl.radiocraft.common.radio.BandUtils;
import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import com.arrl.radiocraft.common.radio.antenna.BEAntenna;
import com.arrl.radiocraft.common.radio.antenna.types.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class JPoleAntennaType implements IAntennaType<EmptyAntennaData> {

    public static final ResourceLocation ID = Radiocraft.location("j_pole");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public BEAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
        return null;
    }

    @Override
    public double getSSBTransmitStrength(AntennaVoicePacket packet, EmptyAntennaData data, BlockPos destination) {
        double distance = Math.sqrt(packet.getSource().getPos().distSqr(destination));
        ServerLevel level = (ServerLevel)packet.getLevel().getServerLevel();
        return BandUtils.getSSBBaseStrength(packet.getWavelength(), distance / 1.3D, 1.0F, 0.0F, level.isDay());
    }

    @Override
    public double getReceiveStrength(IAntennaPacket packet, EmptyAntennaData data, BlockPos pos) {
        return 0;
    }

    @Override
    public EmptyAntennaData getDefaultData() {
        return new EmptyAntennaData();
    }

}

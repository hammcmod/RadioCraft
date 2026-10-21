package com.arrl.radiocraft.common.network;

import com.arrl.radiocraft.common.network.serverbound.SHandheldRadioUpdatePacket;
import com.arrl.radiocraft.common.network.clientbound.CAuroraPacket;
import com.arrl.radiocraft.common.network.clientbound.CCWBufferPacket;
import com.arrl.radiocraft.common.network.clientbound.CInterferencePacket;
import com.arrl.radiocraft.common.network.clientbound.CNoisePacket;
import com.arrl.radiocraft.common.network.serverbound.SCWBufferPacket;
import com.arrl.radiocraft.common.network.clientbound.CWireEndPosUpdatePacket;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import com.arrl.radiocraft.common.network.serverbound.SPlayerClickHoldUpdate;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RadiocraftNetworking {

    //For consistency, this should probably be it's own class in init, but since there doesn't seem to be an available differedRegistry for this anyways, I'll forgo the clutter
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SHandheldRadioUpdatePacket.TYPE, SHandheldRadioUpdatePacket.STREAM_CODEC, SHandheldRadioUpdatePacket::handle);
        registrar.playToServer(SBlockRadioControlPacket.TYPE, SBlockRadioControlPacket.STREAM_CODEC, SBlockRadioControlPacket::handle);
        registrar.playToClient(CWireEndPosUpdatePacket.TYPE, CWireEndPosUpdatePacket.STREAM_CODEC, CWireEndPosUpdatePacket::handle);
        registrar.playToClient(CNoisePacket.TYPE, CNoisePacket.STREAM_CODEC, CNoisePacket::handle);
        registrar.playToServer(SPlayerClickHoldUpdate.TYPE, SPlayerClickHoldUpdate.STREAM_CODEC, SPlayerClickHoldUpdate::handle);
        registrar.playToServer(SCWBufferPacket.TYPE, SCWBufferPacket.STREAM_CODEC, SCWBufferPacket::handle);
        registrar.playToClient(CCWBufferPacket.TYPE, CCWBufferPacket.STREAM_CODEC, CCWBufferPacket::handle);
        registrar.playToClient(CAuroraPacket.TYPE, CAuroraPacket.STREAM_CODEC, CAuroraPacket::handle);
        registrar.playToClient(CInterferencePacket.TYPE, CInterferencePacket.STREAM_CODEC, CInterferencePacket::handle);
    }
}

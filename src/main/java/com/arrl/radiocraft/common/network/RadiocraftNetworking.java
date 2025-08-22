package com.arrl.radiocraft.common.network;

import com.arrl.radiocraft.common.network.serverbound.SHandheldRadioUpdatePacket;
import com.arrl.radiocraft.common.network.clientbound.CWireEndPosUpdatePacket;
import com.arrl.radiocraft.common.network.serverbound.SPlayerClickHoldUpdate;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RadiocraftNetworking {

    //For consistency, this should probably be it's own class in init, but since there doesn't seem to be an available differedRegistry for this anyways, I'll forgo the clutter
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SHandheldRadioUpdatePacket.TYPE, SHandheldRadioUpdatePacket.STREAM_CODEC, SHandheldRadioUpdatePacket::handle);
        registrar.playToClient(CWireEndPosUpdatePacket.TYPE, CWireEndPosUpdatePacket.STREAM_CODEC, CWireEndPosUpdatePacket::handle);
        registrar.playToServer(SPlayerClickHoldUpdate.TYPE, SPlayerClickHoldUpdate.STREAM_CODEC, SPlayerClickHoldUpdate::handle);
    }
}

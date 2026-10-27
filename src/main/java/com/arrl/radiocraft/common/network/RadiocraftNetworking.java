package com.arrl.radiocraft.common.network;

import com.arrl.radiocraft.common.network.serverbound.SHandheldRadioUpdatePacket;
import com.arrl.radiocraft.common.network.clientbound.CAuroraPacket;
import com.arrl.radiocraft.common.network.clientbound.CCWBufferPacket;
import com.arrl.radiocraft.common.network.clientbound.CInterferencePacket;
import com.arrl.radiocraft.common.network.clientbound.CNoisePacket;
import com.arrl.radiocraft.common.network.serverbound.SCWBufferPacket;
import com.arrl.radiocraft.common.network.clientbound.CWireEndPosUpdatePacket;
import com.arrl.radiocraft.common.network.serverbound.SBlockRadioControlPacket;
import com.arrl.radiocraft.common.network.serverbound.SStationPacket;
import com.arrl.radiocraft.common.network.serverbound.SPlayerClickHoldUpdate;
import com.arrl.radiocraft.common.network.serverbound.SWornPttPacket;
import com.arrl.radiocraft.common.network.serverbound.SAntennaTunerDialPacket;
import com.arrl.radiocraft.common.network.serverbound.SDuplexerScrewPacket;
import com.arrl.radiocraft.common.network.serverbound.SChargeControllerTogglePacket;
import com.arrl.radiocraft.common.network.serverbound.SRepeaterSettingsPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RadiocraftNetworking {

    //For consistency, this should probably be it's own class in init, but since there doesn't seem to be an available differedRegistry for this anyways, I'll forgo the clutter
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SHandheldRadioUpdatePacket.TYPE, SHandheldRadioUpdatePacket.STREAM_CODEC, SHandheldRadioUpdatePacket::handle);
        registrar.playToServer(SBlockRadioControlPacket.TYPE, SBlockRadioControlPacket.STREAM_CODEC, SBlockRadioControlPacket::handle);
        registrar.playToClient(CWireEndPosUpdatePacket.TYPE, CWireEndPosUpdatePacket.STREAM_CODEC, CWireEndPosUpdatePacket::handle);
        registrar.playToClient(CNoisePacket.TYPE, CNoisePacket.STREAM_CODEC, CNoisePacket::handle);
        registrar.playToServer(SPlayerClickHoldUpdate.TYPE, SPlayerClickHoldUpdate.STREAM_CODEC, SPlayerClickHoldUpdate::handle);
        registrar.playToServer(SWornPttPacket.TYPE, SWornPttPacket.STREAM_CODEC, SWornPttPacket::handle);
        registrar.playToServer(SCWBufferPacket.TYPE, SCWBufferPacket.STREAM_CODEC, SCWBufferPacket::handle);
        registrar.playToServer(com.arrl.radiocraft.common.network.serverbound.SClaimCallsignPacket.TYPE, com.arrl.radiocraft.common.network.serverbound.SClaimCallsignPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.serverbound.SClaimCallsignPacket::handle);
        registrar.playToClient(com.arrl.radiocraft.common.network.clientbound.CCallsignPromptPacket.TYPE, com.arrl.radiocraft.common.network.clientbound.CCallsignPromptPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.clientbound.CCallsignPromptPacket::handle);
        registrar.playToClient(CCWBufferPacket.TYPE, CCWBufferPacket.STREAM_CODEC, CCWBufferPacket::handle);
        registrar.playToClient(CAuroraPacket.TYPE, CAuroraPacket.STREAM_CODEC, CAuroraPacket::handle);
        registrar.playToClient(CInterferencePacket.TYPE, CInterferencePacket.STREAM_CODEC, CInterferencePacket::handle);
        registrar.playToServer(SRepeaterSettingsPacket.TYPE, SRepeaterSettingsPacket.STREAM_CODEC, SRepeaterSettingsPacket::handle);
        registrar.playToServer(SStationPacket.TYPE, SStationPacket.STREAM_CODEC, SStationPacket::handle);
        registrar.playToServer(com.arrl.radiocraft.common.network.serverbound.SManpackPacket.TYPE, com.arrl.radiocraft.common.network.serverbound.SManpackPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.serverbound.SManpackPacket::handle);
        registrar.playToServer(com.arrl.radiocraft.common.network.serverbound.SManpackCWPacket.TYPE, com.arrl.radiocraft.common.network.serverbound.SManpackCWPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.serverbound.SManpackCWPacket::handle);
        registrar.playToClient(com.arrl.radiocraft.common.network.clientbound.CManpackPacket.TYPE, com.arrl.radiocraft.common.network.clientbound.CManpackPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.clientbound.CManpackPacket::handle);
        registrar.playToClient(com.arrl.radiocraft.common.network.clientbound.CManpackCWPacket.TYPE, com.arrl.radiocraft.common.network.clientbound.CManpackCWPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.clientbound.CManpackCWPacket::handle);
        registrar.playToServer(com.arrl.radiocraft.common.network.serverbound.SHandheldDfFrequencyPacket.TYPE, com.arrl.radiocraft.common.network.serverbound.SHandheldDfFrequencyPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.serverbound.SHandheldDfFrequencyPacket::handle);
        registrar.playToClient(com.arrl.radiocraft.common.network.clientbound.CHandheldDfPacket.TYPE, com.arrl.radiocraft.common.network.clientbound.CHandheldDfPacket.STREAM_CODEC, com.arrl.radiocraft.common.network.clientbound.CHandheldDfPacket::handle);
        registrar.playToServer(SAntennaTunerDialPacket.TYPE, SAntennaTunerDialPacket.STREAM_CODEC, SAntennaTunerDialPacket::handle);
        registrar.playToServer(SDuplexerScrewPacket.TYPE, SDuplexerScrewPacket.STREAM_CODEC, SDuplexerScrewPacket::handle);
        registrar.playToServer(SChargeControllerTogglePacket.TYPE, SChargeControllerTogglePacket.STREAM_CODEC, SChargeControllerTogglePacket::handle);
    }
}

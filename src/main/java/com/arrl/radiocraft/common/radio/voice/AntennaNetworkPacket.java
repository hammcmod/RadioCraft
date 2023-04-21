package com.arrl.radiocraft.common.radio.voice;

import de.maxhenkel.voicechat.api.packets.MicrophonePacket;

public record AntennaNetworkPacket(MicrophonePacket packet, float strength) {
}

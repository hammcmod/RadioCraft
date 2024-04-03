package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.common.radio.antenna.AntennaVoicePacket;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

/**
 * {@link IVoiceReceiver} is used for interacting with the Simple Voice Chat API to send sound packets being received by a Radio.
 * PCM audio gets re-encoded here and sent on an {@link AudioChannel}.
 */
public interface IVoiceReceiver {

    boolean isReceiving();

    void setReceiving(boolean value);

    void receive(AntennaVoicePacket antennaPacket);

}

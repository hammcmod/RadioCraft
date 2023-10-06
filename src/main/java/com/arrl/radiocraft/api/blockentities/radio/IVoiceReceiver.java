package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.common.radio.Radio;

/**
 * Anything which receives voice transmissions should implement {@link IVoiceReceiver}.
 */
public interface IVoiceReceiver {

	/**
	 * @return Returns {@link Radio} instance for this radio, which acts as a container for Simple Voice Chat API
	 * interactions.
	 */
	Radio getRadio();

}

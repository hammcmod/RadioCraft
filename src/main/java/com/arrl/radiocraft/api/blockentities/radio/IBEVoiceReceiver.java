package com.arrl.radiocraft.api.blockentities.radio;

import com.arrl.radiocraft.common.radio.BEVoiceReceiver;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Any {@link BlockEntity} which listens for voice transmissions should implement {@link IBEVoiceReceiver}.
 */
public interface IBEVoiceReceiver {

	/**
     * Get the {@link BEVoiceReceiver} instance for this radio.
	 * @return Returns {@link BEVoiceReceiver} instance for this radio, which acts as a container for Simple Voice Chat API
	 * interactions.
	 */
	BEVoiceReceiver getVoiceReceiver();

}

package com.arrl.radiocraft.client;

public class RadiocraftClientValues {

	public static float NOISE_VOLUME = 0.0F;

	public static boolean SCREEN_PTT_PRESSED = false; // These 2 values are for the voice chat PTT mixin
	public static boolean SCREEN_VOICE_ENABLED = false;

	public static boolean SCREEN_CW_ENABLED = false; // Used for recording morse input buffers

	public static VoxStateEnum RADIO_VOX_MODE = VoxStateEnum.INACTIVE; // Used for showing if VOX is running on the radio

}

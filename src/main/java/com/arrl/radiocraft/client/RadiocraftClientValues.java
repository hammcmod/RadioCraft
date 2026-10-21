package com.arrl.radiocraft.client;

public class RadiocraftClientValues {

	public static float NOISE_VOLUME = 0.0F;

	public static boolean AURORA_ACTIVE = false; // From CAuroraPacket: drawn in the sky and louder HF static past the latitude.
	public static int AURORA_LATITUDE = 10000;
	public static float AURORA_NOISE = 0.0F;

	public static boolean SCREEN_PTT_PRESSED = false; // These 2 values are for the voice chat PTT mixin
	public static boolean SCREEN_VOICE_ENABLED = false;

	public static boolean SCREEN_CW_ENABLED = false; // Used for recording morse input buffers

}

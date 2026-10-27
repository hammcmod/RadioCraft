package com.arrl.radiocraft.client;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.items.HandheldDirectionFinderItem;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The handheld direction finder's beeper, on the client: the server says which way the station is
 * ({@link com.arrl.radiocraft.common.network.clientbound.CHandheldDfPacket}), and every tick the beeps are timed from
 * where the player is looking: one a second facing away, ten a second facing it, with the lamp blinking on each.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public final class HandheldDfClient {

	private HandheldDfClient() {}

	/** Longest and shortest gap between beeps, ticks. */
	private static final int SLOWEST = 20, FASTEST = 2;
	/** Ticks the lamp stays lit after a beep. */
	private static final int LIT_TICKS = 2;

	private static boolean hearing = false;
	private static float bearing = 0.0F;
	private static float strength = 0.0F;
	private static long heardAt = Long.MIN_VALUE;
	private static int sinceBeep = 0;
	private static int litTicks = 0;

	public static void update(boolean hearing, float bearing, float strength) {
		HandheldDfClient.hearing = hearing;
		HandheldDfClient.bearing = bearing;
		HandheldDfClient.strength = strength;
		Minecraft mc = Minecraft.getInstance();
		if(mc.level != null)
			heardAt = mc.level.getGameTime();
	}

	/** True while the lamp is lit (for the item model's {@code radiocraft:lit}). */
	public static boolean isLit() {
		return litTicks > 0;
	}

	/** How squarely the player faces the station: 1 straight at it, 0 straight away. */
	public static double alignment(float playerYaw, float bearing) {
		double facing = DirectionFinding.normalize(playerYaw + 180.0D); // Minecraft's yaw 0 looks south; compass 0 is north.
		return 1.0D - DirectionFinding.difference(facing, bearing) / 180.0D;
	}

	/** Ticks between beeps for an alignment: sharper toward the station, so the last few degrees count. */
	public static int beepGap(double alignment) {
		double a = Math.max(0.0D, Math.min(1.0D, alignment));
		return (int)Math.round(SLOWEST - (SLOWEST - FASTEST) * a * a * a);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(litTicks > 0)
			litTicks--;
		if(player == null || mc.level == null || mc.isPaused())
			return;
		boolean holding = player.getMainHandItem().getItem() instanceof HandheldDirectionFinderItem || player.getOffhandItem().getItem() instanceof HandheldDirectionFinderItem;
		boolean fresh = hearing && mc.level.getGameTime() - heardAt <= 10L;
		if(!holding || !fresh) {
			sinceBeep = SLOWEST;
			return;
		}
		double alignment = alignment(player.getYRot(), bearing);
		if(++sinceBeep >= beepGap(alignment)) {
			sinceBeep = 0;
			litTicks = LIT_TICKS;
			float volume = 0.25F + 0.5F * strength;
			mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS,
					volume, 1.2F + 0.6F * (float)alignment, false);
		}
	}
}

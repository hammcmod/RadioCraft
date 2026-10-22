package com.arrl.radiocraft.client;

import com.arrl.radiocraft.Radiocraft;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Under an aurora in the cold, things pick up static and let it go with faint pops and crackles (people in northern
 * Canada hear it from air ducts). Now and then, a burst of a few very quiet pops a few blocks away: only heard when
 * everything else is quiet. Client only, from what the client already knows ({@link RadiocraftClientValues#AURORA_ACTIVE},
 * the aurora latitude) and whether the player's biome is cold enough to snow. {@code /auroracrackle [pops]} plays a burst
 * on demand, to judge the volume.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID, value = Dist.CLIENT)
public class AuroraCrackle {

	/** Chance per tick that a burst starts: about one every 8 seconds. */
	private static final float BURST_CHANCE = 1.0F / 160.0F;
	private static final float MIN_VOLUME = 0.04F;
	private static final float MAX_VOLUME = 0.09F;

	private static int popsLeft = 0;
	private static int ticksToNextPop = 0;
	private static boolean forced = false; // A burst asked for by /auroracrackle: plays whatever the sky and biome.

	/** {@code /auroracrackle [pops]}: one burst now, as the aurora would play it, to hear how loud it is. */
	@SubscribeEvent
	public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("auroracrackle")
				.executes(context -> burst(context.getSource(), 1 + RandomSource.create().nextInt(4)))
				.then(Commands.argument("pops", IntegerArgumentType.integer(1, 20))
						.executes(context -> burst(context.getSource(), IntegerArgumentType.getInteger(context, "pops")))));
	}

	private static int burst(CommandSourceStack source, int pops) {
		popsLeft = pops;
		ticksToNextPop = 0;
		forced = true;
		source.sendSuccess(() -> Component.literal("Aurora crackle: " + pops + " pop(s) at in-game volume"), false);
		return pops;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;
		if(level == null || player == null || minecraft.isPaused() || (!forced && !underAuroraInTheCold(level, player.blockPosition()))) {
			popsLeft = 0;
			forced = false;
			return;
		}
		RandomSource random = level.getRandom();
		if(popsLeft == 0) {
			forced = false;
			if(random.nextFloat() >= BURST_CHANCE)
				return;
			popsLeft = 1 + random.nextInt(4);
			ticksToNextPop = 0;
		}
		if(ticksToNextPop-- > 0)
			return;
		pop(minecraft, player, random);
		popsLeft--;
		ticksToNextPop = 1 + random.nextInt(6); // Pops in a burst come close together, unevenly.
	}

	/** True while an aurora is on, the player is past its latitude, and their biome is cold enough to snow. */
	private static boolean underAuroraInTheCold(ClientLevel level, BlockPos pos) {
		return RadiocraftClientValues.AURORA_ACTIVE && Math.abs((long)pos.getZ()) >= RadiocraftClientValues.AURORA_LATITUDE
				&& level.getBiome(pos).value().coldEnoughToSnow(pos);
	}

	/** One faint pop, from somewhere a few blocks off: a sharp, quiet campfire crackle at a random pitch. */
	private static void pop(Minecraft minecraft, LocalPlayer player, RandomSource random) {
		double x = player.getX() + (random.nextDouble() - 0.5D) * 8.0D;
		double y = player.getY() + random.nextDouble() * 3.0D;
		double z = player.getZ() + (random.nextDouble() - 0.5D) * 8.0D;
		float volume = MIN_VOLUME + random.nextFloat() * (MAX_VOLUME - MIN_VOLUME);
		float pitch = 1.3F + random.nextFloat() * 0.7F;
		minecraft.getSoundManager().play(new SimpleSoundInstance(SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT, volume, pitch, random, x, y, z));
	}
}

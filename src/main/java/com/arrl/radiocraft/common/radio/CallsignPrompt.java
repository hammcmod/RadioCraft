package com.arrl.radiocraft.common.radio;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import com.arrl.radiocraft.common.network.clientbound.CCallsignPromptPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The first time a player without a callsign reaches for a radio (a radio block or a handheld), they are asked to
 * choose one instead (proposal). Once a session: "Later" lets them get on with it, and they are asked again next time
 * they join. Only while {@code callsign_self_assign} is on.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class CallsignPrompt {

	private static final Set<UUID> ASKED = ConcurrentHashMap.newKeySet();

	private CallsignPrompt() {}

	/** True (and the prompt sent) if this player should choose a callsign before using the radio. */
	public static boolean prompt(ServerPlayer player) {
		if(!RadiocraftServerConfig.CALLSIGN_SELF_ASSIGN.get() || player.getServer() == null || !ASKED.add(player.getUUID()))
			return false;
		if(Callsigns.playerCallsign(player.getServer(), player.getUUID()) != null)
			return false;
		CCallsignPromptPacket.send(player, CCallsignPromptPacket.OPEN, Callsigns.allowedDigits());
		return true;
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if(event.getEntity() instanceof ServerPlayer player && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof RadioBlock && prompt(player)) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if(event.getEntity() instanceof ServerPlayer player && event.getItemStack().getItem() instanceof VHFHandheldItem && prompt(player)) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}

	@SubscribeEvent
	public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		ASKED.remove(event.getEntity().getUUID());
	}

	/** The prompt's Claim button. */
	public static void claimFromPrompt(ServerPlayer player, String callsign) {
		Callsigns.ClaimResult result = Callsigns.claim(player, callsign);
		if(result == Callsigns.ClaimResult.OK) {
			CCallsignPromptPacket.send(player, CCallsignPromptPacket.DONE, "");
			player.sendSystemMessage(Component.translatable("message.radiocraft.callsign.claimed", Callsigns.normalise(callsign)).withStyle(ChatFormatting.GREEN));
		}
		else
			CCallsignPromptPacket.send(player, CCallsignPromptPacket.ERROR, "message.radiocraft.callsign." + result.name().toLowerCase(java.util.Locale.ROOT));
	}
}

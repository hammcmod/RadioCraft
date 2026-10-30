package com.arrl.radiocraft.common.radio.rfi;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.blocks.radios.RadioBlock;
import com.arrl.radiocraft.common.items.HFManpackItem;
import com.arrl.radiocraft.common.items.VHFHandheldItem;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import com.arrl.radiocraft.compat.curios.CuriosCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightning striking a player (maintainer, 2026-10-27): every VHF handheld and HF manpack they carry (inventory, armour
 * and Curios slots) is fried and falls apart into salvage, as a blown radio block does. A manpack switched on at the
 * time makes it worse: {@code lightning_manpack_extra_damage} on top of the bolt.
 */
@EventBusSubscriber(modid = Radiocraft.MOD_ID)
public final class CarriedRadioStrikes {

	/** What a lightning bolt does to an entity in vanilla. */
	private static final float BOLT_DAMAGE = 5.0F;

	private CarriedRadioStrikes() {}

	@SubscribeEvent
	public static void onStruck(EntityStruckByLightningEvent event) {
		if(event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && !event.isCanceled())
			strike(level, player);
	}

	/** {@code player} was struck: breaks what they carry. Returns how many radios broke. */
	public static int strike(ServerLevel level, Player player) {
		if(!RadiocraftServerConfig.LIGHTNING_BREAKS_CARRIED_RADIOS.get() || player.isCreative() || player.isSpectator())
			return 0;
		List<ItemStack> carried = new ArrayList<>();
		for(int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
			carried.add(player.getInventory().getItem(slot)); // Main, armour (the manpack on the chest) and off hand.
		carried.addAll(CuriosCompat.wornManpacks(player));
		carried.addAll(CuriosCompat.wornHandhelds(player));

		int broken = 0;
		boolean manpackOn = false;
		for(ItemStack stack : carried) {
			if(stack.isEmpty() || !(stack.getItem() instanceof VHFHandheldItem || stack.getItem() instanceof HFManpackItem))
				continue;
			if(stack.getItem() instanceof HFManpackItem && ManpackRadio.stateOf(stack).on())
				manpackOn = true;
			for(ItemStack part : RadioBlock.salvage(level, stack.getItem()))
				player.drop(part, true);
			stack.shrink(1);
			broken++;
		}
		if(broken == 0)
			return 0;
		level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 0.8F);
		player.displayClientMessage(Component.translatable("message.radiocraft.lightning_fried_radio"), true);
		// The event comes before the bolt's own 5 damage, which the hurt cooldown then swallows: so hit for both now.
		if(manpackOn && RadiocraftServerConfig.LIGHTNING_MANPACK_EXTRA_DAMAGE.get() > 0.0D)
			player.hurt(level.damageSources().lightningBolt(), BOLT_DAMAGE + RadiocraftServerConfig.LIGHTNING_MANPACK_EXTRA_DAMAGE.get().floatValue());
		return broken;
	}
}

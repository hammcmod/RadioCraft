package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The HF manpack's handset, taken off its hook on the manpack's panel. Hold right click to talk. Sneak right click
 * hangs it back up. It only exists while the manpack is to hand: dropped, or without its manpack, it is gone.
 */
public class ManpackMicItem extends Item {

	public ManpackMicItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static ItemStack create() {
		return new ItemStack(RadiocraftItems.MANPACK_MIC.get());
	}

	/** True if the player has the handset anywhere in their inventory. */
	public static boolean carries(Player player) {
		Inventory inventory = player.getInventory();
		for(int i = 0; i < inventory.getContainerSize(); i++)
			if(inventory.getItem(i).getItem() instanceof ManpackMicItem)
				return true;
		return false;
	}

	public static void removeFrom(Player player) {
		Inventory inventory = player.getInventory();
		for(int i = 0; i < inventory.getContainerSize(); i++)
			if(inventory.getItem(i).getItem() instanceof ManpackMicItem)
				inventory.setItem(i, ItemStack.EMPTY);
	}

	/** Sneak right click: back on its hook. Right click alone talks (the client reports the held key). */
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(player.isShiftKeyDown()) {
			if(!level.isClientSide)
				com.arrl.radiocraft.common.network.serverbound.SManpackPacket.hangMic(player);
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		return InteractionResultHolder.pass(stack);
	}

	/** Without its manpack to hand, the handset goes back on it. */
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
		if(!level.isClientSide && entity instanceof Player player && (ManpackRadio.find(player).isEmpty() || !ManpackRadio.stateOf(ManpackRadio.find(player)).micOut()))
			stack.setCount(0);
	}

	@Override
	public boolean onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity) {
		if(!entity.level().isClientSide)
			entity.discard(); // It springs back on its cord.
		return true;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.radiocraft.manpack_mic").withStyle(ChatFormatting.GRAY));
	}
}

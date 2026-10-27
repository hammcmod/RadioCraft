package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.datacomponents.ManpackState;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.radio.voice.handheld.ManpackRadio;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * The HF manpack: a QRP SSB/CW radio for 10m and 20m in a PRC-25 style backpack (see {@link ManpackRadio}). Worn on the
 * chest slot (or the Curios back slot), or held. Right click in the hand opens its panel; sneak right click puts it
 * on. Its battery charges from a small battery in the other hand (sneak right click with the battery there swaps
 * their charge), and anything that charges Forge Energy items can top it up.
 */
public class HFManpackItem extends Item implements Equipable {

	public HFManpackItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public @NotNull EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.CHEST;
	}

	@Override
	public @NotNull Holder<SoundEvent> getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_LEATHER;
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(player.isShiftKeyDown()) {
			ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if(other.getItem() == RadiocraftItems.SMALL_BATTERY.get()) { // Swap charge with the battery in the other hand.
				if(!level.isClientSide)
					swapCharge(stack, other);
				return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
			}
			return swapWithEquipmentSlot(this, level, player, hand); // On the back.
		}
		if(level.isClientSide)
			com.arrl.radiocraft.client.screens.ManpackScreen.open(); // A separate class, so the server never loads it.
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	/** Swaps the manpack's battery charge with a small battery's, as the VHF handheld does. */
	private static void swapCharge(ItemStack manpack, ItemStack battery) {
		IEnergyStorage storage = battery.getCapability(Capabilities.EnergyStorage.ITEM);
		if(storage == null)
			return;
		ManpackState state = ManpackRadio.stateOf(manpack);
		int capacity = ManpackState.CAPACITY;
		int theirs = storage.extractEnergy(storage.getEnergyStored(), false); // What the battery held...
		storage.receiveEnergy(state.energy(), false); // ...it gets ours...
		manpack.set(RadiocraftDataComponent.MANPACK_STATE.get(), state.withEnergy(Math.min(capacity, theirs))); // ...and we get its.
	}

	@Override
	public boolean isBarVisible(@NotNull ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		return Math.round(13.0F * ManpackRadio.stateOf(stack).energy() / Math.max(1, ManpackState.CAPACITY));
	}

	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		return 0x4CAF50;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		ManpackState state = ManpackRadio.stateOf(stack);
		tooltip.add(Component.literal(String.format(Locale.ROOT, "%s  %.3f MHz  %s  %s", state.bandName(), state.frequency() / 1.0E6D,
				state.cw() ? "CW" : "SSB", state.on() ? "ON" : "OFF")).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.radiocraft.hf_manpack").withStyle(ChatFormatting.GRAY));
	}
}

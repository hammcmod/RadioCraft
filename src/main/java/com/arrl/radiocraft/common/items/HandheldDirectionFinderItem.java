package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import com.arrl.radiocraft.common.network.clientbound.CHandheldDfPacket;
import com.arrl.radiocraft.common.radio.Band;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.arrl.radiocraft.common.radio.TransmitterLog;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * A handheld loop direction finder for fox hunts. Right click sets the frequency. While it is in a hand and a
 * station is transmitting on that frequency within line of sight range, it beeps and its lamp blinks, faster the
 * more squarely the player faces the station. It plays none of the station's audio. The server tells the client
 * which way the station is ({@link CHandheldDfPacket}); the client times the beeps from where the player looks.
 */
public class HandheldDirectionFinderItem extends Item {

	/** 146.520 MHz, the 2m simplex calling frequency: where a fox usually starts. */
	public static final float DEFAULT_FREQUENCY = 146_520_000.0F;
	/** How often (ticks) the server looks for the station and tells the client. */
	public static final int UPDATE_TICKS = 4;

	public HandheldDirectionFinderItem(Properties properties) {
		super(properties);
	}

	public static float frequencyOf(ItemStack stack) {
		Float frequency = stack.get(RadiocraftDataComponent.DF_FREQUENCY.get());
		return frequency == null ? DEFAULT_FREQUENCY : frequency;
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide)
			com.arrl.radiocraft.client.screens.HandheldDirectionFinderScreen.open(hand, frequencyOf(stack)); // A separate class, so the server never loads it.
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
		if(level.isClientSide || !(entity instanceof ServerPlayer player) || level.getGameTime() % UPDATE_TICKS != 0L)
			return;
		boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
		if(!inHand || (player.getOffhandItem() == stack && player.getMainHandItem().getItem() instanceof HandheldDirectionFinderItem))
			return; // One report per player: from the main hand when both hands hold one.
		float frequency = frequencyOf(stack);
		Band band = Band.getBand(frequency);
		double range = band == null ? 0.0D : band.losRange(); // A small loop: line of sight only, no skip.
		double tolerance = band != null && band.isVhf() ? RadiocraftServerConfig.VHF_FREQUENCY_STEP.get() / 2.0D : 1500.0D;
		TransmitterLog.Entry heard = range <= 0.0D ? null : TransmitterLog.nearest(level, player.blockPosition(), frequency, tolerance, range, 10L);
		if(heard == null)
			CHandheldDfPacket.send(player, false, 0.0F, 0.0F);
		else {
			double bearing = DirectionFinding.bearing(player.getX(), player.getZ(), heard.pos().getX() + 0.5D, heard.pos().getZ() + 0.5D);
			double distance = Math.sqrt(heard.pos().distToCenterSqr(player.position()));
			CHandheldDfPacket.send(player, true, (float)bearing, (float)Math.max(0.05D, 1.0D - distance / range));
		}
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.literal(String.format(Locale.ROOT, "%.3f MHz", frequencyOf(stack) / 1.0E6D)).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.radiocraft.handheld_direction_finder").withStyle(ChatFormatting.GRAY));
	}
}

package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.datacomponents.TeletypeRollContents;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A roll of paper torn off a teletype (sneak and right click the machine with an empty hand), with everything it had
 * printed. Right click to read it; right click a teletype whose roll is empty to feed it back in.
 */
public class TeletypeRollItem extends Item {

	public TeletypeRollItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static TeletypeRollContents contents(ItemStack stack) {
		return stack.getOrDefault(RadiocraftDataComponent.TELETYPE_ROLL_CONTENTS.get(), TeletypeRollContents.EMPTY);
	}

	/** A roll with these lines on it. */
	public static ItemStack of(Item item, List<String> lines, int fedLines) {
		ItemStack stack = new ItemStack(item);
		stack.set(RadiocraftDataComponent.TELETYPE_ROLL_CONTENTS.get(), new TeletypeRollContents(List.copyOf(lines), fedLines));
		return stack;
	}

	/** On a teletype with nothing on its roll: feed this one back in. */
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if(!(level.getBlockEntity(context.getClickedPos()) instanceof TeletypeBlockEntity teletype) || !teletype.getRttyLines().isEmpty())
			return InteractionResult.PASS;
		if(!level.isClientSide) {
			teletype.loadRoll(contents(context.getItemInHand()));
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Read it. */
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(level.isClientSide)
			com.arrl.radiocraft.client.screens.TeletypeRollScreen.open(stack);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		TeletypeRollContents contents = contents(stack);
		tooltip.add(Component.translatable("item.radiocraft.teletype_roll.lines", contents.lines().size()).withStyle(ChatFormatting.GRAY));
		if(!contents.isEmpty())
			tooltip.add(Component.literal(contents.lines().getFirst()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}

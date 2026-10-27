package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.TelephoneBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftDataComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A telephone's handset on its cord, picked up by sneak-right-clicking the phone with an empty hand. While it is up
 * the phone hears only its holder and plays the far end at their ear ({@link TelephoneBlockEntity}); the cord snaps
 * back if they walk more than {@link TelephoneBlockEntity#CORD_LENGTH} blocks away or stop holding it. It only exists
 * while out of its cradle, as the radio's hand mic ({@link CordedMicrophoneItem}): dropped, it vanishes. Shares the
 * mic's {@link MicCord} component (the phone's position and which pick-up this is).
 */
public class TelephoneHandsetItem extends Item {

	public TelephoneHandsetItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Nullable
	public static MicCord cord(ItemStack stack) {
		return stack.get(RadiocraftDataComponent.MIC_CORD.get());
	}

	/** True if {@code stack} is the handset {@code phone} has out right now. */
	public static boolean isHandsetOf(ItemStack stack, TelephoneBlockEntity phone) {
		MicCord cord = stack.getItem() instanceof TelephoneHandsetItem ? cord(stack) : null;
		return cord != null && cord.radio().equals(phone.getBlockPos()) && cord.micId().equals(phone.getHandsetId());
	}

	/** Server side: a handset its phone no longer has out with this player is gone. */
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
		if(level.isClientSide)
			return;
		MicCord cord = cord(stack);
		boolean valid = cord != null && entity instanceof Player player && level.getBlockEntity(cord.radio()) instanceof TelephoneBlockEntity phone
				&& isHandsetOf(stack, phone) && player.getUUID().equals(phone.getHandsetHolder());
		if(!valid)
			stack.setCount(0);
	}

	/** Dropped, it springs back to its phone: the item entity just goes. */
	@Override
	public boolean onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity) {
		if(!entity.level().isClientSide)
			entity.discard();
		return true;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		tooltip.add(Component.translatable(Radiocraft.translationKey("tooltip", "telephone_handset")).withStyle(ChatFormatting.GRAY));
	}
}

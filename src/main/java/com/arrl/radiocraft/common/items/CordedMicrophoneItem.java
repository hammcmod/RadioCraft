package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
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
 * A radio's hand microphone on its cord, taken by sneak-right-clicking the radio with an empty hand. Holding right
 * click talks through the radio ({@link RadioBlockEntity#tickHandMic}, which also snaps the cord when the player walks
 * more than {@link RadioBlockEntity#MIC_CORD_LENGTH} blocks away). Right clicking the radio hangs it back up. It only
 * exists while lent out: dropped, it vanishes, and one whose radio no longer lends it to this player is removed.
 */
public class CordedMicrophoneItem extends Item {

    public CordedMicrophoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public static MicCord cord(ItemStack stack) {
        return stack.get(RadiocraftDataComponent.MIC_CORD.get());
    }

    /** True if {@code stack} is the mic {@code radio} currently has lent out. */
    public static boolean isMicOf(ItemStack stack, RadioBlockEntity radio) {
        MicCord cord = stack.getItem() instanceof CordedMicrophoneItem ? cord(stack) : null;
        return cord != null && cord.radio().equals(radio.getBlockPos()) && cord.micId().equals(radio.getMicId());
    }

    /** Server side: a mic its radio no longer lends to this player is gone. */
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if(level.isClientSide)
            return;
        MicCord cord = cord(stack);
        boolean valid = cord != null && entity instanceof Player player && level.getBlockEntity(cord.radio()) instanceof RadioBlockEntity radio
                && isMicOf(stack, radio) && player.getUUID().equals(radio.getMicHolder());
        if(!valid)
            stack.setCount(0);
    }

    /** Dropped, it springs back to its radio: the item entity just goes. */
    @Override
    public boolean onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity) {
        if(!entity.level().isClientSide)
            entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(Radiocraft.translationKey("tooltip", "corded_microphone")).withStyle(ChatFormatting.GRAY));
    }
}

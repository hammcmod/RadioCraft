package com.arrl.radiocraft.common.menus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MenuUtils {

	/**
	 * Helper method to grab a {@link BlockEntity} for use in screens.
	 */
	@SuppressWarnings("unchecked")
	public static <C extends BlockEntity> C getBlockEntity(Inventory playerInventory, FriendlyByteBuf data, Class<C> clazz) {
        if (data == null) {
            throw new NullPointerException("data cannot be null. This wasn't gonna work. " + clazz.getName());
        }
        net.minecraft.core.BlockPos pos = data.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);

        if(be != null && !clazz.isInstance(be)) // A subclass is fine: the commercial repeater opens the repeater's menu.
            throw new IllegalStateException("BlockEntity at " + pos + " is not the correct type");

        return (C)be; // IntelliJ hates this, but it SHOULD be safe given the class check above.
    }
}

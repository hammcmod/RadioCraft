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
		BlockEntity be = playerInventory.player.level.getBlockEntity(data.readBlockPos());

		if(be != null && be.getClass() != clazz)
			throw new IllegalStateException("BlockEntity at " + data.readBlockPos() + " is not the correct type");

		return (C)be; // IntelliJ hates this, but it SHOULD be safe given the class check above.
	}

}

package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio80mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio80mMenu extends RadioMenu<HFRadio80mBlockEntity> {

	public HFRadio80mMenu(int id, HFRadio80mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_80M.get(), id, blockEntity, blockEntity.getBlockState().getBlock()); // Any crystal quality of this radio.
	}

	public HFRadio80mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) { // Clientside constructor doesn't need a RadioNetworkObject
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio80mBlockEntity.class));
	}

}
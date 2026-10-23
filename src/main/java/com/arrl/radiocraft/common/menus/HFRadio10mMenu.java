package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio10mMenu extends RadioMenu<HFRadio10mBlockEntity> {

	public HFRadio10mMenu(int id, HFRadio10mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id, blockEntity, blockEntity.getBlockState().getBlock()); // Any crystal quality of this radio.
	}

	public HFRadio10mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) { // Clientside constructor doesn't need a RadioNetworkObject
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio10mBlockEntity.class));
	}

}
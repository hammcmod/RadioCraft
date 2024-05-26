package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio20mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio20mMenu extends RadioMenu<HFRadio20mBlockEntity> {

	public HFRadio20mMenu(int id, HFRadio20mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_20M.get(), id, blockEntity, RadiocraftBlocks.HF_RADIO_20M.get());
	}

	public HFRadio20mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) { // Clientside constructor doesn't need a RadioNetworkObject
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio20mBlockEntity.class));
	}

}
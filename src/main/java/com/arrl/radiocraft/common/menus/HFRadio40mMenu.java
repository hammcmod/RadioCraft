package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio40mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio40mMenu extends RadioMenu<HFRadio40mBlockEntity> {

	public HFRadio40mMenu(int id, HFRadio40mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_40M.get(), id, blockEntity, blockEntity.getBlockState().getBlock()); // Any crystal quality of this radio.
	}

	public HFRadio40mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) { // Clientside constructor doesn't need a RadioNetworkObject
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio40mBlockEntity.class));
	}

}
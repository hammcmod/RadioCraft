package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio10mMenu extends RadioMenu<HFRadio10mBlockEntity> {

	public HFRadio10mMenu(int id, HFRadio10mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id, blockEntity, RadiocraftBlocks.HF_RADIO_10M.get());
	}

	public HFRadio10mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio10mBlockEntity.class));
	}

}
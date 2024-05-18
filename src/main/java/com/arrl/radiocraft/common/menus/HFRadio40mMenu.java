package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio40mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio40mMenu extends RadioMenu<HFRadio40mBlockEntity> {

	public HFRadio40mMenu(int id, HFRadio40mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_40M.get(), id, blockEntity, RadiocraftBlocks.HF_RADIO_40M.get());
	}

	public HFRadio40mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio40mBlockEntity.class));
	}

}
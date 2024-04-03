package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio80mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadio80mMenu extends RadioMenu<HFRadio80mBlockEntity> {

	public HFRadio80mMenu(int id, HFRadio80mBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RADIO_80M.get(), id, blockEntity, RadiocraftBlocks.HF_RADIO_80M.get());
	}

	public HFRadio80mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadio80mBlockEntity.class));
	}

}
package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFRadioMenu10m extends RadioMenu<HFRadio10mBlockEntity> {

	public HFRadioMenu10m(int id, HFRadio10mBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id, blockEntity, data, RadiocraftBlocks.HF_RADIO_10M.get());
	}

	public HFRadioMenu10m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, null, new SimpleContainerData(2));
		blockEntity = getBlockEntity(playerInventory, data, HFRadio10mBlockEntity.class);
	}

}
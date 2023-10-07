package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio20mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFRadioMenu20m extends RadioMenu<HFRadio20mBlockEntity> {

	public HFRadioMenu20m(int id, HFRadio20mBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_20M.get(), id, blockEntity, data, RadiocraftBlocks.HF_RADIO_20M.get());
	}

	public HFRadioMenu20m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, null, new SimpleContainerData(2));
		blockEntity = getBlockEntity(playerInventory, data, HFRadio20mBlockEntity.class);
	}

}
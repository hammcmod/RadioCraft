package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFRadio40mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFRadioMenu40m extends RadioMenu<HFRadio40mBlockEntity> {

	public HFRadioMenu40m(int id, HFRadio40mBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_40M.get(), id, blockEntity, data, RadiocraftBlocks.HF_RADIO_40M.get());
	}

	public HFRadioMenu40m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, null, new SimpleContainerData(2));
		blockEntity = getBlockEntity(playerInventory, data, HFRadio40mBlockEntity.class);
	}

}
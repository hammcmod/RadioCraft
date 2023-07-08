package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFRadioMenu10m extends AbstractHFRadioMenu {

	public HFRadioMenu10m(int id, AbstractRadioBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id, blockEntity, data);
	}

	public HFRadioMenu10m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, AbstractHFRadioMenu.getBlockEntity(playerInventory, data), new SimpleContainerData(2));
	}

}
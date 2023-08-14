package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFRadioMenu80m extends AbstractHFRadioMenu {

	public HFRadioMenu80m(int id, AbstractRadioBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_80M.get(), id, blockEntity, data, RadiocraftBlocks.HF_RADIO_80M.get());
	}

	public HFRadioMenu80m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, AbstractHFRadioMenu.getBlockEntity(playerInventory, data), new SimpleContainerData(3));
	}

}
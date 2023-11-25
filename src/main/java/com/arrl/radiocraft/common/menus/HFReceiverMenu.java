package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.HFReceiverBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class HFReceiverMenu extends RadioMenu<HFReceiverBlockEntity> {

	public HFReceiverMenu(int id, HFReceiverBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RECEIVER.get(), id, blockEntity, data, RadiocraftBlocks.HF_RECEIVER.get());
	}

	public HFReceiverMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFReceiverBlockEntity.class), new SimpleContainerData(2));
	}

}
package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.VHFReceiverBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class VHFReceiverMenu extends RadioMenu<VHFReceiverBlockEntity> {

	public VHFReceiverMenu(int id, VHFReceiverBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.VHF_RECEIVER.get(), id, blockEntity, RadiocraftBlocks.VHF_RECEIVER.get());
	}

	public VHFReceiverMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, VHFReceiverBlockEntity.class));
	}

}
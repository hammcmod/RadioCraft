package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFReceiverBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFReceiverMenu extends RadioMenu<HFReceiverBlockEntity> {

	public HFReceiverMenu(int id, HFReceiverBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.HF_RECEIVER.get(), id, blockEntity, RadiocraftBlocks.HF_RECEIVER.get());
	}

	public HFReceiverMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, HFReceiverBlockEntity.class));
	}

}
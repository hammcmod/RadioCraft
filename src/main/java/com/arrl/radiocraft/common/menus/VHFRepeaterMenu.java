package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.VHFRepeaterBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class VHFRepeaterMenu extends RadioMenu<VHFRepeaterBlockEntity> {

	public VHFRepeaterMenu(int id, VHFRepeaterBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.VHF_REPEATER.get(), id, blockEntity, RadiocraftBlocks.VHF_REPEATER.get());
	}

	public VHFRepeaterMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, VHFRepeaterBlockEntity.class));
	}

	/** Server-side readouts, see {@link VHFRepeaterBlockEntity#getDataSlots}. */
	public int getData(int index) {
		return data.get(index);
	}

}

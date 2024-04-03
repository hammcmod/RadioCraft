package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.VHFBaseStationBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class VHFBaseStationMenu extends RadioMenu<VHFBaseStationBlockEntity> {

	public VHFBaseStationMenu(int id, VHFBaseStationBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.VHF_BASE_STATION.get(), id, blockEntity, RadiocraftBlocks.VHF_BASE_STATION.get());
	}

	public VHFBaseStationMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, VHFBaseStationBlockEntity.class));
	}

}
package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadio20mMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadio20mBlockEntity extends HFRadioBlockEntity {

	public HFRadio20mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_20M.get(), pos, state, RadiocraftCommonConfig.HF_RADIO_20M_RECEIVE_TICK.get(), RadiocraftCommonConfig.HF_RADIO_20M_TRANSMIT_TICK.get(), 20);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.hf_radio_20m");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new HFRadio20mMenu(id, this, fields);
	}
}

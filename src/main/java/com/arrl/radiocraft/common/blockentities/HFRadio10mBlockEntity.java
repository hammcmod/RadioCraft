package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadio10mMenu;
import com.arrl.radiocraft.common.radio.Radio;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadio10mBlockEntity extends AbstractRadioBlockEntity {

	public HFRadio10mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_10M.get(), pos, state, RadiocraftConfig.HF_RADIO_10M_RECEIVE_TICK.get(), RadiocraftConfig.HF_RADIO_10M_TRANSMIT_TICK.get());
	}

	@Override
	public Radio createRadio() {
		return new Radio(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.hf_radio_10m");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new HFRadio10mMenu(id, this, fields);
	}
}

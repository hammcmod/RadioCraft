package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadioMenu40m;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadio40mBlockEntity extends AbstractRadioBlockEntity {

	public HFRadio40mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_40M.get(), pos, state, RadiocraftConfig.HF_RADIO_40M_RECEIVE_TICK.get(), RadiocraftConfig.HF_RADIO_40M_TRANSMIT_TICK.get(), 40);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.hf_radio_40m");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new HFRadioMenu40m(id, this, fields);
	}
}

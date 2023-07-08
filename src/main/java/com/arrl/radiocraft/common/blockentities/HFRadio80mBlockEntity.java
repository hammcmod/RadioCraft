package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadioMenu80m;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadio80mBlockEntity extends AbstractRadioBlockEntity {

	public HFRadio80mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_40M.get(), pos, state, RadiocraftConfig.HF_RADIO_80M_RECEIVE_TICK.get(), RadiocraftConfig.HF_RADIO_80M_TRANSMIT_TICK.get(), 80);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.hf_radio_80m");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new HFRadioMenu80m(id, this, fields);
	}
}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.Radio;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HFRadio10mBlockEntity extends AbstractRadioBlockEntity {

	public HFRadio10mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_10M.get(), pos, state, RadiocraftConfig.HF_RADIO_10M_RECEIVE_TICK.get(), RadiocraftConfig.HF_RADIO_10M_TRANSMIT_TICK.get());
	}

	@Override
	public Radio createRadio() {
		return new Radio();
	}

}

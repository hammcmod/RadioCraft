package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.RadioData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HFRadio10mBlockEntity extends AbstractRadioBlockEntity {

	public HFRadio10mBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.HF_RADIO_10M.get(), pos, state, 0, 0); // Not going to consume power in the test version
	}

	@Override
	public RadioData createRadioData() {
		return new RadioData();
	}

}

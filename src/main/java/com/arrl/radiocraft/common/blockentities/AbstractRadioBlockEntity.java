package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.radio.RadioData;
import com.arrl.radiocraft.common.radio.RadioManager;
import com.arrl.radiocraft.common.radio.RadioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractRadioBlockEntity extends AbstractPowerBlockEntity {

	private RadioData radioData;

	public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
		super(type, pos, state, capacity, maxTransfer);
	}

	public RadioData getRadioData() {
		if(radioData == null)
			radioData = createRadioData();

		return radioData;
	}

	public RadioNetwork getRadioNetwork() {
		return level != null ? RadioManager.getNetwork(level) : null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		getRadioNetwork().putRadio(worldPosition, getRadioData());
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		getRadioNetwork().removeRadio(worldPosition);
	}

	public abstract RadioData createRadioData();

}

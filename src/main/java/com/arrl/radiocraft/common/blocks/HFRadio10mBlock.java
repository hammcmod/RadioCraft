package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.HFRadio10mBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadio10mBlock extends AbstractPowerNetworkBlock {

	public HFRadio10mBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HFRadio10mBlockEntity(pos, state);
	}

}

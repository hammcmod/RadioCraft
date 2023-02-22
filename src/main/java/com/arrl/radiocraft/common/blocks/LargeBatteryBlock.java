package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LargeBatteryBlock extends AbstractPowerNetworkBlock {

	public LargeBatteryBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LargeBatteryBlockEntity(pos, state);
	}

	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}



}

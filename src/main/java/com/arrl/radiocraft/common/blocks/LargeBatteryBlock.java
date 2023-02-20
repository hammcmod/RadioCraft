package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.power.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LargeBatteryBlock extends BaseEntityBlock {

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

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if(!pLevel.isClientSide)
			for(PowerNetwork network : ((LargeBatteryBlockEntity)pLevel.getBlockEntity(pPos)).getNetworks().values()) // For debug
				Radiocraft.LOGGER.info(network.getConnections().toString());

		return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
	}



}

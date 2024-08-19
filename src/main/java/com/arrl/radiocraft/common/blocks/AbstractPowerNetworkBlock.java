package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractPowerNetworkBlock extends AbstractNetworkBlock {

	protected AbstractPowerNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
		if(!pLevel.isClientSide) {
				BlockEntity be = pLevel.getBlockEntity(pPos);
				pPlayer.openMenu((MenuProvider) be);
				return InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(pState, pLevel, pPos, pPlayer, pHitResult);
	}
}

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
import net.minecraftforge.network.NetworkHooks;

public abstract class AbstractPowerNetworkBlock extends AbstractNetworkBlock {

	protected AbstractPowerNetworkBlock(Properties properties, boolean useCoaxNetworks) {
		super(properties, true, useCoaxNetworks);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide) {
			if(hand == InteractionHand.MAIN_HAND) {
				BlockEntity be = level.getBlockEntity(pos);
				NetworkHooks.openScreen((ServerPlayer)player, (MenuProvider)be, pos);
				return InteractionResult.SUCCESS;
			}
		}

		return super.use(state, level, pos, player, hand, hit);
	}

}

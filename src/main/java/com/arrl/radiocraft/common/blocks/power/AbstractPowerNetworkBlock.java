package com.arrl.radiocraft.common.blocks.power;

import com.arrl.radiocraft.common.blocks.AbstractNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractPowerNetworkBlock extends AbstractNetworkBlock {

	protected AbstractPowerNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull BlockHitResult pHitResult) {
		if (!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(Objects.requireNonNull(pState.getMenuProvider(pLevel, pPos)), pPos);
		}
		return InteractionResult.sidedSuccess(pLevel.isClientSide);
	}
}

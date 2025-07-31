package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.energy.IEnergyStorage;
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

	@Override
	public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		level.invalidateCapabilities(pos);
	}
}

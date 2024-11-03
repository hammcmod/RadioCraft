package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.network_objects.AntennaNetworkObject;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.networks.AntennaNetworkManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Use for any blocks which contain an AntennaBlockEntity (for example baluns)
 */
public class AntennaCenterBlock extends AbstractNetworkBlock {

	public AntennaCenterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull BlockHitResult pHitResult) {
		if(!pLevel.isClientSide) {
			if(IBENetworks.getObject(pLevel, pPos) instanceof AntennaNetworkObject networkObject) {
				StaticAntenna<?> antenna = networkObject.getAntenna();
				if(antenna != null)
					pPlayer.displayClientMessage(Component.literal(antenna.type.toString()).withStyle(ChatFormatting.GREEN), false);
				else
					pPlayer.displayClientMessage(Component.literal("No valid antenna found.").withStyle(ChatFormatting.RED), false);
			}
		}
		return super.useWithoutItem(pState, pLevel, pPos, pPlayer, pHitResult);
	}


	@Override
	protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack pStack, @NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHitResult) {
		if(!pLevel.isClientSide) {
			if(IBENetworks.getObject(pLevel, pPos) instanceof AntennaNetworkObject networkObject) {
				StaticAntenna<?> antenna = networkObject.getAntenna();
				if(antenna != null)
					pPlayer.displayClientMessage(Component.literal(antenna.type.toString()).withStyle(ChatFormatting.GREEN), false);
				else
					pPlayer.displayClientMessage(Component.literal("No valid antenna found.").withStyle(ChatFormatting.RED), false);
			}
		}
		return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return !level.isClientSide ? AntennaBlockEntity::tick : super.getTicker(level, state, type);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new AntennaBlockEntity(pos, state, AntennaNetworkManager.HF_ID);
	}

}

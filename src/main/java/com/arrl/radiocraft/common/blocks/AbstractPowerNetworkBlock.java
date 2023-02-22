package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractPowerNetworkBlock extends BaseEntityBlock {

	protected AbstractPowerNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide)
			for(PowerNetwork network : ((IPowerNetworkItem)level.getBlockEntity(pos)).getNetworks().values()) // For debug
				Radiocraft.LOGGER.info(network.getConnections().toString());

		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(oldState.getBlock() != this) {
			if(level.getBlockEntity(pos) instanceof IPowerNetworkItem networkItem) {
				for(Direction direction : Direction.values()) {

				}
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(newState.getBlock() != this) {
			if(level.getBlockEntity(pos) instanceof IPowerNetworkItem networkItem) {
				for(PowerNetwork network : networkItem.getNetworks().values())
					network.removeConnection(networkItem); // Remove self from networks.
				networkItem.getNetworks().clear();
			}
		}
	}
}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerNetwork;
import com.arrl.radiocraft.common.power.PowerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.Map;

public abstract class AbstractPowerNetworkBlock extends BaseEntityBlock {

	protected AbstractPowerNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide) {
			level.getBlockEntity(pos).getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> Radiocraft.LOGGER.info(cap.getEnergyStored() + "/" + cap.getMaxEnergyStored()));
		}

		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if(!level.isClientSide) {
			if(oldState.getBlock() != this) {
				if(level.getBlockEntity(pos) instanceof IPowerNetworkItem networkItem) {
					Map<Direction, PowerNetwork> networks = networkItem.getNetworks();
					for(Direction direction : Direction.values()) {
						if(networks.isEmpty() || !networks.keySet().contains(direction)) {
							PowerNetwork newNetwork = new PowerNetwork(null);
							newNetwork.addConnection(networkItem, networkItem.getDefaultConnectionType(), direction);
							networkItem.setNetwork(direction, newNetwork); // Fill with empty networks
						}
					}

					for(Direction direction : Direction.values()) {
						BlockPos checkPos = pos.relative(direction);
						BlockState checkState = level.getBlockState(checkPos);

						if(WireBlock.isWire(checkState))
							PowerUtils.mergeWireNetworks(level, checkPos); // Merge self with other networks if wire is found
					}
				}
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		if(!level.isClientSide) {
			if(newState.getBlock() != this) {
				if(level.getBlockEntity(pos) instanceof IPowerNetworkItem networkItem) {
					for(PowerNetwork network : networkItem.getNetworks().values())
						network.removeConnection(networkItem); // Remove self from networks.
					networkItem.getNetworks().clear();
				}
			}
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

}

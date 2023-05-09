package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.IBENetworkItem;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import com.arrl.radiocraft.common.power.PowerNetwork;
import com.arrl.radiocraft.common.power.WireUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.Set;

public abstract class AbstractPowerNetworkBlock extends BaseEntityBlock {

	protected AbstractPowerNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide) {
			if(hand == InteractionHand.MAIN_HAND) {
				BlockEntity be = level.getBlockEntity(pos);
				NetworkHooks.openScreen((ServerPlayer)player, (MenuProvider)be, pos);
			}
		}

		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(!level.isClientSide) {
			if(oldState.getBlock() != this) {
				for(Direction direction : Direction.values()) {
					WireUtils.mergeWireNetworks(level, pos.relative(direction),
							wire -> RadiocraftTags.isPowerWire(wire.getBlock()),
							connection -> RadiocraftTags.isPowerBlock(connection.getBlock()),
							network -> network instanceof PowerNetwork,
							PowerNetwork::new);
				}
			}
		}
	}



	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(!level.isClientSide) {
			if(newState.getBlock() != this) {
				if(level.getBlockEntity(pos) instanceof IBENetworkItem networkItem) {
					for(Set<BENetwork> side : networkItem.getNetworkMap().values()) {
						for(BENetwork network : side) {
							network.removeConnection(networkItem);
						}
						side.clear();
					}
				}
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

}

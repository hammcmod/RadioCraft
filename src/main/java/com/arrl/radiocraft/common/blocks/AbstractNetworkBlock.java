package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.common.benetworks.power.PowerNetwork;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractNetworkBlock extends BaseEntityBlock {

	private final boolean usePowerNetworks;
	private final boolean useCoaxNetworks;

	protected AbstractNetworkBlock(Properties properties, boolean usePowerNetworks, boolean useCoaxNetworks) {
		super(properties);
		this.usePowerNetworks = usePowerNetworks;
		this.useCoaxNetworks = useCoaxNetworks;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(!level.isClientSide) {
			if(oldState.getBlock() != this) {
				if(usePowerNetworks)
					BENetwork.tryConnectToNetworks(level, pos,
							wire -> RadiocraftTags.isPowerWire(wire.getBlock()),
							connection -> RadiocraftTags.isPowerBlock(connection.getBlock()),
							network -> network instanceof PowerNetwork,
							PowerNetwork::new);
				if(useCoaxNetworks)
					BENetwork.tryConnectToNetworks(level, pos,
							wire -> RadiocraftTags.isCoaxWire(wire.getBlock()),
							connection -> RadiocraftTags.isCoaxBlock(connection.getBlock()),
							network -> !(network instanceof PowerNetwork),
							BENetwork::new);
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(!level.isClientSide) {
			if(newState.getBlock() != this)
				BENetwork.tryRemoveFromNetworks(level, pos);
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

}

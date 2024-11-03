package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerBENetwork;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNetworkBlock extends BaseEntityBlock {

	protected AbstractNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
		if(!level.isClientSide) {
			if(oldState.getBlock() != this) {

				// TODO: This should be using capabilities.
				//boolean isPower = ForgeRegistries.BLOCKS.tags().getTag(RadiocraftTags.Blocks.POWER_BLOCKS).contains(this);
				//boolean isCoax = ForgeRegistries.BLOCKS.tags().getTag(RadiocraftTags.Blocks.COAX_BLOCKS).contains(this);

				boolean isPower = false;
				boolean isCoax = false;

				// This is awkward, but I don't see a good way to do it
				if(isPower)
					WireUtils.tryConnect(level, pos, no -> no instanceof PowerNetworkObject, PowerBENetwork::new, RadiocraftBlocks.WIRE.get());
				if(isCoax)
					WireUtils.tryConnect(level, pos, no -> no instanceof ICoaxNetworkObject, BENetwork::new, RadiocraftBlocks.COAX_WIRE.get());
			}
		}
	}

	@Override
	public void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if(!level.isClientSide) {
			if(newState.getBlock() != this) {
				BENetworkObject networkObject = IBENetworks.getObject(level, pos);
				if(networkObject != null)
					networkObject.clearNetworks();
				IBENetworks.removeObject(level, pos);
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
		return RenderShape.MODEL;
	}

}

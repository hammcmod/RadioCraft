package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import com.arrl.radiocraft.api.capabilities.IBENetworks;
import com.arrl.radiocraft.common.be_networks.ICoaxNetworkObject;
import com.arrl.radiocraft.common.be_networks.WireUtils;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public abstract class AbstractNetworkBlock extends BaseEntityBlock {

	protected AbstractNetworkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(!level.isClientSide) {
			if(oldState.getBlock() != this) {

				boolean isPower = ForgeRegistries.BLOCKS.tags().getTag(RadiocraftTags.Blocks.POWER_BLOCKS).contains(this);
				boolean isCoax = ForgeRegistries.BLOCKS.tags().getTag(RadiocraftTags.Blocks.COAX_BLOCKS).contains(this);

				// This is awkward, but I don't see a good way to do it
				if(isPower && isCoax)
					WireUtils.tryConnect(level, pos, no -> no instanceof PowerNetworkObject && no instanceof ICoaxNetworkObject, RadiocraftBlocks.WIRE.get(), RadiocraftBlocks.COAX_WIRE.get());
				else if(isPower)
					WireUtils.tryConnect(level, pos, no -> no instanceof PowerNetworkObject, RadiocraftBlocks.WIRE.get());
				else if(isCoax)
					WireUtils.tryConnect(level, pos, no -> no instanceof ICoaxNetworkObject, RadiocraftBlocks.COAX_WIRE.get());
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(!level.isClientSide) {
			if(newState.getBlock() != this) {
				BENetworkObject networkObject = IBENetworks.getObject(level, pos);
				if(networkObject != null)
					networkObject.clearNetworks();
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

}

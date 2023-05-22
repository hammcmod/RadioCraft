package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractRadioBlock extends AbstractPowerNetworkBlock {

	public AbstractRadioBlock(Properties properties) {
		super(properties, true);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide) {
			if(hand == InteractionHand.MAIN_HAND) {
				if(level.getBlockEntity(pos) instanceof AbstractRadioBlockEntity radio) {
					if(radio.getRadio().isReceiving())
						radio.powerOn();
					else
						radio.powerOff();
				}
			}
		}
		return super.use(state, level, pos, player, hand, hit);
	}

}

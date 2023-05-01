package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import com.arrl.radiocraft.common.radio.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType.DipoleAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BalunBlock extends Block {

	public BalunBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
			Antenna<?> antenna = AntennaTypes.match(level, pos);
			if(antenna != null) {
				if(antenna.data instanceof DipoleAntennaData data)
					Radiocraft.LOGGER.info(String.format("%s %s %s", antenna.type.getId().toString(), data.getArmLength1(), data.getArmLength2()));
				else
					Radiocraft.LOGGER.info(antenna.type.toString());
			}
			else
				Radiocraft.LOGGER.info("No antenna found.");
		}
		return super.use(state, level, pos, player, hand, hit);
	}

}

package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.blockentities.AbstractNetworkBlock;
import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.antenna.Antenna;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Use for any blocks which contain an AntennaBlockEntity (for example baluns)
 */
public class AntennaCenterBlock extends AbstractNetworkBlock {

	public AntennaCenterBlock(Properties properties) {
		super(properties, false, true);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if(!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
			Antenna<?> antenna = ((AntennaBlockEntity)level.getBlockEntity(pos)).antenna;
			if(antenna != null)
				Radiocraft.LOGGER.info(antenna.type.toString());
			else
				Radiocraft.LOGGER.info("No antenna found.");
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == RadiocraftBlockEntities.ANTENNA.get() ? AntennaBlockEntity::tick : super.getTicker(level, state, type);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AntennaBlockEntity(pos, state);
	}

}

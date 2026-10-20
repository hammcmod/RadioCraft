package com.arrl.radiocraft.common.blocks.radios;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.blocks.power.AbstractPowerNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class RadioBlock extends AbstractPowerNetworkBlock {

	public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

	public RadioBlock(Properties properties) {
		super(properties);
	}


	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return !level.isClientSide ? RadioBlockEntity::tick : null;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING);
	}

	// A comparator against any radio reads its S-meter: 0 with nothing coming in, up to 15 for a full-strength signal.

	@Override
	protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockEntity(pos) instanceof RadioBlockEntity radio ? radio.getMeterReading() : 0;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

}

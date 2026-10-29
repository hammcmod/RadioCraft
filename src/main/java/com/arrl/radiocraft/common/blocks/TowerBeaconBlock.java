package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tower's red obstruction light, after a 300 mm TWR beacon: as thin as the thin tower beam, lit while it has a
 * redstone signal (a Tower Beacon Controller under it blinks it 2 s on, 2 s off). Lit, it gives light level
 * {@link #LIGHT}, half again a redstone torch's 7, and its glass glows full bright. Unlike the redstone lamp it goes out
 * at once, so the blink is crisp. A beam under it reaches up into it ({@link TowerBeamBlock#attachesTo}).
 */
public class TowerBeaconBlock extends Block {

	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** Light level when lit: a redstone torch's 7, half again (10.5, rounded up). */
	public static final int LIGHT = 11;
	private static final VoxelShape SHAPE = Block.box(5.5D, 0.0D, 5.5D, 10.5D, 14.0D, 10.5D);

	@SuppressWarnings("this-escape")
	public TowerBeaconBlock(Properties properties) {
		super(properties.lightLevel(state -> state.getValue(LIT) ? LIGHT : 0));
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		return defaultBlockState().setValue(LIT, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighbor,
								   @NotNull BlockPos neighborPos, boolean movedByPiston) {
		if(level.isClientSide)
			return;
		boolean powered = level.hasNeighborSignal(pos);
		if(powered != state.getValue(LIT))
			level.setBlock(pos, state.setValue(LIT, powered), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
		if(!level.isClientSide && !oldState.is(this) && level.hasNeighborSignal(pos) != state.getValue(LIT))
			level.setBlock(pos, state.setValue(LIT, level.hasNeighborSignal(pos)), Block.UPDATE_CLIENTS);
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}
}

// Adapted from Create Crafts & Additions (https://github.com/mrh0/createaddition), MIT License, Copyright (c) 2024 MRH.
// See THIRD_PARTY.md.
package com.arrl.radiocraft.compat.create;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The alternator, as in Create Crafts &amp; Additions: a shaft through it turns a coil that makes Forge Energy, which
 * it pushes into anything beside it that takes FE (RadioCraft's large battery, other mods' machines). It only exists
 * when Create is installed and Crafts &amp; Additions is not (see {@link CreateCompat}).
 */
public class AlternatorBlock extends DirectionalKineticBlock implements IBE<AlternatorBlockEntity>, IRotate {

	/** The housing (full width, short along the shaft so its ends show) and the coil drum, by the way the shaft runs. */
	private static final Map<Axis, VoxelShape> SHAPES = new EnumMap<>(Axis.class);

	static {
		VoxelShape drum = Block.box(2, 2, 2, 14, 14, 14);
		SHAPES.put(Axis.Y, Shapes.or(Block.box(0, 3, 0, 16, 13, 16), drum));
		SHAPES.put(Axis.X, Shapes.or(Block.box(3, 0, 0, 13, 16, 16), drum));
		SHAPES.put(Axis.Z, Shapes.or(Block.box(0, 0, 3, 16, 16, 13), drum));
	}

	public AlternatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING).getAxis());
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredFacing(context);
		if((context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred);
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(FACING).getAxis();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	public BlockEntityType<? extends AlternatorBlockEntity> getBlockEntityType() {
		return CreateAlternator.BLOCK_ENTITY.get();
	}

	@Override
	public Class<AlternatorBlockEntity> getBlockEntityClass() {
		return AlternatorBlockEntity.class;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		if(level.getBlockEntity(pos) instanceof AlternatorBlockEntity alternator)
			alternator.updateCache();
	}
}

package com.arrl.radiocraft.common.items;

import com.arrl.radiocraft.common.blocks.TowerFootholdBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A tower foothold. Clicking the front of a foothold with one extends its stack: the new one goes on top of the
 * column of footholds facing that way, as scaffolding extends, so a climb can be built up past a gap in a lattice leg
 * with nothing behind it. (A foothold is a thin rung; aiming at its top face is fiddly, and its front face would
 * otherwise place the new one out in front, where it has nothing to hang on.)
 */
public class TowerFootholdItem extends BlockItem {

	/** Furthest up a stack a click on its front reaches. */
	public static final int MAX_REACH = 16;

	public TowerFootholdItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos().relative(context.getClickedFace().getOpposite());
		BlockState there = level.getBlockState(clicked);
		if(context.replacingClickedOnBlock() || !(there.getBlock() instanceof TowerFootholdBlock)
				|| context.getClickedFace() != there.getValue(LadderBlock.FACING))
			return context;
		Direction facing = there.getValue(LadderBlock.FACING);
		BlockPos pos = clicked.above();
		for(int i = 0; i < MAX_REACH; i++, pos = pos.above()) {
			BlockState at = level.getBlockState(pos);
			if(at.getBlock() instanceof TowerFootholdBlock && at.getValue(LadderBlock.FACING) == facing)
				continue; // Up the stack.
			if(!level.isInWorldBounds(pos) || !at.canBeReplaced())
				return null;
			return BlockPlaceContext.at(context, pos, Direction.UP);
		}
		return null;
	}
}

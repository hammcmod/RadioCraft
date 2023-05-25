package com.arrl.radiocraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AntennaConnectorBlock extends Block {
    public AntennaConnectorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.or(
                Block.box(6.0D, 3.0D, 6.0D, 10.0D, 5.0D, 10.0D),
                Block.box(6.0D, 0.0D, 6.0D, 10.0D, 2.0D, 10.0D),
                Block.box(7.0D, 5.0D, 7.0D, 9.0D, 6.0D, 9.0D),
                Block.box(7.0D, 2.0D, 7.0D, 9.0D, 3.0D, 9.0D));
    }
}

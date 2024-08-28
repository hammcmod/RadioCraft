package com.arrl.radiocraft.common.blocks;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class HFRadioAllBandBlock extends RadioBlock {

    public static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>();

    static {
        SHAPES.put(Direction.NORTH, Block.box(1.0D, 0.0D, 3.0D, 15.0D, 5.0D, 16.0D));
        SHAPES.put(Direction.SOUTH, Block.box(1.0D, 0.0D, 0.0D, 15.0D, 5.0D, 13.0D));
        SHAPES.put(Direction.WEST, Block.box(3.0D, 0.0D, 1.0D, 16.0D, 5.0D, 15.0D));
        SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 1.0D, 13.0D, 5.0D, 15.0D));
    }

    public HFRadioAllBandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HFRadioAllBandBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(RadioBlock.HORIZONTAL_FACING));
    }

}
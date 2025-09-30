package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class VHFRadioBlockEntity extends RadioBlockEntity {

    public VHFRadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, Band band) {
        super(type, pos, state, band);
        ssbEnabled = true;
    }

}

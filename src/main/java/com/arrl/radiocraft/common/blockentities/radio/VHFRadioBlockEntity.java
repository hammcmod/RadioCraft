package com.arrl.radiocraft.common.blockentities.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class VHFRadioBlockEntity extends RadioBlockEntity {

    public VHFRadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, int wavelength) {
        super(type, pos, state, wavelength);
        ssbEnabled = true;
    }

}

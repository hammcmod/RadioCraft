package com.arrl.radiocraft.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class VHFRadioBlockEntity extends RadioBlockEntity {

    public VHFRadioBlockEntity(BlockEntityType<? extends RadioBlockEntity> type, BlockPos pos, BlockState state, int receiveUsePower, int transmitUsePower, int wavelength) {
        super(type, pos, state, receiveUsePower, transmitUsePower, wavelength);
        ssbEnabled = true;
    }

}

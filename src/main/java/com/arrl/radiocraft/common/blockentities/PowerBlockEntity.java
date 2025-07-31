package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public abstract class PowerBlockEntity extends BlockEntity implements MenuProvider, INetworkObjectProvider, IEnergyStorage {

    public PowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int receiveEnergy(int i, boolean b) {
        return 0;
    }

    public int extractEnergy(int i, boolean b) {
        return 0;
    }

    public int getEnergyStored() {
        return 0;
    }

    public int getMaxEnergyStored() {
        return 0;
    }

    public boolean canExtract() {
        return false;
    }

    public boolean canReceive() {
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide())
            getNetworkObject(level, worldPosition); // This forces the network object to get initialised.
    }

}

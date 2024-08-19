package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.api.benetworks.PowerNetworkObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PowerBlockEntity extends BlockEntity implements MenuProvider, INetworkObjectProvider {

    public PowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /*

    TODO: Review what this may have done.. doesn't seem to do much.

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY ?
                ((PowerNetworkObject)getNetworkObject(level, worldPosition)).getStorage() :
                super.getCapability(cap, side);
    }*/

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide())
            getNetworkObject(level, worldPosition); // This forces the network object to get initialised.
    }

}

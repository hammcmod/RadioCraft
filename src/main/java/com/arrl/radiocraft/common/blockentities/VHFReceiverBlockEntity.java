package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VHFReceiverBlockEntity extends RadioBlockEntity {

    public VHFReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.VHF_RECEIVER.get(), pos, state, RadiocraftCommonConfig.VHF_RECEIVER_TICK.get(), 0, 2);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vhf_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    public boolean canTransmitVoice() {
        return false;
    }

}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFReceiverMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFReceiverBlockEntity extends HFRadioBlockEntity {

    public HFReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.HF_RECEIVER.get(), pos, state, RadiocraftCommonConfig.HF_RECEIVER_TICK.get(), RadiocraftCommonConfig.HF_RECEIVER_TICK.get(), 10);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hf_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new HFReceiverMenu(id, this);
    }

    @Override
    public boolean canTransmitVoice() {
        return false;
    }

}

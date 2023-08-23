package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.QRPRadioMenu20m;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class QRPRadio20mBlockEntity extends AbstractRadioBlockEntity {

    public QRPRadio20mBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.QRP_RADIO_20M.get(), pos, state, RadiocraftCommonConfig.QRP_RADIO_20M_RECEIVE_TICK.get(), RadiocraftCommonConfig.QRP_RADIO_20M_TRANSMIT_TICK.get(), 20);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.qrp_radio_20m");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QRPRadioMenu20m(id, this, fields);
    }
}
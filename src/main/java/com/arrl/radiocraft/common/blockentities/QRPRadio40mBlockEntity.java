package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.QRPRadioMenu40m;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class QRPRadio40mBlockEntity extends HFRadioBlockEntity {

    public QRPRadio40mBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.QRP_RADIO_40M.get(), pos, state, RadiocraftCommonConfig.QRP_RADIO_40M_RECEIVE_TICK.get(), RadiocraftCommonConfig.QRP_RADIO_40M_TRANSMIT_TICK.get(), 40);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.qrp_radio_40m");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QRPRadioMenu40m(id, this, fields);
    }
}
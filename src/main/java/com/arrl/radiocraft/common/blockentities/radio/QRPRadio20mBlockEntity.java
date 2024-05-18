package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.benetworks.power.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.QRPRadio20mMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class QRPRadio20mBlockEntity extends HFRadioBlockEntity {

    public QRPRadio20mBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.QRP_RADIO_20M.get(), pos, state, 20);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.qrp_radio_20m");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QRPRadio20mMenu(id, this);
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.QRP_RADIO_20M_TRANSMIT_TICK.get(), CommonConfig.QRP_RADIO_20M_RECEIVE_TICK.get());
    }

}
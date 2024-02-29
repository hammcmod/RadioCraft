package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.QRPRadio20mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class QRPRadio20mMenu extends RadioMenu<QRPRadio20mBlockEntity> {

    public QRPRadio20mMenu(int id, QRPRadio20mBlockEntity blockEntity) {
        super(RadiocraftMenuTypes.QRP_RADIO_20M.get(), id, blockEntity, RadiocraftBlocks.QRP_RADIO_20M.get());
    }

    public QRPRadio20mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(id, MenuUtils.getBlockEntity(playerInventory, data, QRPRadio20mBlockEntity.class));
    }

}
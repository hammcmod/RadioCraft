package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.QRPRadio20mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class QRPRadioMenu20m extends RadioMenu<QRPRadio20mBlockEntity> {

    public QRPRadioMenu20m(int id, QRPRadio20mBlockEntity blockEntity, ContainerData data) {
        super(RadiocraftMenuTypes.QRP_RADIO_20M.get(), id, blockEntity, data, RadiocraftBlocks.QRP_RADIO_20M.get());
    }

    public QRPRadioMenu20m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(id, null, new SimpleContainerData(2));
        blockEntity = getBlockEntity(playerInventory, data, QRPRadio20mBlockEntity.class);
    }

}
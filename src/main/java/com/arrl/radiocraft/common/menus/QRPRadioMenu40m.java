package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.QRPRadio40mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class QRPRadioMenu40m extends RadioMenu<QRPRadio40mBlockEntity> {

    public QRPRadioMenu40m(int id, QRPRadio40mBlockEntity blockEntity, ContainerData data) {
        super(RadiocraftMenuTypes.QRP_RADIO_40M.get(), id, blockEntity, data, RadiocraftBlocks.QRP_RADIO_40M.get());
    }

    public QRPRadioMenu40m(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(id, null, new SimpleContainerData(2));
        blockEntity = getBlockEntity(playerInventory, data, QRPRadio40mBlockEntity.class);
    }

}
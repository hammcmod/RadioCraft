package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.QRPRadio40mBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class QRPRadio40mMenu extends RadioMenu<QRPRadio40mBlockEntity> {

    public QRPRadio40mMenu(int id, QRPRadio40mBlockEntity blockEntity, ContainerData data) {
        super(RadiocraftMenuTypes.QRP_RADIO_40M.get(), id, blockEntity, data, RadiocraftBlocks.QRP_RADIO_40M.get());
    }

    public QRPRadio40mMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(id, MenuUtils.getBlockEntity(playerInventory, data, QRPRadio40mBlockEntity.class), new SimpleContainerData(2));
    }

}
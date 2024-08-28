package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.HFRadio10mBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.HFRadioAllBandBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class HFRadioAllBandMenu  extends RadioMenu<HFRadioAllBandBlockEntity> {

    public HFRadioAllBandMenu(int id, HFRadioAllBandBlockEntity blockEntity) {
        super(RadiocraftMenuTypes.HF_RADIO_ALL_BAND.get(), id, blockEntity, RadiocraftBlocks.ALL_BAND_RADIO.get());
    }

    public HFRadioAllBandMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) { // Clientside constructor doesn't need a RadioNetworkObject
        this(id, MenuUtils.getBlockEntity(playerInventory, data, HFRadioAllBandBlockEntity.class));
    }

}
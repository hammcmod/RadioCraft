package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFRadioAllBandMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HFRadioAllBandBlockEntity extends HFRadioBlockEntity {

    public HFRadioAllBandBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.HF_RADIO_ALL_BAND.get(), pos, state, 10);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hf_radio_all_band");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new HFRadioAllBandMenu(id, this);
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.HF_RADIO_ALL_BAND_TRANSMIT_TICK.get(), CommonConfig.HF_RADIO_ALL_BAND_RECEIVE_TICK.get());
    }

}


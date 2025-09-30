package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.HFReceiverMenu;
import com.arrl.radiocraft.common.radio.Band;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HFReceiverBlockEntity extends HFRadioBlockEntity {

    public HFReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.HF_RECEIVER.get(), pos, state, Band.getBand("10m"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hf_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new HFReceiverMenu(id, this);
    }

    @Override
    public boolean canTransmitVoice() {
        return false;
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.HF_RADIO_10M_TRANSMIT_TICK.get(), CommonConfig.HF_RADIO_10M_RECEIVE_TICK.get());
    }

}

package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.be_networks.network_objects.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.VHFReceiverMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VHFReceiverBlockEntity extends VHFRadioBlockEntity {

    public VHFReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.VHF_RECEIVER.get(), pos, state, 2);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vhf_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VHFReceiverMenu(id, this);
    }

    @Override
    public boolean canTransmitVoice() {
        return false;
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.VHF_RECEIVER_TICK.get(), CommonConfig.VHF_RECEIVER_TICK.get());
    }

}

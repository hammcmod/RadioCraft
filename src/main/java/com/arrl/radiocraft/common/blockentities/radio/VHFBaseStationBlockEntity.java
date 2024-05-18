package com.arrl.radiocraft.common.blockentities.radio;

import com.arrl.radiocraft.CommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.common.benetworks.power.RadioNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.VHFBaseStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VHFBaseStationBlockEntity extends VHFRadioBlockEntity {

    public VHFBaseStationBlockEntity(BlockPos pos, BlockState state) {
        super(RadiocraftBlockEntities.VHF_BASE_STATION.get(), pos, state, 2);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vhf_base_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VHFBaseStationMenu(id, this);
    }

    @Override
    public BENetworkObject createNetworkObject() {
        return new RadioNetworkObject(level, worldPosition, CommonConfig.VHF_BASE_STATION_RECEIVE_TICK.get(), CommonConfig.VHF_BASE_STATION_TRANSMIT_TICK.get());
    }

}

package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.FMRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FMRadioMenu extends AbstractContainerMenu {

    public FMRadioBlockEntity blockEntity;

    private final ContainerLevelAccess canInteractWithCallable;

    public FMRadioMenu(int containerId, FMRadioBlockEntity blockEntity) {
        super(RadiocraftMenuTypes.FM_RADIO.get(), containerId);
        this.blockEntity = blockEntity;
        this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
    }

    public FMRadioMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        this(id, MenuUtils.getBlockEntity(playerInventory, data, FMRadioBlockEntity.class));
    }

    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(canInteractWithCallable, player, RadiocraftBlocks.FM_RADIO.get());
    }
}

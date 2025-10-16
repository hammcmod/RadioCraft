package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.DeskChargerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DeskChargerMenu extends AbstractContainerMenu {

    public final DeskChargerBlockEntity blockEntity;
    private final ContainerLevelAccess canInteractWithCallable;

    private final Level level;

    public DeskChargerMenu(int containerId, Inventory inv, DeskChargerBlockEntity blockEntity) {
        super(RadiocraftMenuTypes.DESK_CHARGER.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == RadiocraftItems.VHF_HANDHELD.get();
            }
        });
    }

    public DeskChargerMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, MenuUtils.getBlockEntity(playerInventory, data, DeskChargerBlockEntity.class));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(canInteractWithCallable, player, RadiocraftBlocks.DESK_CHARGER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // slot indices: player hotbar 0-8, player inventory 9-35, tile slot starts at 36
        final int HOTBAR_SLOT_COUNT = 9;
        final int PLAYER_INVENTORY_ROW_COUNT = 3;
        final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
        final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
        final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
        final int VANILLA_FIRST_SLOT_INDEX = 0;
        final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
        final int TE_INVENTORY_SLOT_COUNT = 1;

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}

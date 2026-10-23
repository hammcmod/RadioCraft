package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.TransferInterfaceBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The digital transfer interface's screen. Its state reaches the client with the block entity's update tag; its slots
 * are the file to send, the copies received, the paper, book and map supply, and the player's inventory.
 */
public class TransferInterfaceMenu extends AbstractContainerMenu implements StationMenu {

	/** Where the player's inventory starts, below the interface's own panel (a vanilla panel until it has art). */
	public static final int INVENTORY_X = 28;
	public static final int INVENTORY_Y = 147;

	public final TransferInterfaceBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;

	private static final int OUTBOX_SLOT = 0;
	private static final int RECEIVED_START = 1;
	private static final int SUPPLY_START = RECEIVED_START + TransferInterfaceBlockEntity.RECEIVED_SLOTS;
	private static final int PLAYER_START = SUPPLY_START + 3;
	private static final int PLAYER_END = PLAYER_START + 36;

	public TransferInterfaceMenu(int id, Inventory playerInventory, TransferInterfaceBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.TRANSFER_INTERFACE.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());

		addSlot(new SlotItemHandler(blockEntity.getOutbox(), 0, 59, 39));
		int[] receivedX = {28, 51, 74, 97};
		for(int i = 0; i < TransferInterfaceBlockEntity.RECEIVED_SLOTS; i++)
			addSlot(new SlotItemHandler(blockEntity.getReceived(), i, receivedX[i], 81));
		addSlot(new SlotItemHandler(blockEntity.getSupplies(), TransferInterfaceBlockEntity.SLOT_PAPER, 134, 108));
		addSlot(new SlotItemHandler(blockEntity.getSupplies(), TransferInterfaceBlockEntity.SLOT_BOOK, 159, 108));
		addSlot(new SlotItemHandler(blockEntity.getSupplies(), TransferInterfaceBlockEntity.SLOT_MAP, 183, 108));

		for(int row = 0; row < 3; row++)
			for(int col = 0; col < 9; col++)
				addSlot(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		for(int col = 0; col < 9; col++)
			addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
	}

	public TransferInterfaceMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, playerInventory, MenuUtils.getBlockEntity(playerInventory, data, TransferInterfaceBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		if(action.equals("file") && !blockEntity.sendFile(data.getString("to")))
			player.displayClientMessage(Component.translatable(blockEntity.isTransferring() ? "screen.radiocraft.transfer_interface.busy"
					: blockEntity.getOutbox().getStackInSlot(0).isEmpty() ? "screen.radiocraft.transfer_interface.nothing_to_send"
					: StationMenu.failureKey(blockEntity.bus())), true);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.TRANSFER_INTERFACE.get());
	}

	/** Shift click: from the interface to the player; from the player to the supply it fits, or to the outbox. */
	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if(index < PLAYER_START) {
			if(!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true))
				return ItemStack.EMPTY;
		}
		else {
			boolean moved = false;
			for(int s = 0; s < 3; s++)
				if(TransferInterfaceBlockEntity.isSupply(s, stack))
					moved = moveItemStackTo(stack, SUPPLY_START + s, SUPPLY_START + s + 1, false);
			if(!moved && FileTransfer.canSend(stack))
				moved = moveItemStackTo(stack, OUTBOX_SLOT, OUTBOX_SLOT + 1, false);
			if(!moved)
				return ItemStack.EMPTY;
		}
		if(stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		return original;
	}
}

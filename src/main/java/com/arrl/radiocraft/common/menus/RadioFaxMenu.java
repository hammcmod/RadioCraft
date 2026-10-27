package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.RadioFaxBlockEntity;
import com.arrl.radiocraft.common.blocks.RadioFaxBlock;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.radio.data.FileTransfer;
import com.arrl.radiocraft.common.radio.data.TncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * The radio fax's screen. Its state reaches the client with the block entity's update tag; its slots are the document
 * to send, the copies received, the paper, book and map supply, and the player's inventory.
 */
public final class RadioFaxMenu extends AbstractContainerMenu implements StationMenu {

	/** Where the player's inventory starts, below the fax's own panel (a vanilla panel until it has art). */
	public static final int INVENTORY_X = 28;
	public static final int INVENTORY_Y = 128;
	/** Where the slots are on the panel: the document, the received tray and the supplies (see tools/radio_fax_models.py). */
	public static final int[] OUTBOX_AT = {20, 30};
	public static final int[] TRAY_X = {110, 132, 154, 176};
	public static final int TRAY_Y = 80;
	public static final int[] SUPPLY_X = {20, 42, 64};
	public static final int SUPPLY_Y = 80;

	public final RadioFaxBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;

	private static final int OUTBOX_SLOT = 0;
	private static final int RECEIVED_START = 1;
	private static final int SUPPLY_START = RECEIVED_START + RadioFaxBlockEntity.RECEIVED_SLOTS;
	private static final int PLAYER_START = SUPPLY_START + 3;
	private static final int PLAYER_END = PLAYER_START + 36;

	public RadioFaxMenu(int id, Inventory playerInventory, RadioFaxBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.RADIO_FAX.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());

		addSlot(new SlotItemHandler(blockEntity.getOutbox(), 0, OUTBOX_AT[0], OUTBOX_AT[1]));
		for(int i = 0; i < RadioFaxBlockEntity.RECEIVED_SLOTS; i++)
			addSlot(new SlotItemHandler(blockEntity.getReceived(), i, TRAY_X[i], TRAY_Y));
		for(int i = 0; i < 3; i++)
			addSlot(new SlotItemHandler(blockEntity.getSupplies(), i, SUPPLY_X[i], SUPPLY_Y));

		for(int row = 0; row < 3; row++)
			for(int col = 0; col < 9; col++)
				addSlot(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		for(int col = 0; col < 9; col++)
			addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
	}

	public RadioFaxMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, playerInventory, MenuUtils.getBlockEntity(playerInventory, data, RadioFaxBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "send" -> {
				String problem = blockEntity.send(data.getString("to"));
				if(problem != null)
					player.displayClientMessage(Component.translatable(problem), true);
			}
			case "callsign" -> {
				if(!TncPacket.sanitizeCallsign(data.getString("callsign")).isEmpty())
					blockEntity.setCallsign(data.getString("callsign"));
				else if(!blockEntity.claim(player)) // Empty means "mine".
					player.displayClientMessage(Component.translatable("screen.radiocraft.radio_interface.no_callsign"), true);
			}
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return canInteractWithCallable.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof RadioFaxBlock
				&& player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
	}

	/** Shift click: from the fax to the player; from the player to the supply it fits, or to the document slot. */
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
				if(RadioFaxBlockEntity.isSupply(s, stack))
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

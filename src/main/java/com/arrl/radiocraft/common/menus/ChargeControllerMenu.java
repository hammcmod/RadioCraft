package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.ChargeControllerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ChargeControllerMenu extends AbstractContainerMenu {

	public final ChargeControllerBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

    @SuppressWarnings("this-escape")
	public ChargeControllerMenu(int id, Inventory playerInventory, @NotNull ChargeControllerBlockEntity be, ContainerData data) {
		super(RadiocraftMenuTypes.CHARGE_CONTROLLER.get(), id);
		this.blockEntity = be;
		this.canInteractWithCallable = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
		this.data = data;
		addDataSlots(this.data);

		addSlot(new BatterySlot(be.inventory, 0, 66, 41));

		for (int y = 0; y < 3; y++) { // Main Inventory
			for (int x = 0; x < 9; x++) {
				addSlot(new Slot(playerInventory, x + (y * 9) + 9, 8 + (x * 18), 131 + (y * 18)));
			}
		}
		for (int x = 0; x < 9; x++) { // Hotbar
			addSlot(new Slot(playerInventory, x, 8 + (18 * x), 189));
		}

	}

	public ChargeControllerMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
		this(id, playerInventory, MenuUtils.getBlockEntity(playerInventory, data, ChargeControllerBlockEntity.class), new SimpleContainerData(1));
	}

	public int getPowerTick() {
		return data.get(0);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.CHARGE_CONTROLLER.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}

	public static class BatterySlot extends SlotItemHandler {

		public BatterySlot(ItemStackHandler inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.getItem() == RadiocraftItems.SMALL_BATTERY.get();
		}

	}

}
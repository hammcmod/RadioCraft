package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.ChargeControllerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftItems;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class ChargeControllerMenu extends AbstractContainerMenu {

	public final ChargeControllerBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public ChargeControllerMenu(final int id, final ChargeControllerBlockEntity be, ContainerData data) {
		super(RadiocraftMenuTypes.CHARGE_CONTROLLER.get(), id);
		this.blockEntity = be;
		this.canInteractWithCallable = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
		this.data = data;
		addDataSlots(this.data);

		addSlot(new BatterySlot(be.inventoryWrapper, 0, 172, 178));
	}

	public ChargeControllerMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, ChargeControllerBlockEntity.class), new SimpleContainerData(1));
	}

	public int getPowerTick() {
		return data.get(0);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.CHARGE_CONTROLLER.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	public static class BatterySlot extends Slot {

		public BatterySlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.getItem() == RadiocraftItems.SMALL_BATTERY.get();
		}

	}

}
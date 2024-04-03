package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.LargeBatteryBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class LargeBatteryMenu extends AbstractContainerMenu {

	public final LargeBatteryBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;

	public LargeBatteryMenu(final int id, final LargeBatteryBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.LARGE_BATTERY.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return getCurrentPower() & 0xffff;
			}

			@Override
			public void set(int value) {
				int frequency = getCurrentPower() & 0xffff0000;
				setCurrentPower(frequency + (value & 0xffff));
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return (getCurrentPower() << 16) & 0x0000ffff;
			}

			@Override
			public void set(int value) {
				int frequency = getCurrentPower() & 0x0000ffff;
				setCurrentPower(frequency + (value << 16));
			}
		});

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return getMaxPower() & 0xffff;
			}

			@Override
			public void set(int value) {
				int frequency = getMaxPower() & 0xffff0000;
				setMaxPower(frequency + (value & 0xffff));
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return (getMaxPower() << 16) & 0x0000ffff;
			}

			@Override
			public void set(int value) {
				int frequency = getMaxPower() & 0x0000ffff;
				setMaxPower(frequency + (value << 16));
			}
		});

	}

	public LargeBatteryMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, LargeBatteryBlockEntity.class));
	}

	public int getCurrentPower() {
		return blockEntity.getEnergy().getEnergyStored();
	}

	public int getMaxPower() {
		return blockEntity.getEnergy().getMaxEnergyStored();
	}

	public void setCurrentPower(int value) {
		blockEntity.getEnergy().setEnergy(value);
	}

	public void setMaxPower(int value) {
		blockEntity.getEnergy().setMaxEnergy(value);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.LARGE_BATTERY.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return null;
	}

}
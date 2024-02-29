package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.RadioBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class RadioMenu<T extends RadioBlockEntity> extends AbstractContainerMenu {

	public T blockEntity;
	private final Block validBlock;
	private final ContainerLevelAccess canInteractWithCallable;

	public RadioMenu(MenuType<?> type, final int id, final T blockEntity, Block validBlock) {
		super(type, id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.validBlock = validBlock;

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return getWavelength();
			}

			@Override
			public void set(int value) {
				setWavelength(value);
			}
		});

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return getFrequency() & 0xffff;
			}

			@Override
			public void set(int value) {
				int frequency = getFrequency() & 0xffff0000;
				setFrequency(frequency + (value & 0xffff));
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return (getFrequency() << 16) & 0x0000ffff;
			}

			@Override
			public void set(int value) {
				int frequency = getFrequency() & 0x0000ffff;
				setFrequency(frequency + (value << 16));
			}
		});
	}

	public int getFrequency() {
		return blockEntity.getFrequency();
	}

	public void setFrequency(int value) {
		blockEntity.setFrequency(value);
	}

	public int getWavelength() {
		return blockEntity.getWavelength();
	}

	public void setWavelength(int value) {
		blockEntity.setWavelength(value);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, validBlock);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return null;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if(blockEntity != null) {
			blockEntity.setPTTDown(false);
		}
	}

}
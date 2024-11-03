package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RadioMenu<T extends RadioBlockEntity> extends AbstractContainerMenu {

	public T blockEntity;
	private final Block validBlock;
	private final ContainerLevelAccess canInteractWithCallable;

	private final ContainerData data;

	public RadioMenu(MenuType<?> type, int id, T blockEntity, Block validBlock) {
		super(type, id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
		this.validBlock = validBlock;
		this.data = blockEntity.getDataSlots();

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
				return getFrequency() & 0xFFFF;
			}

			@Override
			public void set(int value) {
				int frequency = getFrequency() & 0xFFFF0000;
				setFrequency(frequency + (value & 0xFFFF));
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return (getFrequency() << 16) & 0x0000FFFF;
			}

			@Override
			public void set(int value) {
				int frequency = getFrequency() & 0x0000FFFF;
				setFrequency(frequency + (value << 16));
			}
		});
		addDataSlots(data);
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

	public boolean isPowered() {
		return data.get(0) == 1;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, validBlock);
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return null;
	}

	@Override
	public void removed(@NotNull Player player) {
		super.removed(player);
		if(blockEntity != null)
			blockEntity.setPTTDown(false);
	}

}
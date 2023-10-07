package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class AbstractHFRadioMenu extends AbstractContainerMenu {

	public final AbstractRadioBlockEntity blockEntity;
	private final Block validBlock;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public AbstractHFRadioMenu(MenuType<?> type, final int id, final AbstractRadioBlockEntity blockEntity, ContainerData data, Block validBlock) {
		super(type, id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.validBlock = validBlock;
		this.data = data;
		addDataSlots(this.data);
	}

	public AbstractHFRadioMenu(MenuType<?> type, final int id, final Inventory playerInventory, final FriendlyByteBuf data, Block validBlock) {
		this(type, id, getBlockEntity(playerInventory, data), new SimpleContainerData(1), validBlock);
	}

	protected static AbstractRadioBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
		final BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(data.readBlockPos());

		if(blockEntity instanceof AbstractRadioBlockEntity) {
			return (AbstractRadioBlockEntity)blockEntity;
		}
		throw new IllegalStateException("BlockEntity at " + data.readBlockPos() + " is not correct");
	}

	public boolean isPowered() {
		return blockEntity.isPowered();
	}

	public boolean getSSBEnabled() {
		return blockEntity.getSSBEnabled();
	}

	public boolean getCWEnabled() {
		return blockEntity.getCWEnabled();
	}

	public boolean isReceiving() {
		return blockEntity.isReceiving();
	}

	public boolean isTransmitting() {
		return getSSBEnabled() && blockEntity.isPTTDown();
	}

	public int getFrequency() {
		return data.get(0);
	}

	public void setFrequency(int value) {
		data.set(0, value);
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
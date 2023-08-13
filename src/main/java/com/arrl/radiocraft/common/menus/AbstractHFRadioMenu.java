package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AbstractRadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class AbstractHFRadioMenu extends AbstractContainerMenu {

	public final AbstractRadioBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public AbstractHFRadioMenu(MenuType<?> type, final int id, final AbstractRadioBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.HF_RADIO_10M.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
		this.data = data;
		addDataSlots(this.data);
	}

	public AbstractHFRadioMenu(MenuType<?> type, final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(type, id, getBlockEntity(playerInventory, data), new SimpleContainerData(3));
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
		return blockEntity.isReceivingVoice();
	}

	public boolean isTransmitting() {
		return getSSBEnabled() && blockEntity.isPTTDown();
	}

	public int getFrequency() {
		return data.get(2);
	}

	public void setFrequency(int value) {
		data.set(2, value);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.HF_RADIO_10M.get());
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
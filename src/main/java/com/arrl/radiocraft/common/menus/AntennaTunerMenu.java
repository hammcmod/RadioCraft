package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AntennaTunerMenu extends AbstractContainerMenu {

	public AntennaTunerBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;
	private final ContainerData data;

	public AntennaTunerMenu(int id, AntennaTunerBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.ANTENNA_TUNER.get(), id);
		this.blockEntity = blockEntity;
		this.data = data;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
		addDataSlots(data);
	}

	public AntennaTunerMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, AntennaTunerBlockEntity.class), new SimpleContainerData(AntennaTunerBlockEntity.DATA_COUNT));
	}

	/** One of the {@code AntennaTunerBlockEntity.STATUS_} constants, kept in sync from the server. */
	public int getStatus() {
		return data.get(AntennaTunerBlockEntity.DATA_STATUS);
	}

	/** The antenna's own SWR at the radio's frequency. */
	public double getRawSwr() {
		return data.get(AntennaTunerBlockEntity.DATA_RAW_SWR) / 100.0D;
	}

	/** The SWR the radio sees through the tuner. */
	public double getTunedSwr() {
		return data.get(AntennaTunerBlockEntity.DATA_TUNED_SWR) / 100.0D;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(canInteractWithCallable, player, RadiocraftBlocks.ANTENNA_TUNER.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}

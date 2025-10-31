package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AntennaTunerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AntennaTunerMenu extends AbstractContainerMenu {

	public AntennaTunerBlockEntity blockEntity;
	private final ContainerLevelAccess canInteractWithCallable;

	public AntennaTunerMenu(int id, AntennaTunerBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.ANTENNA_TUNER.get(), id);
		this.blockEntity = blockEntity;
		this.canInteractWithCallable = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
	}

	public AntennaTunerMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, AntennaTunerBlockEntity.class));
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

package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.DirectionFinderBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** The direction finder's screen. Its state reaches the client with the block entity's update tag; no slots. */
public class DirectionFinderMenu extends AbstractContainerMenu implements StationMenu {

	public final DirectionFinderBlockEntity blockEntity;

	public DirectionFinderMenu(int id, DirectionFinderBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.DIRECTION_FINDER.get(), id);
		this.blockEntity = blockEntity;
	}

	public DirectionFinderMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, DirectionFinderBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "power" -> blockEntity.togglePower();
			case "booted" -> blockEntity.markBooted();
			case "tune" -> blockEntity.tune(Math.max(-1000, Math.min(1000, data.getInt("steps"))));
			case "band" -> blockEntity.changeBand(data.getInt("direction") < 0 ? -1 : 1);
			case "frequency" -> blockEntity.setFrequency(data.getFloat("hertz"));
			case "add" -> blockEntity.addEntry(new DirectionFinderBlockEntity.Entry(data.getInt("x"), data.getInt("z"), data.getDouble("bearing"), data.getDouble("sigma")));
			case "remove" -> blockEntity.removeEntry(data.getInt("index"));
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}

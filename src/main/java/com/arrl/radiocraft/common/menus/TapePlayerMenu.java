package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.TapePlayerBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** The tape player's controls; no slots (the tape goes in by using it on the player). */
public class TapePlayerMenu extends AbstractContainerMenu implements StationMenu {

	public final TapePlayerBlockEntity blockEntity;
	private final ContainerData data;

	@SuppressWarnings("this-escape")
	public TapePlayerMenu(int id, TapePlayerBlockEntity blockEntity, ContainerData data) {
		super(RadiocraftMenuTypes.TAPE_PLAYER.get(), id);
		this.blockEntity = blockEntity;
		this.data = data;
		addDataSlots(data);
	}

	public TapePlayerMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf buffer) {
		this(id, MenuUtils.getBlockEntity(playerInventory, buffer, TapePlayerBlockEntity.class), new SimpleContainerData(TapePlayerBlockEntity.DATA_COUNT));
	}

	/** A readout from the server, see {@link TapePlayerBlockEntity#dataSlots}. */
	public int get(int index) {
		return data.get(index);
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag tag) {
		switch(action) {
			case "play" -> blockEntity.play();
			case "stop" -> blockEntity.stop();
			case "record" -> blockEntity.record();
			case "mode" -> {
				TapePlayerBlockEntity.Mode[] modes = TapePlayerBlockEntity.Mode.values();
				blockEntity.setMode(modes[Math.floorMod(tag.getInt("mode"), modes.length)]);
			}
			case "interval" -> blockEntity.setIntervalMinutes(tag.getInt("minutes"));
			case "eject" -> {
				ItemStack tape = blockEntity.eject();
				if(!tape.isEmpty() && !player.getInventory().add(tape))
					player.drop(tape, false);
			}
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

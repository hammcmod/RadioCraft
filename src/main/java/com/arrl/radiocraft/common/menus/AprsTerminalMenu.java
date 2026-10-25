package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.AprsTerminalBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** The APRS terminal's screen. Its state reaches the client with the block entity's update tag; no slots. */
public class AprsTerminalMenu extends AbstractContainerMenu implements StationMenu {

	public final AprsTerminalBlockEntity blockEntity;
	private final ContainerLevelAccess access;

	public AprsTerminalMenu(int id, AprsTerminalBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.APRS_TERMINAL.get(), id);
		this.blockEntity = blockEntity;
		this.access = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
	}

	public AprsTerminalMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, AprsTerminalBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "beacon" -> {
				if(!blockEntity.beacon())
					player.displayClientMessage(Component.translatable(StationMenu.failureKey(blockEntity.bus())), true);
			}
			case "message" -> {
				if(!blockEntity.sendMessage(data.getString("to"), data.getString("text")))
					player.displayClientMessage(Component.translatable(StationMenu.failureKey(blockEntity.bus())), true);
			}
			case "settings" -> blockEntity.applySettings(data);
			case "callsign" -> {
				if(!blockEntity.setCallsign(player, data.getString("callsign")))
					player.displayClientMessage(Component.translatable(blockEntity.bus().hasInterface() ? "screen.radiocraft.radio_interface.no_callsign"
							: "screen.radiocraft.station.no_interface"), true);
			}
			case "read" -> blockEntity.markSeen();
			case "booted" -> blockEntity.markBooted();
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(access, player, RadiocraftBlocks.APRS_TERMINAL.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}

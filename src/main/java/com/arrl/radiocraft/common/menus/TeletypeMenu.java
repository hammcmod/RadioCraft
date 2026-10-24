package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.TeletypeBlockEntity;
import com.arrl.radiocraft.common.blockentities.radio.RadioBlockEntity;
import com.arrl.radiocraft.common.init.RadiocraftBlocks;
import com.arrl.radiocraft.common.init.RadiocraftMenuTypes;
import com.arrl.radiocraft.common.radio.data.TncPacket;
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

/** The teletype's screen: the paper roll, typing, its callsign and SOLAR WX. No slots; paper goes in by hand or hopper. */
public final class TeletypeMenu extends AbstractContainerMenu implements StationMenu {

	public final TeletypeBlockEntity blockEntity;
	private final ContainerLevelAccess access;

	public TeletypeMenu(int id, TeletypeBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.TELETYPE.get(), id);
		this.blockEntity = blockEntity;
		this.access = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
	}

	public TeletypeMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, TeletypeBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "rtty" -> {
				if(!blockEntity.typeRtty(data.getString("text")))
					player.displayClientMessage(Component.translatable(blockEntity.getCallsign().isEmpty() ? "screen.radiocraft.teletype.set_callsign_first"
							: blockEntity.getRadios().stream().anyMatch(RadioBlockEntity::isPowered) ? "screen.radiocraft.teletype.send_failed"
							: "screen.radiocraft.teletype.no_radio"), true);
			}
			case "solar" -> blockEntity.printReport();
			case "callsign" -> {
				if(!TncPacket.sanitizeCallsign(data.getString("callsign")).isEmpty())
					blockEntity.setCallsign(data.getString("callsign"), player.getGameProfile().getName());
				else if(!blockEntity.claim(player)) // Empty means "mine".
					player.displayClientMessage(Component.translatable("screen.radiocraft.radio_interface.no_callsign"), true);
			}
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(access, player, RadiocraftBlocks.TELETYPE.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}

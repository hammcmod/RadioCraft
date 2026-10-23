package com.arrl.radiocraft.common.menus;

import com.arrl.radiocraft.common.blockentities.RadioInterfaceBlockEntity;
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

/** The radio interface's screen: its callsign (empty claims your own) and the transmit switch. No slots. */
public class RadioInterfaceMenu extends AbstractContainerMenu implements StationMenu {

	public final RadioInterfaceBlockEntity blockEntity;
	private final ContainerLevelAccess access;

	public RadioInterfaceMenu(int id, RadioInterfaceBlockEntity blockEntity) {
		super(RadiocraftMenuTypes.RADIO_INTERFACE.get(), id);
		this.blockEntity = blockEntity;
		this.access = ContainerLevelAccess.create(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
	}

	public RadioInterfaceMenu(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
		this(id, MenuUtils.getBlockEntity(playerInventory, data, RadioInterfaceBlockEntity.class));
	}

	@Override
	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	@Override
	public void handleAction(ServerPlayer player, String action, CompoundTag data) {
		switch(action) {
			case "callsign" -> {
				if(!TncPacket.sanitizeCallsign(data.getString("callsign")).isEmpty())
					blockEntity.setCallsign(data.getString("callsign"), player.getGameProfile().getName());
				else if(!blockEntity.claim(player)) // Empty means "mine".
					player.displayClientMessage(Component.translatable("screen.radiocraft.radio_interface.no_callsign"), true);
			}
			case "transmit" -> blockEntity.setTransmitEnabled(data.getBoolean("transmit"));
			default -> {}
		}
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return stillValid(access, player, RadiocraftBlocks.RADIO_INTERFACE.get());
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		return ItemStack.EMPTY;
	}
}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.common.benetworks.power.ConnectionType;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LargeBatteryBlockEntity extends AbstractPowerBlockEntity {

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state, RadiocraftCommonConfig.LARGE_BATTERY_CAPACITY.get(), RadiocraftCommonConfig.LARGE_BATTERY_OUTPUT.get());
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(!level.isClientSide) {
			if(t instanceof LargeBatteryBlockEntity be) {
				be.pushToAll(be.energyStorage.getMaxExtract(), false); // Battery will always push as much power as it can, receives power from charge controllers
			}
		}
	}

	@Override
	public ConnectionType getConnectionType() {
		return ConnectionType.PUSH;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.large_battery");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new LargeBatteryMenu(id, this);
	}
}

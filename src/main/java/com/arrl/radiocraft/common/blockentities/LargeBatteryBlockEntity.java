package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftCommonConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.benetworks.power.BatteryNetworkObject;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.LargeBatteryMenu;
import com.arrl.radiocraft.common.menus.slots.IntRefSplitDataSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LargeBatteryBlockEntity extends BlockEntity implements MenuProvider, INetworkObjectProvider {

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.large_battery");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new LargeBatteryMenu(id, this,
				new IntRefSplitDataSlot(
						value -> ((BatteryNetworkObject)getNetworkObject(level, worldPosition)).getStorage().setEnergy(value),
						() -> ((BatteryNetworkObject)getNetworkObject(level, worldPosition)).getStorage().getEnergyStored()
				),
				new IntRefSplitDataSlot(
						value -> ((BatteryNetworkObject)getNetworkObject(level, worldPosition)).getStorage().setMaxEnergy(value),
						() -> ((BatteryNetworkObject)getNetworkObject(level, worldPosition)).getStorage().getMaxEnergyStored()
				));
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new BatteryNetworkObject(
				RadiocraftCommonConfig.LARGE_BATTERY_CAPACITY.get(),
				RadiocraftCommonConfig.LARGE_BATTERY_OUTPUT.get(),
				RadiocraftCommonConfig.LARGE_BATTERY_OUTPUT.get()
		);
	}

}

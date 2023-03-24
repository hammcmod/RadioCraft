package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LargeBatteryBlockEntity extends AbstractPowerBlockEntity {

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state, RadiocraftConfig.LARGE_BATTERY_CAPACITY.get(), RadiocraftConfig.LARGE_BATTERY_OUTPUT.get());
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
}

package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class LargeBatteryBlockEntity extends AbstractPowerBlockEntity {

	public LargeBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.LARGE_BATTERY.get(), pos, state, 5000, 100);
	}

	@Override
	public ConnectionType getDefaultConnectionType() {
		return ConnectionType.PUSH;
	}
}

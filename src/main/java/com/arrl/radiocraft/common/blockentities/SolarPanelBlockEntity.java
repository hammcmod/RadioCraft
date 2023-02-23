package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends AbstractPowerBlockEntity {

	public static final int POWER_PER_TICK = 10;
	public static final float RAIN_MULTIPLIER = 0.5F;

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state, 200, 15);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof SolarPanelBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				if(level.isDay()) { // Time is day
					int powerGenerated = level.isRaining() ? Math.round(POWER_PER_TICK * RAIN_MULTIPLIER) : POWER_PER_TICK;
					be.energyStorage.receiveEnergy(powerGenerated, false);
				}
			}
		}
	}

	@Override
	public ConnectionType getDefaultConnectionType() {
		return ConnectionType.PUSH;
	}
}

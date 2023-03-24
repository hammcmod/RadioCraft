package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends AbstractPowerBlockEntity {

	public final int powerPerTick;
	public final double rainMultiplier;

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state, 200, 15);
		powerPerTick = RadiocraftConfig.SOLAR_PANEL_MAX_OUTPUT.get();
		rainMultiplier = RadiocraftConfig.SOLAR_PANEL_RAIN_MULTIPLIER.get();
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof SolarPanelBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				if(level.isDay()) { // Time is day
					int powerGenerated = level.isRaining() ? (int)Math.round(be.powerPerTick * be.rainMultiplier) : be.powerPerTick;
					be.energyStorage.receiveEnergy(powerGenerated, false); // Do not push, only charge controller pushes to batteries, everything else will pull from this.
				}
			}
		}
	}

	@Override
	public ConnectionType getDefaultConnectionType() {
		return ConnectionType.PUSH;
	}
}

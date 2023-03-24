package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends AbstractPowerBlockEntity {

	public final double rainMultiplier;

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state, RadiocraftConfig.SOLAR_PANEL_MAX_OUTPUT.get(), RadiocraftConfig.SOLAR_PANEL_MAX_OUTPUT.get());
		rainMultiplier = RadiocraftConfig.SOLAR_PANEL_RAIN_MULTIPLIER.get();
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof SolarPanelBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				if(level.isDay()) { // Time is day
					int powerGenerated = level.isRaining() ? (int)Math.round(be.energyStorage.getMaxReceive() * be.rainMultiplier) : be.energyStorage.getMaxReceive();
					be.energyStorage.receiveEnergy(powerGenerated, false); // Generate power
				}
				be.pushToAll(be.energyStorage.getMaxExtract(), true); // Push to connected networks
			}
		}
	}

	@Override
	public ConnectionType getConnectionType() {
		return ConnectionType.PUSH;
	}
}

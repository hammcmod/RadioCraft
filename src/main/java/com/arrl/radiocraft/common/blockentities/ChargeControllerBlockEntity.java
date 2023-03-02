package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

public class ChargeControllerBlockEntity extends AbstractPowerBlockEntity {

	public ChargeControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.CHARGE_CONTROLLER.get(), pos, state, 75, 75);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof ChargeControllerBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				Collection<PowerNetwork> networks = be.getNetworks().values();

				for(PowerNetwork network : networks) { // Pull power from every network if possible
					if(be.energyStorage.getEnergyStored() == be.energyStorage.getMaxEnergyStored())
						break; // Stop if full
					int powerAvailable = network.pullPower(75, true);
					int powerUsed = be.energyStorage.receiveEnergy(powerAvailable, false);
					network.pullPower(powerUsed, false); // Actually pull the power out now
				}


				int energyToPush = be.energyStorage.extractEnergy(be.energyStorage.getEnergyStored(), true); // Do not actually pull out power yet.

				for(PowerNetwork network : networks) {
					energyToPush -= network.pushBatteries(energyToPush, false); // Push as much as possible into batteries
					if(energyToPush <= 0)
						break;
				}
				be.energyStorage.setEnergy(energyToPush); // Set energy to the remainder after pushing.
			}
		}
	}

}


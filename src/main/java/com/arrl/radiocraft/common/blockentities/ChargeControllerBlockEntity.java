package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import com.arrl.radiocraft.common.power.PowerNetwork;
import com.arrl.radiocraft.common.power.PowerNetwork.PowerNetworkEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class ChargeControllerBlockEntity extends AbstractPowerBlockEntity {

	public ChargeControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.CHARGE_CONTROLLER.get(), pos, state, RadiocraftConfig.CHARGE_CONTROLLER_TICK.get(), RadiocraftConfig.CHARGE_CONTROLLER_TICK.get());
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof ChargeControllerBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				int energyToPush = be.energyStorage.extractEnergy(be.energyStorage.getEnergyStored(), true); // Do not actually pull out power yet.

				List<LargeBatteryBlockEntity> batteries = new ArrayList<>(); // Specifically grab batteries to avoid having to use another sorted list.
				for(PowerNetwork network : be.getNetworks().values()) {
					for(PowerNetworkEntry item : network.getConnections()) {
						if(item.getNetworkItem().getConnectionType() == ConnectionType.PUSH) // Double check here is faster as instanceof can be quite slow.
							if(item.getNetworkItem() instanceof LargeBatteryBlockEntity battery)
								batteries.add(battery);
					}
				}

				for(LargeBatteryBlockEntity battery : batteries) {
					LazyOptional<IEnergyStorage> energyCap = battery.getCapability(ForgeCapabilities.ENERGY);
					if(energyCap.isPresent()) { // This is horrendous code but java doesn't like lambdas and vars.
						IEnergyStorage storage = energyCap.orElse(null);
						energyToPush -= storage.receiveEnergy(energyToPush, false);

						if(energyToPush <= 0)
							break;
					}
				}
				be.energyStorage.setEnergy(energyToPush); // Set energy to the remainder after pushing.
			}
		}
	}

}


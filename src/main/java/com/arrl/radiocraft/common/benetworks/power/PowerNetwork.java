package com.arrl.radiocraft.common.benetworks.power;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.api.benetworks.IPowerNetworkItem;
import com.arrl.radiocraft.common.benetworks.BENetwork;
import com.arrl.radiocraft.api.benetworks.IBENetworkItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

/**
 * PowerNetwork represents all devices connected to a given line of wires
 */
public class PowerNetwork extends BENetwork {


	public PowerNetwork(List<BENetworkEntry> entries) {
		super(entries);
	}

	public PowerNetwork() {
		this(null);
	}

	@Override
	public void addConnection(IBENetworkItem networkItem) {
		if(!(networkItem instanceof IPowerNetworkItem)) {
			Radiocraft.LOGGER.error("Tried to add a non-power BE to a PowerNetwork");
			return;
		}
		super.addConnection(networkItem);
	}

	@Override
	public void addConnection(BENetworkEntry entry) {
		if(!(entry.getNetworkItem() instanceof IPowerNetworkItem)) {
			Radiocraft.LOGGER.error("Tried to add a non-power BE to a PowerNetwork");
			return;
		}
		super.addConnection(entry);
	}

	/**
	 * Attempts to pull power from the network.
	 * @param simulate If true, do not actually extract energy from providers
	 * @returns amount pulled from network
	 */
	public int pullPower(int amount, boolean simulate) {
		int pulled = 0;

		clean();
		for(BENetworkEntry entry : connections) {
			if(entry.getNetworkItem() instanceof IPowerNetworkItem networkItem) {
				if(networkItem.getConnectionType() == ConnectionType.PUSH) {
					BlockEntity be = (BlockEntity)networkItem;

					LazyOptional<IEnergyStorage> energyCap = be.getCapability(ForgeCapabilities.ENERGY);

					if(energyCap.isPresent()) { // This is horrendous code but java doesn't like lambdas and vars.
						IEnergyStorage storage = energyCap.orElse(null);
						int amountRemoved = storage.extractEnergy(amount - pulled, simulate);
						pulled += amountRemoved;

						if(pulled >= amount) // Stop checking if required amount is reached
							return pulled;
					}
				}
			}
			else {
				Radiocraft.LOGGER.error("Found a non-power BE in a PowerNetwork");
				connections.remove(entry);
				break;
			}


		}
		return pulled;
	}

	@Override
	public BENetwork createNetwork() {
		return new PowerNetwork();
	}


}

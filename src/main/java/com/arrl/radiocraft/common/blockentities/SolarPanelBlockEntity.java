package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.data.PowerNetworkSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SolarPanelBlockEntity extends BlockEntity {

	private static final int SOLAR_OUTPUT_WATTS = 200; // Measured in W/m^2
	// Convert 1W = 8FE/t; 200 W/m^2 = 1,600 (FE/t)/m^2
	private static final int SOLAR_OUTPUT = SOLAR_OUTPUT_WATTS * 8;

	private final EnergyStorage energyStorage = new EnergyStorage(SOLAR_OUTPUT, SOLAR_OUTPUT, SOLAR_OUTPUT);

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state);
	}

	public static double getSolarCoefficient(Level level, BlockPos pos) {
		if (!level.canSeeSky(pos)) return 0;
		if (!level.isDay()) return 0;
		return Math.cos(level.getSunAngle(0));
	}

	public static double getRainCoefficient(Level level) {
		return level.isRaining() ? 0.2 : 1;
	}

	public static int getSolarOutput(Level level, BlockPos pos) {
		double solarEffCoefficient = getSolarCoefficient(level, pos);
		double rainEffCoefficient = getRainCoefficient(level);
		return (int) (SOLAR_OUTPUT * solarEffCoefficient * rainEffCoefficient);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
		if (t instanceof SolarPanelBlockEntity be) {
			if (level.isClientSide) return;

			int energyFromSolar = getSolarOutput(level, pos);
			if (energyFromSolar > 0) {
				be.energyStorage.receiveEnergy(energyFromSolar, false);
			}

			// Distribute power to network
			be.distributePower((ServerLevel) level);
		}
	}

	private void distributePower(ServerLevel level) {
		if (energyStorage.getEnergyStored() == 0) return;

		PowerNetworkSavedData networkData = PowerNetworkSavedData.get(level);
		var network = networkData.getNetwork(worldPosition);

		if (network != null) {
			// Find consumers in the network
			Set<BlockPos> networkPositions = networkData.getNetworkPositions(network.getUUID());
			int totalEnergyToDistribute = energyStorage.getEnergyStored();
			int energyPerBlock = totalEnergyToDistribute / networkPositions.size();

			for (BlockPos pos : networkPositions) {
				if (pos.equals(worldPosition)) continue; // Skip self

				BlockEntity be = level.getBlockEntity(pos);
				if (be != null) {
					IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
					if (storage != null && storage.canReceive()) {
						int transferred = storage.receiveEnergy(energyPerBlock, false);
						energyStorage.extractEnergy(transferred, false);

						if (energyStorage.getEnergyStored() == 0) break;
					}
				}
			}
		}
	}

	public IEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level instanceof ServerLevel serverLevel) {
			PowerNetworkSavedData networkData = PowerNetworkSavedData.get(serverLevel);
			var network = networkData.getNetwork(worldPosition);
			if (network == null) {
				network = networkData.createNetwork();
				networkData.addToNetwork(worldPosition, network);
			}
		}
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel serverLevel) {
			PowerNetworkSavedData networkData = PowerNetworkSavedData.get(serverLevel);
			networkData.removeFromNetwork(worldPosition);
		}
		super.setRemoved();
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("energy", energyStorage.serializeNBT(registries));
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("energy")) {
			energyStorage.deserializeNBT(registries, tag.get("energy"));
		}
	}
}
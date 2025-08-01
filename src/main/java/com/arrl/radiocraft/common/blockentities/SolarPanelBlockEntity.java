package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.data.PowerNetworkSavedData;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

public class SolarPanelBlockEntity extends BlockEntity implements IEnergyStorage {

	private int lastSolarOutput = 0;

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

			// Distribute power using unified method
			be.distributePower((ServerLevel) level);
		}
	}

	public int getLastSolarOutput() {
		return lastSolarOutput;
	}

	private void distributePower(ServerLevel level) {
		lastSolarOutput = 0;
		if (energyStorage.getEnergyStored() == 0) return;

		PowerNetworkSavedData networkData = PowerNetworkSavedData.get(level);
		int transferred = networkData.distributeProducerPower(
				level,
				worldPosition,
				energyStorage.getEnergyStored(),
				SOLAR_OUTPUT
		);

		lastSolarOutput = transferred;
		energyStorage.extractEnergy(transferred, false);
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

	@Override
	public int receiveEnergy(int i, boolean b) {
		return 0;
	}

	@Override
	public int extractEnergy(int i, boolean b) {
		int extracted = energyStorage.extractEnergy(i, b);
		lastSolarOutput += extracted;
		return extracted;
	}

	@Override
	public int getEnergyStored() {
		return energyStorage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored() {
		return energyStorage.getMaxEnergyStored();
	}

	@Override
	public boolean canExtract() {
		return true;
	}

	@Override
	public boolean canReceive() {
		return false;
	}
}
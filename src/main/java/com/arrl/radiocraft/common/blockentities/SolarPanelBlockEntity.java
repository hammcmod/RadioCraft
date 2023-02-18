package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.power.ConnectionType;
import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SolarPanelBlockEntity extends BlockEntity implements IPowerNetworkItem {

	private final BasicEnergyStorage energyStorage = new BasicEnergyStorage(200, 15); // 200 capacity, 15 max transfer
	private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);
	private Map<Direction, PowerNetwork> networks = new HashMap<>();

	public static final int POWER_PER_TICK = 10;
	public static final float RAIN_MULTIPLIER = 0.5F;

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.SOLAR_PANEL.get(), pos, state);
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

	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == ForgeCapabilities.ENERGY ? energy.cast() : super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energy.invalidate();
	}

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.put("energy", energyStorage.serializeNBT());
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		energyStorage.deserializeNBT(nbt.get("energy"));
	}

	@Override
	public Map<Direction, PowerNetwork> getNetworks() {
		return networks;
	}

	@Override
	public void setNetworks(Map<Direction, PowerNetwork> networks) {
		this.networks = networks;
	}

	@Override
	public ConnectionType getDefaultConnectionType() {
		return ConnectionType.PUSH;
	}
}

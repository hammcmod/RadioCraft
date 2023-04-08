package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.common.blocks.AbstractPowerNetworkBlock;
import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.arrl.radiocraft.common.power.ConnectionType;
import com.arrl.radiocraft.common.power.IPowerNetworkItem;
import com.arrl.radiocraft.common.power.PowerNetwork;
import com.arrl.radiocraft.common.power.PowerNetwork.PowerNetworkEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractPowerBlockEntity extends BlockEntity implements IPowerNetworkItem, MenuProvider {

	protected final BasicEnergyStorage energyStorage;
	protected final LazyOptional<IEnergyStorage> energy;
	private Map<Direction, PowerNetwork> networks = new HashMap<>();

	public AbstractPowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
		super(type, pos, state);
		energyStorage = new BasicEnergyStorage(capacity, maxTransfer);
		energy = LazyOptional.of(() -> energyStorage);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == ForgeCapabilities.ENERGY ? energy.cast() : super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energy.invalidate();
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
	public void onLoad() {
		super.onLoad();
		BlockState state = level.getBlockState(getBlockPos()); // Create/add to networks when loaded
		if(state.getBlock() instanceof AbstractPowerNetworkBlock block)
			block.onPlace(state, level, getBlockPos(), Blocks.AIR.defaultBlockState(), false);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		for(PowerNetwork network : getNetworks().values())
			network.removeConnection(this); // Remove self from networks
	}

	/**
	 * Attempt to consume power from self, return false if there isn't enough
	 */
	public boolean tryConsumePower(int amount, boolean simulate) {
		return energyStorage.extractEnergy(amount, simulate) == amount;
	}

	public void pushToAll(int amount, boolean includeChargeControllers) {
		List<ChargeControllerBlockEntity> chargeControllers = new ArrayList<>();
		List<BlockEntity> otherItems = new ArrayList<>();

		for(PowerNetwork network : getNetworks().values()) {
			for(PowerNetworkEntry item : network.getConnections()) {
				if(item.getNetworkItem().getConnectionType() == ConnectionType.PULL) {
					if(item.getNetworkItem() instanceof ChargeControllerBlockEntity be) {
						if(includeChargeControllers)
							chargeControllers.add(be);
						continue;
					}
					otherItems.add((BlockEntity)item.getNetworkItem());
				}
			}
		}

		if(!chargeControllers.isEmpty()) {
			for(ChargeControllerBlockEntity be : chargeControllers) {
				amount -= tryPushPowerTo(be, amount);
				if(amount <= 0)
					return;
			}
		}

		for(BlockEntity be : otherItems) {
			amount -= tryPushPowerTo(be, amount);
			if(amount <= 0)
				return;
		}
	}

	public int tryPushPowerTo(BlockEntity be, int amount) {
		if(be != null) {
			LazyOptional<IEnergyStorage> energyCap = be.getCapability(ForgeCapabilities.ENERGY);
			if(energyCap.isPresent()) { // This is horrendous code but java doesn't like lambdas and vars.
				IEnergyStorage storage = energyCap.orElse(null);
				return storage.receiveEnergy(amount, false);
			}
		}
		return 0;
	}

}

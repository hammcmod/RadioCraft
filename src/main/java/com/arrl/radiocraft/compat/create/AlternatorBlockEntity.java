// Adapted from Create Crafts & Additions (https://github.com/mrh0/createaddition), MIT License, Copyright (c) 2024 MRH.
// See THIRD_PARTY.md.
package com.arrl.radiocraft.compat.create;

import com.arrl.radiocraft.common.capabilities.BasicEnergyStorage;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * An alternator's coil: while its shaft turns fast enough it makes FE in proportion to the speed
 * ({@code alternator_fe_at_256_rpm}) and pushes it into whatever beside it takes FE, up to {@link #MAX_OUTPUT} a tick
 * to each. It takes no FE in.
 */
public class AlternatorBlockEntity extends KineticBlockEntity {

	static final int CAPACITY = 5000;
	static final int MAX_OUTPUT = 5000;

	final BasicEnergyStorage energy = new BasicEnergyStorage(CAPACITY, 0, MAX_OUTPUT);
	private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> neighbours = new EnumMap<>(Direction.class);

	public AlternatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** FE a tick at {@code rpm}. */
	public static int production(float rpm) {
		return (int)(CreateAlternator.feAt256Rpm() * Math.abs(rpm) / 256.0);
	}

	@Override
	public float calculateStressApplied() {
		float impact = (float)CreateAlternator.stressImpact();
		lastStressApplied = impact;
		return impact;
	}

	@Override
	protected Block getStressConfigKey() {
		return CreateAlternator.BLOCK.get();
	}

	@Override
	public void tick() {
		super.tick();
		if(!(level instanceof ServerLevel))
			return;
		if(neighbours.isEmpty())
			updateCache();
		if(getSpeed() != 0 && isSpeedRequirementFulfilled())
			energy.setEnergy(energy.getEnergyStored() + production(getSpeed()));
		for(BlockCapabilityCache<IEnergyStorage, Direction> neighbour : neighbours.values()) {
			IEnergyStorage storage = neighbour.getCapability();
			if(storage == null || energy.getEnergyStored() == 0)
				continue;
			int accepted = storage.receiveEnergy(Math.min(MAX_OUTPUT, energy.getEnergyStored()), true);
			storage.receiveEnergy(energy.extractEnergy(accepted, false), false);
		}
	}

	/** Looks afresh for FE storage on every side. */
	public void updateCache() {
		if(!(level instanceof ServerLevel serverLevel))
			return;
		for(Direction side : Direction.values())
			neighbours.put(side, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, serverLevel,
					worldPosition.relative(side), side.getOpposite(), () -> !isRemoved(), () -> {}));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		int rate = production(isSpeedRequirementFulfilled() ? getSpeed() : 0);
		tooltip.add(Component.literal("    ").append(Component.translatable("radiocraft.goggles.alternator.production")
				.withStyle(ChatFormatting.GRAY)));
		tooltip.add(Component.literal("    ").append(Component.literal(String.format(Locale.ROOT, " %,d FE/t ", rate))
				.withStyle(ChatFormatting.AQUA)).append(Component.translatable("create.gui.goggles.at_current_speed")
				.withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.setEnergy(tag.getInt("energy"));
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("energy", energy.getEnergyStored());
	}
}

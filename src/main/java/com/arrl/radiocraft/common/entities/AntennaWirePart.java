package com.arrl.radiocraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

public class AntennaWirePart extends PartEntity<AntennaWire> implements IAntennaWire {

	public final AntennaWire parent;
	public final String name;

	public AntennaWirePart(AntennaWire parent, String name) {
		super(parent);
		this.parent = parent;
		this.name = name;
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return !this.isInvulnerableTo(source) && parent.hurt(source, amount);
	}

	@Override
	public boolean is(Entity entity) {
		return this == entity || parent == entity;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public ItemStack getPickResult() {
		return parent.getPickResult();
	}

	@Override
	public Vec3 getRopeHoldPosition(float partialTicks) {
		return position();
	}

	@Override
	public Vec3 getLeashOffset(float partialTicks) {
		return parent.getLeashOffset(partialTicks);
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return parent.getDimensions(pose);
	}

	@Override
	public BlockPos getEndPos() {
		return parent.blockPosition();
	}

	public BlockPos getStartPos() {
		return blockPosition();
	}

	public boolean isPairedWith(IAntennaWire other) {
		return other == parent;
	}

}

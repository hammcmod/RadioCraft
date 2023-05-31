package com.arrl.radiocraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

public class AntennaWirePart extends PartEntity<AntennaWire> {

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
	public Vec3 position() {
		BlockPos pos = parent.getEndPos();
		setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
		return super.position();
	}
}

package com.arrl.radiocraft.common.entities;

import com.arrl.radiocraft.common.blockentities.AntennaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public class AntennaWirePart extends PartEntity<AntennaWire> implements IAntennaWire {

	public final AntennaWire parent;
	public final String name;

	private boolean checkEnabled = true;

	public AntennaWirePart(AntennaWire parent, String name) {
		super(parent);
		this.parent = parent;
		this.name = name;
	}


	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {}

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

	public BlockPos getEndPos() {
		return parent.blockPosition();
	}

	public BlockPos getStartPos() {
		return blockPosition();
	}

	public double getLength() { return parent.getLength();}

	public boolean isPairedWith(IAntennaWire other) {
		return other == parent;
	}

	public void updateAntennas() {
		if(checkEnabled) {
			checkEnabled = false;
			if(level().getBlockEntity(blockPosition()) instanceof AntennaBlockEntity be)
				be.markAntennaChanged();

			parent.updateAntennas();
			checkEnabled = true;
		}
	}

}

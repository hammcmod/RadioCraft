package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftServerConfig;
import com.arrl.radiocraft.api.benetworks.BENetworkObject;
import com.arrl.radiocraft.api.benetworks.INetworkObjectProvider;
import com.arrl.radiocraft.common.be_networks.network_objects.MicrowaveNetworkObjects;
import com.arrl.radiocraft.common.blocks.MicrowaveHornBlock;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.radio.DirectionFinding;
import com.arrl.radiocraft.common.radio.microwave.MicrowaveMath;
import com.arrl.radiocraft.compat.sable.Sublevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A microwave dish (the small dish) or horn (the large horn's bottom back part, which takes the coax): where it points
 * and how far it reaches. A placed dish faces the player who placed it, level; the microwave radio's screen turns
 * and tilts it (SETUP a degree at a time, ALIGN half a degree).
 */
public class MicrowaveDishBlockEntity extends BlockEntity implements INetworkObjectProvider {

	/** Compass degrees the dish points at, and degrees above the horizon. */
	private float azimuth = 0.0F;
	private float elevation = 0.0F;

	public MicrowaveDishBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The large horn's block entity. */
	public static MicrowaveDishBlockEntity horn(BlockPos pos, BlockState state) {
		MicrowaveDishBlockEntity horn = new MicrowaveDishBlockEntity(RadiocraftBlockEntities.MICROWAVE_HORN.get(), pos, state);
		if(state.hasProperty(MicrowaveHornBlock.FACING))
			horn.azimuth = state.getValue(MicrowaveHornBlock.FACING).toYRot() + 180.0F; // Its mouth, as a compass bearing.
		horn.azimuth = (float)DirectionFinding.normalize(horn.azimuth);
		return horn;
	}

	public boolean isHorn() {
		return getBlockState().getBlock() instanceof MicrowaveHornBlock;
	}

	/** How far this end reaches on its own, blocks. */
	public double getRange() {
		double dish = RadiocraftServerConfig.MICROWAVE_DISH_RANGE.get();
		return isHorn() ? dish * RadiocraftServerConfig.MICROWAVE_HORN_RANGE_FACTOR.get() : dish;
	}

	public float getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(double degrees) {
		azimuth = (float)DirectionFinding.normalize(degrees);
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public float getElevation() {
		return elevation;
	}

	public void setElevation(double degrees) {
		// A horn tilts inside itself (it does not show), and only so far: the feed horn's reach within the reflector.
		double low = isHorn() ? -MicrowaveMath.HORN_TILT : MicrowaveMath.MIN_ELEVATION, high = isHorn() ? MicrowaveMath.HORN_TILT : MicrowaveMath.MAX_ELEVATION;
		elevation = (float)Math.max(low, Math.min(high, degrees));
		setChanged();
		if(level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** Where the beam starts: the dish a little above its block, the horn's mouth two blocks up. */
	public MicrowaveMath.End end() {
		double up = isHorn() ? 2.5D : 0.75D;
		Vec3 mouth = new Vec3(worldPosition.getX() + 0.5D, worldPosition.getY() + up, worldPosition.getZ() + 0.5D);
		Sublevels.Frame ship = Sublevels.frame(level, worldPosition);
		if(ship.isGround())
			return new MicrowaveMath.End(mouth.x, mouth.y, mouth.z, azimuth, elevation, getRange());
		// On a Sable ship: the beam starts where the dish really is, and points where the ship has turned it.
		Vec3 at = ship.apply(mouth);
		double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
		Vec3 pointing = ship.applyDirection(new Vec3(Math.sin(az) * Math.cos(el), Math.sin(el), -Math.cos(az) * Math.cos(el)));
		double worldAzimuth = DirectionFinding.normalize(Math.toDegrees(Math.atan2(pointing.x, -pointing.z)));
		double worldElevation = Math.toDegrees(Math.atan2(pointing.y, Math.hypot(pointing.x, pointing.z)));
		return new MicrowaveMath.End(at.x, at.y, at.z, worldAzimuth, worldElevation, getRange());
	}

	/** The nearest of the four directions to where it points, for the block state. */
	public static Direction nearestDirection(double azimuth) {
		return Direction.fromYRot(DirectionFinding.normalize(azimuth) + 180.0D);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(level != null && !level.isClientSide)
			getNetworkObject(level, worldPosition); // Joins the coax.
	}

	@Override
	public BENetworkObject createNetworkObject() {
		return new MicrowaveNetworkObjects.Dish(level, worldPosition);
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putFloat("azimuth", azimuth);
		tag.putFloat("elevation", elevation);
	}

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		if(tag.contains("azimuth"))
			azimuth = tag.getFloat("azimuth");
		elevation = tag.getFloat("elevation");
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}

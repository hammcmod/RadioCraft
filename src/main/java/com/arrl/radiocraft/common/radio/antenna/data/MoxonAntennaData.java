package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

/** A Moxon's size (width across, depth front to back, in blocks) and the way its beam points. */
public class MoxonAntennaData extends AntennaData {

	private int width;
	private int depth;
	private Direction facing;

	public MoxonAntennaData(int width, int depth, Direction facing) {
		this.width = width;
		this.depth = depth;
		this.facing = facing;
	}

	public int getWidth() {
		return width;
	}

	public int getDepth() {
		return depth;
	}

	/** The way the beam points: from the reflector to the driven element. */
	public Direction getFacing() {
		return facing;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("width", width);
		nbt.putInt("depth", depth);
		nbt.putInt("facing", facing.get2DDataValue());
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		width = nbt.getInt("width");
		depth = nbt.getInt("depth");
		facing = Direction.from2DDataValue(nbt.getInt("facing"));
	}
}

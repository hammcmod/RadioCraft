package com.arrl.radiocraft.common.radio.antenna.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;

/**
 * Wires on a balun that make none of the proper antennas: each arm's length along the wire, how far the shape is from
 * the nearest ideal one, and the balun ratio. See {@link com.arrl.radiocraft.common.radio.antenna.types.ImprovisedAntennaType}.
 */
public class ImprovisedAntennaData extends AntennaData {

	private double[] armLengths;
	private double shapePenalty;
	private int balunRatio;

	/**
	 * @param armLengths   Length of each arm leaving the balun, measured along the wire, longest first.
	 * @param shapePenalty SWR added for the shape (bent, sloping, extra or branching arms).
	 * @param balunRatio   1, 2, 4 or 9.
	 */
	public ImprovisedAntennaData(double[] armLengths, double shapePenalty, int balunRatio) {
		this.armLengths = armLengths.clone();
		this.shapePenalty = shapePenalty;
		this.balunRatio = balunRatio;
	}

	public double[] getArmLengths() {
		return armLengths.clone();
	}

	public double getShapePenalty() {
		return shapePenalty;
	}

	public int getBalunRatio() {
		return balunRatio;
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag nbt = new CompoundTag();
		nbt.putLongArray("arms", Arrays.stream(armLengths).mapToLong(Double::doubleToLongBits).toArray());
		nbt.putDouble("shapePenalty", shapePenalty);
		nbt.putInt("balunRatio", balunRatio);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		armLengths = Arrays.stream(nbt.getLongArray("arms")).mapToDouble(Double::longBitsToDouble).toArray();
		shapePenalty = nbt.getDouble("shapePenalty");
		balunRatio = Math.max(1, nbt.getInt("balunRatio"));
	}
}

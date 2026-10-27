package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The HF manpack's whip: a short vertical with a built-in matching unit, so it loads on 10m and 20m alike. A few dB
 * down on a proper antenna both ways, fine on the ground wave, poor for skip. Never built in the world: the manpack
 * carries it.
 */
public class ManpackWhipAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

	public static final ManpackWhipAntennaType INSTANCE = new ManpackWhipAntennaType();

	public ManpackWhipAntennaType() {
		super(Radiocraft.id("manpack_whip"), -3.0D, -3.0D, 1.0D, 0.5D);
	}

	@Override
	public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
		return null;
	}

	/** The matching unit keeps it near 1.5:1 on both bands. */
	@Override
	public double getSWR(EmptyAntennaData data, float frequencyHertz) {
		return 1.5D;
	}

	@Override
	public EmptyAntennaData getDefaultData() {
		return new EmptyAntennaData();
	}
}

package com.arrl.radiocraft.common.radio.antenna.types;

import com.arrl.radiocraft.Radiocraft;
import com.arrl.radiocraft.common.radio.antenna.StaticAntenna;
import com.arrl.radiocraft.common.radio.antenna.data.EmptyAntennaData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The antenna a commercial radio module is wired to (on the roof of whatever building it is in, out of sight): a plain
 * vertical, well matched on every band, as good on the ground wave as on skip. Never built in the world.
 */
public class CabinetAntennaType extends NonDirectionalAntennaType<EmptyAntennaData> {

	public static final CabinetAntennaType INSTANCE = new CabinetAntennaType();

	public CabinetAntennaType() {
		super(Radiocraft.id("cabinet_antenna"), 0.0D, 0.0D, 1.0D, 1.0D);
	}

	@Override
	public StaticAntenna<EmptyAntennaData> match(Level level, BlockPos pos) {
		return null;
	}

	@Override
	public double getSWR(EmptyAntennaData data, float frequencyHertz) {
		return 1.0D;
	}

	@Override
	public EmptyAntennaData getDefaultData() {
		return new EmptyAntennaData();
	}
}

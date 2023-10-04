package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.*;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.JPoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.SlimJimAntennaType;

public class RadiocraftAntennaTypes {

	// HF Antennas
	public static DipoleAntennaType DIPOLE;
	public static EndFedAntennaType END_FED;
	public static HorizontalQuadLoopAntennaType HORIZONTAL_QUAD_LOOP;
	public static VerticalQuadLoopAntennaType VERTICAL_QUAD_LOOP;
	public static QuarterWaveVerticalAntennaType QUARTER_WAVE_VERTICAL;

	// VHF Antennas
	public static JPoleAntennaType J_POLE;
	public static SlimJimAntennaType SLIM_JIM;

	public static void register() {
		DIPOLE = AntennaTypes.registerType(new DipoleAntennaType());
		END_FED = AntennaTypes.registerType(new EndFedAntennaType());
		HORIZONTAL_QUAD_LOOP = AntennaTypes.registerType(new HorizontalQuadLoopAntennaType());
		VERTICAL_QUAD_LOOP = AntennaTypes.registerType(new VerticalQuadLoopAntennaType());
		QUARTER_WAVE_VERTICAL = AntennaTypes.registerType(new QuarterWaveVerticalAntennaType());

		J_POLE = AntennaTypes.registerType(new JPoleAntennaType());
		SLIM_JIM = AntennaTypes.registerType(new SlimJimAntennaType());
	}

}

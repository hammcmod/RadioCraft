package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.common.radio.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.EndFedAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.HorizontalQuadLoopAntennaType;

public class RadiocraftAntennaTypes {

	public static DipoleAntennaType DIPOLE;
	public static EndFedAntennaType END_FED;
	public static HorizontalQuadLoopAntennaType HORIZONTAL_QUAD_LOOP;

	public static void register() {
		DIPOLE = AntennaTypes.registerType(new DipoleAntennaType());
		END_FED = AntennaTypes.registerType(new EndFedAntennaType());
		HORIZONTAL_QUAD_LOOP = AntennaTypes.registerType(new HorizontalQuadLoopAntennaType());
	}

}

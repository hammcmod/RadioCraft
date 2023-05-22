package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.common.radio.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.DipoleAntennaType;

public class RadiocraftAntennaTypes {

	public static DipoleAntennaType DIPOLE;

	public static void register() {
		DIPOLE = AntennaTypes.registerType(new DipoleAntennaType());
	}

}

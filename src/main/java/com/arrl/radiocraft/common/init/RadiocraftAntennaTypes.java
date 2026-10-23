package com.arrl.radiocraft.common.init;

import com.arrl.radiocraft.api.antenna.AntennaTypes;
import com.arrl.radiocraft.common.radio.antenna.types.*;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.JPoleAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.SlimJimAntennaType;
import com.arrl.radiocraft.common.radio.antenna.types.vhf.YagiAntennaType;

public class RadiocraftAntennaTypes {

	// HF Antennas
	public static DipoleAntennaType DIPOLE;
	public static EndFedAntennaType END_FED;
	public static HorizontalQuadLoopAntennaType HORIZONTAL_QUAD_LOOP;
	public static VerticalQuadLoopAntennaType VERTICAL_QUAD_LOOP;
	public static QuarterWaveVerticalAntennaType QUARTER_WAVE_VERTICAL;
	public static InvertedVAntennaType INVERTED_V;
	public static RandomWireAntennaType RANDOM_WIRE;
	public static OffCenterFedDipoleAntennaType OFF_CENTER_FED_DIPOLE;
	public static ImprovisedAntennaType IMPROVISED;

	// VHF Antennas
	public static JPoleAntennaType J_POLE;
	public static com.arrl.radiocraft.common.radio.antenna.types.DummyLoadAntennaType DUMMY_LOAD; // Every band.
	public static SlimJimAntennaType SLIM_JIM;
	public static YagiAntennaType YAGI;
	public static com.arrl.radiocraft.common.radio.antenna.types.vhf.SatelliteDishAntennaType SATELLITE_DISH;
	public static com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType IONOSONDE; // Every band, badly; for the ionospheric processor.

	public static void register() {
		DIPOLE = AntennaTypes.registerType(new DipoleAntennaType());
		END_FED = AntennaTypes.registerType(new EndFedAntennaType());
		HORIZONTAL_QUAD_LOOP = AntennaTypes.registerType(new HorizontalQuadLoopAntennaType());
		VERTICAL_QUAD_LOOP = AntennaTypes.registerType(new VerticalQuadLoopAntennaType());
		QUARTER_WAVE_VERTICAL = AntennaTypes.registerType(new QuarterWaveVerticalAntennaType());
		INVERTED_V = AntennaTypes.registerType(new InvertedVAntennaType());
		RANDOM_WIRE = AntennaTypes.registerType(new RandomWireAntennaType());
		OFF_CENTER_FED_DIPOLE = AntennaTypes.registerType(new OffCenterFedDipoleAntennaType());
		IMPROVISED = AntennaTypes.registerFallbackType(new ImprovisedAntennaType()); // Last: anything the others do not recognise.

		J_POLE = AntennaTypes.registerType(new JPoleAntennaType());
		DUMMY_LOAD = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.DummyLoadAntennaType());
		SLIM_JIM = AntennaTypes.registerType(new SlimJimAntennaType());
		YAGI = AntennaTypes.registerType(new YagiAntennaType());
		SATELLITE_DISH = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.vhf.SatelliteDishAntennaType());
		IONOSONDE = AntennaTypes.registerType(new com.arrl.radiocraft.common.radio.antenna.types.IonosondeAntennaType());
	}

}
